package civiltech.hydrology;

/**
 * Hourly rainfall depths, assumed to fall at a constant intensity within each hour.
 * Hour n covers the interval [n, n + 1) hours from the start of the simulation.
 */
public final class HourlyRainfall {

    private final double[] depth;
    private final double[] cumulative;   // cumulative[n] = rain before hour n

    public HourlyRainfall(double[] hourlyDepthMm) {
        if (hourlyDepthMm.length == 0) {
            throw new IllegalArgumentException("at least one hourly value is required");
        }
        depth = hourlyDepthMm.clone();
        cumulative = new double[depth.length + 1];
        for (int n = 0; n < depth.length; n++) {
            if (depth[n] < 0 || Double.isNaN(depth[n])) {
                throw new IllegalArgumentException("rainfall in hour " + n + " is negative or missing");
            }
            cumulative[n + 1] = cumulative[n] + depth[n];
        }
    }

    public int hours() { return depth.length; }
    public double depth(int hour) { return depth[hour]; }
    public double total() { return cumulative[depth.length]; }
    public double[] depths() { return depth.clone(); }

    /** Intensity (mm/h) at time t hours. */
    public double intensity(double t) {
        if (t < 0 || t >= depth.length) {
            return 0.0;
        }
        return depth[(int) Math.floor(t)];
    }

    /** Cumulative rainfall (mm) from time 0 to time t hours. */
    public double cumulative(double t) {
        if (t <= 0) {
            return 0.0;
        }
        if (t >= depth.length) {
            return total();
        }
        int n = (int) Math.floor(t);
        return cumulative[n] + depth[n] * (t - n);
    }

    /**
     * Parses rainfall from CSV text. The last numeric column of every data row is used; lines that
     * start with '#' and a header line are skipped. Accepts ',' ';' or tab separators.
     */
    public static HourlyRainfall fromCsv(String text) {
        String[] lines = text.split("\\r?\\n");
        double[] values = new double[lines.length];
        int count = 0;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("[,;\\t]");
            String last = parts[parts.length - 1].trim();
            try {
                values[count] = Double.parseDouble(last);
                count++;
            } catch (NumberFormatException e) {
                if (count > 0) {
                    throw new IllegalArgumentException("not a number in line: " + line);
                }
                // first non-numeric line is the header
            }
        }
        double[] v = new double[count];
        System.arraycopy(values, 0, v, 0, count);
        return new HourlyRainfall(v);
    }
}
