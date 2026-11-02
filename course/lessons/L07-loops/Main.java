// L07: for, while, nested loops and the accumulator pattern (SCS Curve Number runoff).
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        double cn = sc.nextDouble();           // curve number
        int hours = sc.nextInt();              // number of hourly values that follow

        double s = 25400.0 / cn - 254.0;       // potential maximum retention, mm
        double ia = 0.2 * s;                   // initial abstraction, mm
        System.out.printf("CN = %.0f, S = %.1f mm, Ia = %.1f mm%n", cn, s, ia);
        System.out.println("Hour  Rain  CumRain  CumRunoff  Runoff");

        double cumRain = 0, previousQ = 0;
        for (int h = 1; h <= hours; h++) {
            double rain = sc.nextDouble();
            cumRain += rain;                                    // accumulator
            double q = 0;
            if (cumRain > ia) {
                q = (cumRain - ia) * (cumRain - ia) / (cumRain - ia + s);
            }
            System.out.printf("%4d %5.1f %8.1f %10.2f %7.2f%n", h, rain, cumRain, q, q - previousQ);
            previousQ = q;
        }
        System.out.printf("Runoff coefficient = %.3f%n", previousQ / cumRain);

        // while: hours for a 40 mm pond to drain at 6 mm/h
        double pond = 40;
        int drainHours = 0;
        while (pond > 0) {
            pond -= 6;
            drainHours++;
        }
        System.out.println("Pond empties after " + drainHours + " h");

        // nested loops: runoff Q (mm) for P = 50, 100, 150 mm and CN = 60, 75, 90
        System.out.println("P\\CN    60     75     90");
        for (int p = 50; p <= 150; p += 50) {
            System.out.printf("%3d", p);
            for (int c = 60; c <= 90; c += 15) {
                double sr = 25400.0 / c - 254.0;
                double q = p > 0.2 * sr ? Math.pow(p - 0.2 * sr, 2) / (p + 0.8 * sr) : 0.0;
                System.out.printf(" %6.1f", q);
            }
            System.out.println();
        }
        sc.close();
    }
}
