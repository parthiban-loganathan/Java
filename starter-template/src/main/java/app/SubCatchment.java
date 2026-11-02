package app;

/**
 * One sub-catchment: an area of land that drains to one point.
 *
 * CHANGE THIS: replace the fields and methods with the objects of your own project
 * (a beam, a footing, a road segment, a building, a rain-water tank, ...).
 */
public class SubCatchment {

    private final String name;
    private final double latitude;      // degrees north
    private final double longitude;     // degrees east
    private final double areaKm2;       // plan area, km^2
    private final double curveNumber;   // SCS curve number, 30..100

    public SubCatchment(String name, double latitude, double longitude, double areaKm2, double curveNumber) {
        if (areaKm2 <= 0) {
            throw new IllegalArgumentException(name + ": area must be positive");
        }
        if (curveNumber <= 0 || curveNumber > 100) {
            throw new IllegalArgumentException(name + ": curve number must be between 1 and 100");
        }
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.areaKm2 = areaKm2;
        this.curveNumber = curveNumber;
    }

    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getAreaKm2() { return areaKm2; }
    public double getCurveNumber() { return curveNumber; }

    /** Potential maximum retention S in mm (USDA NRCS, NEH Part 630, Chapter 10). */
    public double retentionMm() {
        return 25400.0 / curveNumber - 254.0;
    }

    /** Cumulative direct runoff Q (mm) for cumulative rainfall P (mm); Ia = 0.2 S. */
    public double cumulativeRunoffMm(double rainfallMm) {
        double s = retentionMm();
        double ia = 0.2 * s;
        if (rainfallMm <= ia) {
            return 0.0;
        }
        return (rainfallMm - ia) * (rainfallMm - ia) / (rainfallMm - ia + s);
    }

    /** Runoff depth (mm) in each hour for hourly rainfall depths (mm). */
    public double[] hourlyRunoffMm(double[] hourlyRainMm) {
        double[] runoff = new double[hourlyRainMm.length];
        double cumulativeRain = 0.0;
        double previous = 0.0;
        for (int h = 0; h < hourlyRainMm.length; h++) {
            cumulativeRain += hourlyRainMm[h];
            double q = cumulativeRunoffMm(cumulativeRain);
            runoff[h] = q - previous;
            previous = q;
        }
        return runoff;
    }

    /** Converts a runoff depth over this sub-catchment to a volume: mm x km^2 = 1000 m^3. */
    public double volumeM3(double depthMm) {
        return depthMm * areaKm2 * 1000.0;
    }
}
