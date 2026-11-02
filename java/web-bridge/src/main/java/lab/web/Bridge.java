package lab.web;

import civiltech.hydrology.CurveNumberTable;
import civiltech.hydrology.HourlyRainfall;
import civiltech.io.JsonWriter;
import civiltech.lab.CatchmentModel;
import civiltech.lab.GridData;
import civiltech.lab.RainfallGrid;
import civiltech.lab.SimulationResult;
import civiltech.lab.SimulationSettings;
import civiltech.terrain.FlowNetwork;
import civiltech.terrain.Watershed;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.teavm.jso.JSExport;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/**
 * Methods that JavaScript can call. TeaVM compiles this class (and everything it uses) into
 * assets/java/civiltech.js, an ES module whose exports have the same names as the methods below.
 *
 * Data crosses the boundary as numbers, strings, JSON text and numeric arrays (JavaScript
 * typed arrays), which TeaVM converts automatically.
 */
public final class Bridge {

    /** JavaScript callback receiving the text printed by a course program. */
    @JSFunctor
    public interface OutputCallback extends JSObject {
        void done(String output, boolean finished);
    }

    private static CatchmentModel model;
    private static GridData grid;
    private static RainfallGrid rainfallGrid;
    private static SimulationResult lastResult;

    private Bridge() { }

    public static void main(String[] args) {
        // nothing to do at start-up; the page calls the exported methods
    }

    // ------------------------------------------------------------------ course programs

