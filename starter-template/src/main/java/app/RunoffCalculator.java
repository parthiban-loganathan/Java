package app;

import java.util.ArrayList;
import java.util.List;

/**
 * The calculation module: reads the input data (CSV text) and computes the results.
 * It has no input/output of its own, so it is used unchanged by the web page (WebApi),
 * by the console program (ConsoleApp) and by the tests.
 *
 * CHANGE THIS: put the engineering calculation of your project here.
 */
public class RunoffCalculator {

    /** Reads hourly rainfall: last column of every data row, in mm. */
    public static double[] readRainfall(String csv) {
        List<Double> values = new ArrayList<>();
        for (String raw : csv.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split(",");
            String last = parts[parts.length - 1].trim();
            try {
                double v = Double.parseDouble(last);
                if (v < 0) {
                    throw new IllegalArgumentException("negative rainfall in line: " + line);
                }
                values.add(v);
            } catch (NumberFormatException e) {
                if (!values.isEmpty()) {
                    throw new IllegalArgumentException("not a number in line: " + line);
                }
                // the first non-numeric line is the header
            }
        }
        if (values.isEmpty()) {
            throw new IllegalArgumentException("no rainfall values found");
        }
        double[] result = new double[values.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    /**
     * Reads sub-catchments. Required columns (by header name): name, area_km2, curve_number,
     * and either centroid_lat/centroid_lon or lat/lon.
     */
    public static List<SubCatchment> readSubCatchments(String csv) {
        List<SubCatchment> list = new ArrayList<>();
        String[] header = null;
        for (String raw : csv.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] cells = line.split(",", -1);
            if (header == null) {
                header = cells;
                continue;
            }
            String name = cells[column(header, "name")].trim();
            double lat = Double.parseDouble(cells[column(header, "centroid_lat", "lat")].trim());
            double lon = Double.parseDouble(cells[column(header, "centroid_lon", "lon")].trim());
            double area = Double.parseDouble(cells[column(header, "area_km2")].trim());
            double cn = Double.parseDouble(cells[column(header, "curve_number")].trim());
            list.add(new SubCatchment(name, lat, lon, area, cn));
        }
        if (list.isEmpty()) {
            throw new IllegalArgumentException("no sub-catchments found");
        }
        return list;
    }

    private static int column(String[] header, String... names) {
        for (String n : names) {
            for (int i = 0; i < header.length; i++) {
                if (header[i].trim().equalsIgnoreCase(n)) {
                    return i;
                }
            }
        }
        throw new IllegalArgumentException("missing column: " + names[0]);
    }

    /** Runs the calculation and returns the results as JSON text for the web page. */
    public static String calculateJson(String subCatchmentCsv, String rainfallCsv) {
        List<SubCatchment> subs = readSubCatchments(subCatchmentCsv);
        double[] rain = readRainfall(rainfallCsv);
        double totalRain = 0;
        for (double r : rain) {
            totalRain += r;
        }
        double[] hourlyVolume = new double[rain.length];
        double totalArea = 0, totalVolume = 0;

        StringBuilder items = new StringBuilder();
        for (int k = 0; k < subs.size(); k++) {
            SubCatchment s = subs.get(k);
            double[] q = s.hourlyRunoffMm(rain);
            double depth = 0;
            for (int h = 0; h < q.length; h++) {
                depth += q[h];
                hourlyVolume[h] += s.volumeM3(q[h]);
            }
            double volume = s.volumeM3(depth);
            totalArea += s.getAreaKm2();
            totalVolume += volume;
            if (k > 0) items.append(',');
            items.append('{')
                 .append("\"name\":\"").append(s.getName().replace("\"", "'")).append("\",")
                 .append("\"lat\":").append(s.getLatitude()).append(',')
                 .append("\"lon\":").append(s.getLongitude()).append(',')
                 .append("\"areaKm2\":").append(s.getAreaKm2()).append(',')
                 .append("\"curveNumber\":").append(s.getCurveNumber()).append(',')
                 .append("\"retentionMm\":").append(round(s.retentionMm())).append(',')
                 .append("\"runoffMm\":").append(round(depth)).append(',')
                 .append("\"runoffCoefficient\":").append(round(totalRain > 0 ? depth / totalRain : 0)).append(',')
                 .append("\"volumeM3\":").append(round(volume))
                 .append('}');
        }
        StringBuilder json = new StringBuilder("{");
        json.append("\"hours\":").append(rain.length).append(',');
        json.append("\"totalRainMm\":").append(round(totalRain)).append(',');
        json.append("\"totalAreaKm2\":").append(round(totalArea)).append(',');
        json.append("\"totalVolumeM3\":").append(round(totalVolume)).append(',');
        json.append("\"averageRunoffMm\":").append(round(totalVolume / (totalArea * 1000.0))).append(',');
        json.append("\"rain\":").append(array(rain)).append(',');
        json.append("\"hourlyVolumeM3\":").append(array(hourlyVolume)).append(',');
        json.append("\"subCatchments\":[").append(items).append("]}");
        return json.toString();
    }

    static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private static String array(double[] a) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < a.length; i++) {
            if (i > 0) b.append(',');
            b.append(round(a[i]));
        }
        return b.append(']').toString();
    }
}
