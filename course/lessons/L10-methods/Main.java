// L10: static methods, parameters, return values, overloading and pass-by-value.
import java.util.Arrays;

public class Main {

    /** Rational Method peak flow (m^3/s): C runoff coefficient, i intensity (mm/h), A area (ha). */
    static double peakFlow(double c, double intensityMmPerHour, double areaHa) {
        return c * intensityMmPerHour * areaHa / 360.0;
    }

    /** Kirpich (1940) time of concentration in minutes: L flow length (m), S slope (m/m). */
    static double kirpichMinutes(double lengthM, double slope) {
        return 0.0195 * Math.pow(lengthM, 0.77) * Math.pow(slope, -0.385);
    }

    // Overloaded: same name, different parameter lists
    static double area(double radius) {
        return Math.PI * radius * radius;
    }

    static double area(double width, double depth) {
        return width * depth;
    }

    static void doubleAll(double[] values) {      // receives a copy of the REFERENCE
        for (int i = 0; i < values.length; i++) {
            values[i] *= 2;
        }
    }

    static void tryToChange(int n) {              // receives a copy of the VALUE
        n = 999;
    }

    public static void main(String[] args) {
        double q = peakFlow(0.7, 60.0, 12.5);
        System.out.printf("Rational method: Q = %.3f m^3/s%n", q);

        double tc = kirpichMinutes(1500, 0.02);
        System.out.printf("Kirpich Tc = %.1f min%n", tc);

        System.out.printf("area(0.3)      = %.4f m^2 (circle)%n", area(0.3));
        System.out.printf("area(0.3, 0.5) = %.4f m^2 (rectangle)%n", area(0.3, 0.5));

        double[] loads = {1.5, 2.0, 3.25};
        doubleAll(loads);
        System.out.println("after doubleAll: " + Arrays.toString(loads));

        int bars = 4;
        tryToChange(bars);
        System.out.println("after tryToChange: bars = " + bars);
    }
}
