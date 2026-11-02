package civiltech.lab;

import civiltech.io.MiniJson;
import java.util.List;
import java.util.Map;

/**
 * Hourly precipitation on a regular latitude/longitude grid (era5_precip_grid.json):
 * precip[i][j][t] for latitude i (descending), longitude j (ascending) and hour t.
 */
public final class RainfallGrid {

    public final double[] lats;
    public final double[] lons;
    public final double[][][] precip;
    public final String startUtc;
    public final int hours;

    private RainfallGrid(Map<String, Object> m) {
        lats = MiniJson.doubles(m.get("lats"));
        lons = MiniJson.doubles(m.get("lons"));
        startUtc = (String) m.get("start");
        List<Object> pi = MiniJson.list(m.get("precip"));
        precip = new double[lats.length][lons.length][];
        for (int i = 0; i < lats.length; i++) {
            List<Object> pj = MiniJson.list(pi.get(i));
            for (int j = 0; j < lons.length; j++) {
                precip[i][j] = MiniJson.doubles(pj.get(j));
            }
        }
        hours = precip[0][0].length;
    }

    public static RainfallGrid parse(String json) {
        return new RainfallGrid(MiniJson.object(MiniJson.parse(json)));
    }

    /**
     * Bilinear interpolation of the hourly series at (lat, lon), for hours [fromHour, fromHour + count).
     * The point must lie inside the grid.
     */
    public double[] seriesAt(double lat, double lon, int fromHour, int count) {
        int i = 0;
        while (i < lats.length - 2 && lats[i + 1] > lat) i++;
        int j = 0;
        while (j < lons.length - 2 && lons[j + 1] < lon) j++;
        if (lat > lats[0] || lat < lats[lats.length - 1] || lon < lons[0] || lon > lons[lons.length - 1]) {
            throw new IllegalArgumentException("point outside the rainfall grid");
        }
        double u = (lon - lons[j]) / (lons[j + 1] - lons[j]);
        double v = (lats[i] - lat) / (lats[i] - lats[i + 1]);
        double[] out = new double[count];
        for (int t = 0; t < count; t++) {
            int h = fromHour + t;
            out[t] = (1 - u) * (1 - v) * precip[i][j][h] + u * (1 - v) * precip[i][j + 1][h]
                   + (1 - u) * v * precip[i + 1][j][h] + u * v * precip[i + 1][j + 1][h];
        }
        return out;
    }
}
