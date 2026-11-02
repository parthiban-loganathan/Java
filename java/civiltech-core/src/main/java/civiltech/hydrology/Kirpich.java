package civiltech.hydrology;

/**
 * Kirpich (1940) time of concentration, SI form:
 * <pre>  Tc [min] = 0.0195 * L^0.77 * S^-0.385   (L = flow length in m, S = slope in m/m)</pre>
 * Kirpich, Z.P. (1940), "Time of concentration of small agricultural watersheds",
 * Civil Engineering 10(6), 362. Developed for small, steep, rural catchments in Tennessee
 * (0.4 to 45 hectares); applying it to larger catchments is an extrapolation.
 */
public final class Kirpich {

    private Kirpich() { }

    /** Time of concentration in minutes. */
    public static double minutes(double flowLengthM, double slope) {
        if (!(flowLengthM > 0) || !(slope > 0)) {
            throw new IllegalArgumentException("flow length and slope must be positive");
        }
        return 0.0195 * Math.pow(flowLengthM, 0.77) * Math.pow(slope, -0.385);
    }

    /** Time of concentration in hours. */
    public static double hours(double flowLengthM, double slope) {
        return minutes(flowLengthM, slope) / 60.0;
    }
}
