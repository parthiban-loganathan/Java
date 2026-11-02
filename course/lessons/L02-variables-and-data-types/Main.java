// L02: one variable of every primitive type, described with a reinforced-concrete beam.
public class Main {
    public static void main(String[] args) {
        byte floorsBelowGround = 2;             // small whole number
        short spanMillimetres = 6000;           // whole number up to 32 767
        int numberOfBars = 4;                   // the usual choice for counts
        long designLifeSeconds = 50L * 365 * 24 * 3600; // L makes the arithmetic use 64 bits
        float coverMetres = 0.025f;             // f marks a float literal
        double spanMetres = 6.0;                // the usual choice for measurements
        char concreteGradeLetter = 'M';         // single quotes for one character
        boolean isDoublyReinforced = false;     // true or false only

        System.out.println("byte    floorsBelowGround  = " + floorsBelowGround);
        System.out.println("short   spanMillimetres    = " + spanMillimetres);
        System.out.println("int     numberOfBars       = " + numberOfBars);
        System.out.println("long    designLifeSeconds  = " + designLifeSeconds);
        System.out.println("float   coverMetres        = " + coverMetres);
        System.out.println("double  spanMetres         = " + spanMetres);
        System.out.println("char    concreteGradeLetter= " + concreteGradeLetter);
        System.out.println("boolean isDoublyReinforced = " + isDoublyReinforced);

        // Limits of int and what happens when they are exceeded (overflow wraps around)
        int biggest = Integer.MAX_VALUE;
        System.out.println("Integer.MAX_VALUE     = " + biggest);
        System.out.println("Integer.MAX_VALUE + 1 = " + (biggest + 1));

        // Binary floating point cannot store 0.1 exactly
        System.out.println("0.1 + 0.2 = " + (0.1 + 0.2));

        // Constants and casting
        final double UNIT_WEIGHT_CONCRETE = 25.0;      // kN/m^3, cannot be reassigned
        double selfWeight = 0.3 * 0.5 * spanMetres * UNIT_WEIGHT_CONCRETE; // b x D x L x gamma
        System.out.println("Self-weight of 300 x 500 beam = " + selfWeight + " kN");
        System.out.printf("Rounded for the report      = %.2f kN%n", selfWeight);
        double barsNeeded = 3.4;
        int truncated = (int) barsNeeded;               // cast: fraction discarded
        long rounded = Math.round(barsNeeded);          // nearest whole number
        int providedBars = (int) Math.ceil(barsNeeded); // engineers round reinforcement up
        System.out.println("(int) 3.4 = " + truncated + ", Math.round(3.4) = " + rounded
                + ", Math.ceil(3.4) = " + providedBars);

        var storeys = 12;                               // compiler infers int
        System.out.println("var storeys = " + storeys);
    }
}
