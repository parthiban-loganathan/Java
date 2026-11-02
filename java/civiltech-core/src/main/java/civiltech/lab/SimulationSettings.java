package civiltech.lab;

import civiltech.hydrology.CurveNumberTable;

/** User-adjustable parameters of a rainfall-runoff simulation. Defaults are listed per field. */
public final class SimulationSettings {

    /** Curve number for every WorldCover code (index 0..100). Default: TR-55 table, soil group C. */
    public double[] curveNumberByClass = CurveNumberTable.lookup(2);
    /** Computational time step in minutes; must divide 60. Default 10. */
    public int stepMinutes = 10;
    /** Time of concentration override in hours; 0 means "use Kirpich". */
    public double tcOverrideHours = 0.0;
    /** Travel-time model for the time-area method: false = uniform velocity, true = slope weighted. */
    public boolean slopeWeightedVelocity = false;
    /** Clark storage coefficient R in hours applied to the time-area hydrograph; 0 = translation only. */
    public double clarkStorageHours = 0.0;
    /** Minimum upstream area (km2) for a cell to be animated as part of the stream network. */
    public double channelThresholdKm2 = 0.25;
    /** Interval between animation frames in minutes (multiple of stepMinutes). */
    public int frameMinutes = 30;
    /** Minimum slope (m/m) used for a D8 step in the slope-weighted velocity model. */
    public double minimumStepSlope = 0.001;

    void validate() {
        if (stepMinutes <= 0 || 60 % stepMinutes != 0) {
            throw new IllegalArgumentException("step must divide 60 minutes (e.g. 5, 10, 15, 30, 60)");
        }
        if (frameMinutes < stepMinutes || frameMinutes % stepMinutes != 0) {
            throw new IllegalArgumentException("frame interval must be a multiple of the step");
        }
        if (clarkStorageHours < 0 || tcOverrideHours < 0 || channelThresholdKm2 < 0) {
            throw new IllegalArgumentException("R, Tc override and channel threshold cannot be negative");
        }
        if (curveNumberByClass == null || curveNumberByClass.length < 101) {
            throw new IllegalArgumentException("curve number lookup must have 101 entries (WorldCover codes)");
        }
    }
}
