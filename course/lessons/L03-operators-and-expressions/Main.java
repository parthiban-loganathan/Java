// L03: arithmetic, precedence, Math functions, integer division and remainder.
public class Main {
    public static void main(String[] args) {
        double w = 15.0;   // uniformly distributed load, kN/m
        double L = 6.0;    // span, m

        double reaction = w * L / 2;            // kN
        double moment = w * L * L / 8;          // kN m (* and / evaluated left to right)
        System.out.println("Reaction R = wL/2   = " + reaction + " kN");
        System.out.println("Moment   M = wL^2/8 = " + moment + " kN m");

        double d = 16;                          // bar diameter, mm
        double barArea = Math.PI * Math.pow(d, 2) / 4;
        System.out.printf("Area of one 16 mm bar = %.2f mm^2%n", barArea);
        System.out.printf("Area of 4 bars        = %.2f mm^2%n", 4 * barArea);

        // Integer division throws the fraction away
        System.out.println("7 / 2   = " + (7 / 2));
        System.out.println("7.0 / 2 = " + (7.0 / 2));
        System.out.println("1 / 3 * 300   = " + (1 / 3 * 300));
        System.out.println("1.0 / 3 * 300 = " + (1.0 / 3 * 300));

        // Remainder: 1250 mm of bar left after cutting 400 mm pieces
        int barLength = 1250, piece = 400;
        System.out.println("Pieces = " + (barLength / piece) + ", offcut = " + (barLength % piece) + " mm");

        // Compound assignment and increment
        double totalLoad = 0;
        totalLoad += 12.5;   // dead load
        totalLoad += 7.5;    // live load
        totalLoad *= 1.5;    // load factor
        int count = 0;
        count++;
        System.out.println("Factored load = " + totalLoad + " kN/m, count = " + count);

        // Precedence: parentheses change the result
        System.out.println("2 + 3 * 4   = " + (2 + 3 * 4));
        System.out.println("(2 + 3) * 4 = " + ((2 + 3) * 4));

        // Trigonometry uses radians: horizontal component of a 10 kN force at 30 degrees
        double fx = 10 * Math.cos(Math.toRadians(30));
        System.out.printf("Fx = %.3f kN%n", fx);
    }
}
