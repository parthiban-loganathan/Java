package app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Runs the same calculation on the JVM, without a browser:
 *   mvn -q compile
 *   java -cp target/classes app.ConsoleApp web/data/subcatchments.csv web/data/rainfall_fengal_2024.csv
 */
public class ConsoleApp {
    public static void main(String[] args) throws Exception {
        String subs = Files.readString(Path.of(args[0]));
        String rain = Files.readString(Path.of(args[1]));
        double[] r = RunoffCalculator.readRainfall(rain);
        double total = 0;
        for (double v : r) total += v;
        System.out.printf("%d hours of rainfall, total %.1f mm%n", r.length, total);
        System.out.printf("%-22s %9s %5s %10s %6s %12s%n", "sub-catchment", "area km2", "CN", "runoff mm", "C", "volume m3");
        List<SubCatchment> list = RunoffCalculator.readSubCatchments(subs);
        for (SubCatchment s : list) {
            double depth = 0;
            for (double q : s.hourlyRunoffMm(r)) depth += q;
            System.out.printf("%-22s %9.3f %5.1f %10.1f %6.2f %12.0f%n", s.getName(), s.getAreaKm2(),
                    s.getCurveNumber(), depth, depth / total, s.volumeM3(depth));
        }
    }
}
