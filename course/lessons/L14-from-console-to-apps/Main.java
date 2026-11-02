// L14: a pure logic class (no input/output) and a thin console front end that exchanges CSV/JSON.
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Engineering logic only: no Scanner, no System.out. Reusable on the web and on Android. */
class RunoffCalculator {
    private final double retention;          // S, mm
    private final double initialAbstraction; // Ia, mm

    RunoffCalculator(double curveNumber) {
        this.retention = 25400.0 / curveNumber - 254.0;
        this.initialAbstraction = 0.2 * retention;
    }

    /** Hourly direct runoff (mm) for hourly rainfall (mm), SCS Curve Number on cumulative depth. */
    double[] hourlyRunoff(double[] rain) {
        double[] q = new double[rain.length];
        double cumRain = 0, previous = 0;
        for (int i = 0; i < rain.length; i++) {
            cumRain += rain[i];
            double cumQ = cumRain > initialAbstraction
                    ? Math.pow(cumRain - initialAbstraction, 2) / (cumRain - initialAbstraction + retention)
                    : 0.0;
            q[i] = cumQ - previous;
            previous = cumQ;
        }
        return q;
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        double cn = Double.parseDouble(sc.nextLine().trim());
        List<String> times = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        while (sc.hasNextLine()) {
            String line = sc.nextLine().trim();
            if (line.isEmpty() || !Character.isDigit(line.charAt(0))) continue; // skip blank and header lines
            String[] parts = line.split(",");
            times.add(parts[0]);
            values.add(Double.parseDouble(parts[parts.length - 1]));
        }
        double[] rain = new double[values.size()];
        for (int i = 0; i < rain.length; i++) rain[i] = values.get(i);

        double[] runoff = new RunoffCalculator(cn).hourlyRunoff(rain);

        System.out.println("time (IST)         rain mm  runoff mm");
        double totalRain = 0, totalRunoff = 0;
        for (int i = 0; i < rain.length; i++) {
            System.out.printf("%-18s %8.2f %10.2f%n", times.get(i), rain[i], runoff[i]);
            totalRain += rain[i];
            totalRunoff += runoff[i];
        }
        StringBuilder json = new StringBuilder("{\"cn\":").append(cn).append(",\"runoff\":[");
        for (int i = 0; i < runoff.length; i++) {
            if (i > 0) json.append(',');
            json.append(String.format("%.2f", runoff[i]));
        }
        json.append("],\"totalRain\":").append(String.format("%.2f", totalRain))
            .append(",\"totalRunoff\":").append(String.format("%.2f", totalRunoff)).append('}');
        System.out.println(json);
        sc.close();
    }
}
