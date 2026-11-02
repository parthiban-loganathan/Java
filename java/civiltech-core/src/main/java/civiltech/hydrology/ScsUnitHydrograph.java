package civiltech.hydrology;

/**
 * SCS (NRCS) curvilinear dimensionless unit hydrograph, NEH Part 630 Chapter 16, Table 16-1.
 *
 * <pre>
 *   lag  Tl = 0.6 Tc                         (NEH-630 Ch. 15)
 *   Tp      = D / 2 + Tl                     time to peak, D = duration of the excess pulse
 *   qp      = 0.208 A / Tp                   m^3/s per mm of runoff, A in km^2, Tp in h
 * </pre>
 * 0.208 is the SI form of the standard peak rate factor 484. Ordinates between the tabulated
 * points are linearly interpolated. Because the tabulated curve integrates only approximately to
 * one unit of runoff, the ordinates are rescaled so the unit hydrograph holds exactly 1 mm over
 * the catchment; the applied factor is available from {@link #volumeCorrection()}.
 */
public final class ScsUnitHydrograph {

    static final double[] T_TP = {
        0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7,
        1.8, 1.9, 2.0, 2.2, 2.4, 2.6, 2.8, 3.0, 3.2, 3.4, 3.6, 3.8, 4.0, 4.5, 5.0};
    static final double[] Q_QP = {
        0.000, 0.030, 0.100, 0.190, 0.310, 0.470, 0.660, 0.820, 0.930, 0.990, 1.000, 0.990, 0.930,
        0.860, 0.780, 0.680, 0.560, 0.460, 0.390, 0.330, 0.280, 0.207, 0.147, 0.107, 0.077, 0.055,
        0.040, 0.029, 0.021, 0.015, 0.011, 0.005, 0.000};

    private final double[] ordinates;
    private final double peakTimeHours;
    private final double peakRate;
    private final double correction;

    /**
     * @param areaKm2     catchment area
     * @param tcHours     time of concentration
     * @param stepHours   duration D of one excess pulse and the output time step
     */
    public ScsUnitHydrograph(double areaKm2, double tcHours, double stepHours) {
        if (!(areaKm2 > 0) || !(tcHours > 0) || !(stepHours > 0)) {
            throw new IllegalArgumentException("area, Tc and step must be positive");
        }
        peakTimeHours = stepHours / 2.0 + 0.6 * tcHours;
        peakRate = 0.208 * areaKm2 / peakTimeHours;
        int n = (int) Math.ceil(5.0 * peakTimeHours / stepHours) + 1;
        double[] u = new double[n];
        double volume = 0.0;
        for (int j = 0; j < n; j++) {
            u[j] = peakRate * dimensionless(j * stepHours / peakTimeHours);
            volume += u[j] * stepHours * 3600.0;
        }
        double target = areaKm2 * 1.0e6 * 1.0e-3;   // 1 mm over the catchment, m^3
        correction = target / volume;
        for (int j = 0; j < n; j++) {
            u[j] *= correction;
        }
        ordinates = u;
    }

    /** q/qp for a given t/Tp, linear interpolation of Table 16-1; 0 beyond t/Tp = 5. */
    public static double dimensionless(double tOverTp) {
        if (tOverTp <= 0.0 || tOverTp >= 5.0) {
            return 0.0;
        }
        int i = 1;
        while (T_TP[i] < tOverTp) {
            i++;
        }
        double f = (tOverTp - T_TP[i - 1]) / (T_TP[i] - T_TP[i - 1]);
        return Q_QP[i - 1] + f * (Q_QP[i] - Q_QP[i - 1]);
    }

    /** Unit hydrograph ordinates (m^3/s per mm of excess in one step) at t = 0, D, 2D, ... */
    public double[] ordinates() { return ordinates.clone(); }
    public double peakTimeHours() { return peakTimeHours; }
    /** Peak rate before the volume correction, m^3/s per mm. */
    public double peakRate() { return peakRate; }
    public double volumeCorrection() { return correction; }

    /** Direct-runoff hydrograph: discrete convolution of excess depths (mm per step) with the UH. */
    public double[] convolve(double[] excessMm, int outputLength) {
        double[] q = new double[outputLength];
        for (int s = 0; s < excessMm.length; s++) {
            if (excessMm[s] == 0.0) {
                continue;
            }
            for (int j = 0; j < ordinates.length && s + j < outputLength; j++) {
                q[s + j] += excessMm[s] * ordinates[j];
            }
        }
        return q;
    }
}
