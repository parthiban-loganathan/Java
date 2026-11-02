package lab.civiltech.android;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import civiltech.hydrology.CurveNumberTable;
import civiltech.hydrology.HourlyRainfall;
import civiltech.lab.CatchmentModel;
import civiltech.lab.GridData;
import civiltech.lab.RainfallGrid;
import civiltech.lab.SimulationResult;
import civiltech.lab.SimulationSettings;
import civiltech.terrain.Watershed;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Android front end of the lab's catchment model. All calculations are done by the civiltech-core
 * classes, the same source files that the website runs after TeaVM translation.
 * The user interface is built in code (no XML resources) to keep the project small.
 */
public class MainActivity extends Activity {

    /** Rainfall windows of the bundled ERA5 file: label, first hour index, number of hours. */
    private static final String[] EVENT_NAMES = {
        "Cyclone Fengal, 29 Nov - 3 Dec 2024 (96 h)", "11 - 14 Dec 2024 (72 h)", "16 Nov - 16 Dec 2024 (720 h)"};
    private static final int[][] EVENTS = {{312, 96}, {600, 72}, {0, 720}};

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());

    private GridData grid;
    private RainfallGrid rainfall;
    private CatchmentModel model;
    private int outletRow, outletCol, snapRadius;

    private TextView status, catchmentInfo, results;
    private Spinner soil, event;
    private Button runButton;
    private CatchmentMapView mapView;
    private HydrographView hydrograph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildLayout());
        status.setText("Loading open data and building the drainage network...");
        worker.execute(() -> {
            try {
                grid = GridData.parse(readAsset("vellore_hills_grid.json"));
                rainfall = RainfallGrid.parse(readAsset("era5_precip_grid.json"));
                model = new CatchmentModel(grid.terrain(), grid.landCover);
                outletRow = grid.rowOfLat(grid.outletLat);
                outletCol = grid.colOfLon(grid.outletLon);
                snapRadius = 0;
                ui.post(() -> {
                    mapView.setGrid(grid, model.network());
                    runButton.setEnabled(true);
                    simulate();
                });
            } catch (Exception e) {
                ui.post(() -> status.setText("Could not load data: " + e.getMessage()));
            }
        });
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }

    private View buildLayout() {
        int pad = dp(14);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = text("Hill catchment beside VIT Vellore", 20, true);
        root.addView(title);
        root.addView(text("Copernicus DEM, ESA WorldCover and ERA5 rainfall. Java delineates the catchment, "
                + "computes SCS Curve Number runoff and routes it to the outlet. Educational model: not calibrated.", 14, false));

        root.addView(text("Hydrologic soil group", 14, true));
        soil = new Spinner(this);
        soil.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[] {"A (sandy, low runoff)", "B", "C (moderately high runoff)", "D (clay or shallow rock)"}));
        soil.setSelection(2);
        root.addView(soil);

        root.addView(text("Rainfall (ERA5 hourly)", 14, true));
        event = new Spinner(this);
        event.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, EVENT_NAMES));
        root.addView(event);

        runButton = new Button(this);
        runButton.setText("Run simulation (Java)");
        runButton.setEnabled(false);
        runButton.setOnClickListener(v -> simulate());
        root.addView(runButton);

        status = text("", 13, false);
        root.addView(status);

        mapView = new CatchmentMapView(this);
        mapView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(300)));
        mapView.setOnCellTapListener((row, col) -> {
            outletRow = row;
            outletCol = col;
            snapRadius = 3;
            simulate();
        });
        root.addView(mapView);
        root.addView(text("Tap the map to move the outlet. Colours: land cover; yellow: catchment; red: outlet.", 12, false));

        catchmentInfo = text("", 14, false);
        root.addView(catchmentInfo);

        hydrograph = new HydrographView(this);
        hydrograph.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(260)));
        root.addView(hydrograph);

        results = text("", 14, false);
        results.setTypeface(Typeface.MONOSPACE);
        root.addView(results);

        AdapterView.OnItemSelectedListener rerun = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { if (model != null) simulate(); }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        };
        soil.setOnItemSelectedListener(rerun);
        event.setOnItemSelectedListener(rerun);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        return scroll;
    }

    /** Runs delineation and simulation on the worker thread, then updates the views. */
    private void simulate() {
        final int soilGroup = soil.getSelectedItemPosition();
        final int[] ev = EVENTS[event.getSelectedItemPosition()];
        final int row = outletRow, col = outletCol, snap = snapRadius;
        runButton.setEnabled(false);
        status.setText("Computing...");
        worker.execute(() -> {
            try {
                long t0 = System.nanoTime();
                Watershed ws = model.delineate(row, col, snap);
                double[] centroid = ws.centroidRowCol();
                double[] rain = rainfall.seriesAt(grid.latOfRow(centroid[0]), grid.lonOfCol(centroid[1]), ev[0], ev[1]);
                SimulationSettings settings = new SimulationSettings();
                settings.curveNumberByClass = CurveNumberTable.lookup(soilGroup);
                SimulationResult r = model.simulate(new HourlyRainfall(rain), settings);
                long ms = (System.nanoTime() - t0) / 1_000_000;
                int[] mask = ws.mask();
                ui.post(() -> show(ws, r, mask, ms));
            } catch (RuntimeException e) {
                ui.post(() -> {
                    status.setText("Error: " + e.getMessage());
                    runButton.setEnabled(true);
                });
            }
        });
    }

    private void show(Watershed ws, SimulationResult r, int[] mask, long ms) {
        int o = ws.outlet();
        int oRow = o / grid.cols, oCol = o % grid.cols;
        outletRow = oRow;
        outletCol = oCol;
        snapRadius = 0;
        mapView.setCatchment(mask, oRow, oCol);
        catchmentInfo.setText(String.format(Locale.ROOT,
                "Outlet %.5f N, %.5f E\nArea %.2f km2, longest flow path %.2f km, slope %.4f\nKirpich Tc %.2f h, area-weighted CN %.1f",
                grid.latOfRow(oRow), grid.lonOfCol(oCol), r.areaKm2, r.longestFlowPathM / 1000.0, r.longestPathSlope,
                r.tcHours, r.weightedCurveNumber));
        hydrograph.setData(r.timeHours, r.hourlyRainMm, r.flowScs, r.flowTimeArea);
        results.setText(String.format(Locale.ROOT,
                "Rainfall          %8.1f mm%nDirect runoff     %8.1f mm%nRunoff coeff.     %8.2f%n"
                        + "Runoff volume     %8.3f million m3%nPeak (SCS UH)     %8.1f m3/s at %.1f h%n"
                        + "Peak (time-area)  %8.1f m3/s at %.1f h",
                r.totalRainMm, r.totalExcessMm, r.runoffCoefficient, r.excessVolumeM3 / 1e6,
                r.peakScs, r.peakScsTimeHours, r.peakTimeArea, r.peakTimeAreaTimeHours));
        status.setText("Computed by Java in " + ms + " ms on this phone.");
        runButton.setEnabled(true);
    }

    private String readAsset(String name) throws IOException {
        try (InputStream in = getAssets().open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private TextView text(String s, int sizeSp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sizeSp);
        t.setTextColor(Color.rgb(23, 32, 43));
        if (bold) {
            t.setTypeface(Typeface.DEFAULT_BOLD);
        }
        t.setPadding(0, dp(6), 0, dp(4));
        return t;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
