package civiltech.hydrology;

/**
 * SCS Curve Number runoff equation (USDA NRCS, National Engineering Handbook Part 630, Chapter 10).
 *
 * <pre>
 *   S  = 25400 / CN - 254                 potential maximum retention (mm)
 *   Ia = lambda * S                       initial abstraction (mm), lambda = 0.2 in NEH-630
 *   Q  = (P - Ia)^2 / (P - Ia + S)        for P > Ia, otherwise Q = 0   (P, Q cumulative, mm)
 * </pre>
 *
 * The equation is applied to cumulative storm rainfall; runoff in a time step is the difference of
 * cumulative runoff at the end and the start of the step.
 */
public final class ScsCurveNumber {

    /** Initial abstraction ratio used in NEH-630 Chapter 10. */
    public static final double LAMBDA = 0.2;

    private ScsCurveNumber() { }

    private static void check(double cn) {
        if (!(cn > 0.0 && cn <= 100.0)) {
            throw new IllegalArgumentException("curve number must be in (0, 100], got " + cn);
        }
    }

    /** Potential maximum retention S in millimetres. */
    public static double retentionMm(double cn) {
        check(cn);
        return 25400.0 / cn - 254.0;
    }

    /** Cumulative direct runoff Q (mm) for cumulative rainfall P (mm). */
    public static double cumulativeRunoffMm(double rainfallMm, double cn) {
        double s = retentionMm(cn);
        double ia = LAMBDA * s;
        if (rainfallMm <= ia) {
            return 0.0;
        }
        double pe = rainfallMm - ia;
        return pe * pe / (pe + s);
    }

    /**
     * dQ/dP: the fraction of the current rainfall intensity that becomes runoff when the cumulative
     * rainfall is P. Equal to 1 - S^2 / (P - Ia + S)^2 for P > Ia, otherwise 0.
     */
    public static double runoffFraction(double rainfallMm, double cn) {
        double s = retentionMm(cn);
        double ia = LAMBDA * s;
        if (rainfallMm <= ia) {
            return 0.0;
        }
        double d = rainfallMm - ia + s;
        return d <= 0.0 ? 1.0 : 1.0 - (s * s) / (d * d);
    }
}
