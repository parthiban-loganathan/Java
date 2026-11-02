import civiltech.hydrology.CurveNumberTable;
import civiltech.hydrology.Kirpich;
import civiltech.lab.GridData;
import civiltech.terrain.FlowNetwork;
import civiltech.terrain.TerrainGrid;
import civiltech.terrain.Watershed;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Splits the default Vellore hill catchment into the tributary sub-catchments that join its main
 * stream, plus the remaining area along the main stream, and writes them as CSV for the starter
 * template. All values are derived from the open grid file; nothing is estimated by hand.
 *
 * java -cp java/civiltech-core/target/classes tools/data-prep/SubCatchments.java GRID.json MIN_KM2 OUT.csv
 */
public class SubCatchments {
    public static void main(String[] a) throws Exception {
        GridData d = GridData.parse(Files.readString(Path.of(a[0])));
        double minKm2 = Double.parseDouble(a[1]);
        TerrainGrid g = d.terrain();
        FlowNetwork net = FlowNetwork.build(g);
        int outlet = g.index(d.rowOfLat(d.outletLat), d.colOfLon(d.outletLon));
        Watershed whole = new Watershed(net, outlet);
        double cellKm2 = g.cellArea() / 1e6;
        int n = g.size();
        // donors of each cell
        List<List<Integer>> donors = new ArrayList<>();
        for (int i = 0; i < n; i++) donors.add(new ArrayList<>());
        for (int i = 0; i < n; i++) if (net.receiver(i) >= 0) donors.get(net.receiver(i)).add(i);
        // main stem: from the outlet, always step to the donor with the largest accumulation
        boolean[] stem = new boolean[n];
        List<Integer> tributaryOutlets = new ArrayList<>();
        int c = outlet;
        while (true) {
            stem[c] = true;
            int best = -1;
            for (int k : donors.get(c)) if (best < 0 || net.accumulation(k) > net.accumulation(best)) best = k;
            if (best < 0) break;
            for (int k : donors.get(c)) if (k != best && net.accumulation(k) * cellKm2 >= minKm2) tributaryOutlets.add(k);
            c = best;
        }
        double[] cn = CurveNumberTable.lookup(2);
        boolean[] assigned = new boolean[n];
        StringBuilder out = new StringBuilder();
        out.append("# Sub-catchments of the hill catchment south-east of VIT Vellore (outlet 12.94208 N, 79.26014 E).\n");
        out.append("# Derived with civiltech-core (Priority-Flood, D8) from Copernicus GLO-30 DEM and ESA WorldCover 2021 v200.\n");
        out.append("# curve_number: area-weighted TR-55 value for hydrologic soil group C (assumed). tc_h: Kirpich (1940).\n");
        out.append("name,outlet_lat,outlet_lon,centroid_lat,centroid_lon,area_km2,curve_number,flow_length_m,slope,tc_h\n");
        int idx = 1;
        double total = 0;
        for (int t : tributaryOutlets) {
            Watershed w = new Watershed(net, t);
            double sum = 0;
            for (int i = 0; i < n; i++) if (w.contains(i)) { sum += cn[d.landCover[i]]; assigned[i] = true; }
            double[] ce = w.centroidRowCol();
            out.append(String.format(Locale.ROOT, "Tributary %d,%.5f,%.5f,%.5f,%.5f,%.3f,%.1f,%.0f,%.4f,%.2f%n", idx++,
                d.latOfRow(g.rowOf(t)), d.lonOfCol(g.colOf(t)), d.latOfRow(ce[0]), d.lonOfCol(ce[1]),
                w.areaSquareKilometres(), sum / w.cellCount(), w.longestFlowPathMetres(), w.longestPathSlope(),
                Kirpich.hours(w.longestFlowPathMetres(), w.longestPathSlope())));
            total += w.areaSquareKilometres();
        }
        // residual: catchment cells not inside any listed tributary
        double sum = 0, sr = 0, sc = 0; int cnt = 0;
        for (int i = 0; i < n; i++) if (whole.contains(i) && !assigned[i]) { sum += cn[d.landCover[i]]; sr += g.rowOf(i); sc += g.colOf(i); cnt++; }
        out.append(String.format(Locale.ROOT, "Main stream corridor,%.5f,%.5f,%.5f,%.5f,%.3f,%.1f,,,%n",
            d.latOfRow(g.rowOf(outlet)), d.lonOfCol(g.colOf(outlet)), d.latOfRow(sr / cnt), d.lonOfCol(sc / cnt), cnt * cellKm2, sum / cnt));
        total += cnt * cellKm2;
        Files.writeString(Path.of(a[2]), out.toString());
        System.out.print(out);
        System.out.printf(Locale.ROOT, "total %.3f km2 vs catchment %.3f km2%n", total, whole.areaSquareKilometres());
    }
}
