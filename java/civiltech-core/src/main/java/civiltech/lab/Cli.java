package civiltech.lab;

import civiltech.hydrology.CurveNumberTable;
import civiltech.hydrology.HourlyRainfall;
import civiltech.terrain.Watershed;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Command-line run of the catchment model on the JVM (the same code the web page and the Android
 * app use).
 *
 * <pre>
 * java -cp civiltech-core.jar civiltech.lab.Cli GRID.json RAIN.csv [options]
 *   --from H      first row of the rainfall CSV to use (default 0)
 *   --hours N     number of hourly values (default: all remaining)
 *   --soil A|B|C|D  hydrologic soil group for the TR-55 table (default C)
 *   --step M      computational step in minutes (default 10)
 *   --row R --col C --snap S   outlet cell and snap radius (default: file's outlet, snap 0)
 *   --slope       slope-weighted travel times
 *   --clark R     Clark storage coefficient in hours (default 0)
 * </pre>
 */
public final class Cli {

    private Cli() { }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: Cli GRID.json RAIN.csv [--from H] [--hours N] [--soil C] [--step 10] "
                    + "[--row R --col C --snap S] [--slope] [--clark R]");
            System.exit(2);
        }
        GridData grid = GridData.parse(new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8));
        double[] all = HourlyRainfall.fromCsv(new String(Files.readAllBytes(Paths.get(args[1])), StandardCharsets.UTF_8)).depths();
        int from = 0, hours = -1, row = grid.rowOfLat(grid.outletLat), col = grid.colOfLon(grid.outletLon), snap = 0;
        SimulationSettings s = new SimulationSettings();
        for (int i = 2; i < args.length; i++) {
            switch (args[i]) {
                case "--from": from = Integer.parseInt(args[++i]); break;
                case "--hours": hours = Integer.parseInt(args[++i]); break;
                case "--soil": s.curveNumberByClass = CurveNumberTable.lookup(CurveNumberTable.soilGroupIndex(args[++i])); break;
                case "--step": s.stepMinutes = Integer.parseInt(args[++i]); break;
                case "--row": row = Integer.parseInt(args[++i]); break;
                case "--col": col = Integer.parseInt(args[++i]); break;
                case "--snap": snap = Integer.parseInt(args[++i]); break;
                case "--slope": s.slopeWeightedVelocity = true; break;
                case "--clark": s.clarkStorageHours = Double.parseDouble(args[++i]); break;
                default: throw new IllegalArgumentException("unknown option " + args[i]);
            }
        }
        if (hours < 0) {
            hours = all.length - from;
        }
        double[] rain = new double[hours];
        System.arraycopy(all, from, rain, 0, hours);

        CatchmentModel model = new CatchmentModel(grid.terrain(), grid.landCover);
        Watershed ws = model.delineate(row, col, snap);
        SimulationResult r = model.simulate(new HourlyRainfall(rain), s);
        System.out.println(r.summaryJson());
        System.out.printf("outlet row %d col %d, area %.3f km2%n", model.grid().rowOf(ws.outlet()),
                model.grid().colOf(ws.outlet()), ws.areaSquareKilometres());
        System.out.println("hour  rain_mm  excess_mm  Q_scs_m3s  Q_clark_m3s");
        for (int h = 0; h < r.hourlyRainMm.length; h++) {
            System.out.printf("%4d %8.2f %10.3f %10.3f %12.3f%n", h + 1, r.hourlyRainMm[h], r.hourlyExcessMm[h],
                    r.hourlyMeanFlowScs[h], r.hourlyMeanFlowClark[h]);
        }
    }
}
