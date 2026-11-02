package civiltech.hydrology;

/**
 * Linear reservoir routing (storage S = R * O), the attenuation step of the Clark (1945) unit
 * hydrograph. Uses the exact solution of dS/dt = I - O for inflow varying linearly within a step:
 * <pre>
 *   k  = exp(-dt / R),  a = R / dt
 *   O2 = I2 - a (I2 - I1) + (O1 - I1 + a (I2 - I1)) k
 * </pre>
 * The scheme is stable for every R > 0 and returns the inflow unchanged when R = 0.
 */
public final class LinearReservoir {

    private LinearReservoir() { }

    public static double[] route(double[] inflow, double storageCoefficientHours, double stepHours) {
        if (storageCoefficientHours < 0 || !(stepHours > 0)) {
            throw new IllegalArgumentException("R must be >= 0 and the step positive");
        }
        double[] out = new double[inflow.length];
        if (storageCoefficientHours == 0.0) {
            System.arraycopy(inflow, 0, out, 0, inflow.length);
            return out;
        }
        double k = Math.exp(-stepHours / storageCoefficientHours);
        double a = storageCoefficientHours / stepHours;
        out[0] = 0.0;
        for (int t = 1; t < inflow.length; t++) {
            double i1 = inflow[t - 1], i2 = inflow[t];
            out[t] = i2 - a * (i2 - i1) + (out[t - 1] - i1 + a * (i2 - i1)) * k;
        }
        return out;
    }
}