    /**
     * Runs a course program with the given keyboard input. The program runs in a TeaVM green
     * thread so that it may use threads, wait() and sleep(); the callback receives everything it
     * printed once the output has stopped changing (or after 10 s).
     */
    @JSExport
    public static void runProgram(String id, String input, OutputCallback callback) {
        new Thread(() -> {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            PrintStream out = new PrintStream(buffer, true);
            PrintStream oldOut = System.out;
            PrintStream oldErr = System.err;
            BrowserStdin.reset(input);
            System.setOut(out);
            System.setErr(out);
            boolean known = true;
            try {
                known = CourseRegistry.run(id);
            } catch (Throwable t) {
                out.println("Exception in thread \"main\" " + t);
            }
            if (!known) {
                out.println("This program cannot run in the browser.");
            }
            // Programs may still be running in their own threads (Experiment 7): wait until the
            // output has been stable for 100 ms.
            long start = System.currentTimeMillis();
            int lastSize = -1, stable = 0;
            boolean finished = true;
            while (stable < 10) {
                if (System.currentTimeMillis() - start > 10000) {
                    finished = false;
                    break;
                }
                int size = buffer.size();
                stable = size == lastSize ? stable + 1 : 0;
                lastSize = size;
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    break;
                }
            }
            System.setOut(oldOut);
            System.setErr(oldErr);
            callback.done(new String(buffer.toByteArray(), StandardCharsets.UTF_8), finished);
        }).start();
    }

    // ------------------------------------------------------------------ terrain

    /** Loads the terrain/land-cover grid JSON and builds the D8 network. Returns a JSON summary. */
    @JSExport
    public static String loadTerrain(String gridJson) {
        grid = GridData.parse(gridJson);
        model = new CatchmentModel(grid.terrain(), grid.landCover);
        lastResult = null;
        FlowNetwork net = model.network();
        int maxAcc = 0;
        for (int i = 0; i < grid.rows * grid.cols; i++) {
            maxAcc = Math.max(maxAcc, net.accumulation(i));
        }
        return new JsonWriter().beginObject()
                .field("name", grid.name).field("rows", grid.rows).field("cols", grid.cols)
                .field("west", grid.west).field("north", grid.north).field("cellDeg", grid.cellDeg)
                .field("cellSizeX", grid.cellSizeX).field("cellSizeY", grid.cellSizeY)
                .field("outletRow", grid.rowOfLat(grid.outletLat)).field("outletCol", grid.colOfLon(grid.outletLon))
                .field("maxAccumulation", maxAcc)
                .endObject().toString();
    }

    @JSExport
    public static double[] elevations() { return model.grid().elevations(); }

    @JSExport
    public static double[] filledElevations() { return model.network().filledElevations(); }

    @JSExport
    public static int[] accumulations() { return model.network().accumulations(); }

    @JSExport
    public static int[] landCover() { return grid.landCover.clone(); }

    /** Delineates the catchment of the stream cell nearest (row, col). Returns a JSON summary. */
    @JSExport
    public static String delineate(int row, int col, int snapRadius) {
        Watershed ws = model.delineate(row, col, snapRadius);
        lastResult = null;
        int o = ws.outlet();
        double[] c = ws.centroidRowCol();
        return new JsonWriter().beginObject()
                .field("outletRow", model.grid().rowOf(o)).field("outletCol", model.grid().colOf(o))
                .field("outletLat", grid.latOfRow(model.grid().rowOf(o)))
                .field("outletLon", grid.lonOfCol(model.grid().colOf(o)))
                .field("cells", ws.cellCount()).field("areaKm2", ws.areaSquareKilometres())
                .field("longestFlowPathM", ws.longestFlowPathMetres())
                .field("longestPathDropM", ws.longestPathDropMetres())
                .field("longestPathSlope", ws.longestPathSlope())
                .field("farthestRow", model.grid().rowOf(ws.farthestCell()))
                .field("farthestCol", model.grid().colOf(ws.farthestCell()))
                .field("centroidLat", grid.latOfRow(c[0])).field("centroidLon", grid.lonOfCol(c[1]))
                .field("meanElevation", ws.meanElevation()).field("maxElevation", ws.maxElevation())
                .field("outletElevation", model.grid().elevation(o))
                .endObject().toString();
    }

    @JSExport
    public static int[] catchmentMask() { return model.watershed().mask(); }

    @JSExport
    public static double[] flowLengths() { return model.watershed().flowLengths(); }

    /** Receiver index of every cell (-1 drains out of the grid), for drawing flow paths. */
    @JSExport
    public static int[] receivers() {
        int n = model.grid().size();
        int[] r = new int[n];
        for (int i = 0; i < n; i++) {
            r[i] = model.network().receiver(i);
        }
        return r;
    }

    // ------------------------------------------------------------------ rainfall

    @JSExport
    public static String loadRainfallGrid(String json) {
        rainfallGrid = RainfallGrid.parse(json);
        return new JsonWriter().beginObject().field("start", rainfallGrid.startUtc)
                .field("hours", rainfallGrid.hours).endObject().toString();
    }

    /** Hourly series (mm) at (lat, lon) by bilinear interpolation of the loaded rainfall grid. */
    @JSExport
    public static double[] rainfallAt(double lat, double lon, int fromHour, int count) {
        return rainfallGrid.seriesAt(lat, lon, fromHour, count);
    }

    /** Parses hourly rainfall CSV text (last column = mm). Throws on invalid input. */
    @JSExport
    public static double[] parseRainfallCsv(String text) {
        return HourlyRainfall.fromCsv(text).depths();
    }

    // ------------------------------------------------------------------ curve numbers

    /** Curve number for every WorldCover code 0..100 for soil group 0=A .. 3=D (TR-55). */
    @JSExport
    public static double[] curveNumberTable(int soilGroup) {
        return CurveNumberTable.lookup(soilGroup);
    }

    /** WorldCover classes, their TR-55 cover type and CN for groups A-D, as JSON. */
    @JSExport
    public static String curveNumberDescription() {
        JsonWriter w = new JsonWriter().beginArray();
        for (int i = 0; i < CurveNumberTable.CLASSES.length; i++) {
            int c = CurveNumberTable.CLASSES[i];
            w.beginObject().field("code", c).field("coverType", CurveNumberTable.COVER_TYPE[i]);
            w.name("cn").beginArray();
            for (int g = 0; g < 4; g++) {
                w.value(CurveNumberTable.curveNumber(c, g));
            }
            w.endArray().endObject();
        }
        return w.endArray().toString();
    }

    // ------------------------------------------------------------------ simulation

    @JSExport
    public static String simulate(double[] hourlyRainMm, double[] curveNumberByClass, int stepMinutes,
                                  double tcOverrideHours, boolean slopeWeightedVelocity,
                                  double clarkStorageHours, double channelThresholdKm2, int frameMinutes) {
        SimulationSettings s = new SimulationSettings();
        s.curveNumberByClass = curveNumberByClass;
        s.stepMinutes = stepMinutes;
        s.tcOverrideHours = tcOverrideHours;
        s.slopeWeightedVelocity = slopeWeightedVelocity;
        s.clarkStorageHours = clarkStorageHours;
        s.channelThresholdKm2 = channelThresholdKm2;
        s.frameMinutes = frameMinutes;
        lastResult = model.simulate(new HourlyRainfall(hourlyRainMm), s);
        return lastResult.toJson();
    }

    @JSExport
    public static double[] cellCurveNumbers() { return lastResult.cellCurveNumber; }

    @JSExport
    public static double[] cellTravelTimes() { return lastResult.cellTravelTimeHours; }

    @JSExport
    public static int[] channelCells() { return lastResult.channelCells; }

    @JSExport
    public static double[] channelFrames() { return lastResult.frameChannelFlow; }

    @JSExport
    public static double[] frameClassExcess() { return lastResult.frameClassCumulativeExcessMm; }
}
