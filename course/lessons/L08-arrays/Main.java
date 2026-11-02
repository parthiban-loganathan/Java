// L08: 1-D arrays (hourly rainfall) and 2-D arrays (an elevation grid).
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        double[] rain = {2.0, 8.5, 22.0, 31.5, 12.0, 4.0};   // mm in each hour

        System.out.println("rain = " + Arrays.toString(rain));
        System.out.println("length = " + rain.length + ", first = " + rain[0] + ", last = " + rain[rain.length - 1]);

        double total = 0, max = rain[0];
        int maxIndex = 0;
        for (int i = 0; i < rain.length; i++) {
            total += rain[i];
            if (rain[i] > max) {
                max = rain[i];
                maxIndex = i;
            }
        }
        System.out.printf("total = %.1f mm, mean = %.2f mm/h, peak = %.1f mm in hour %d%n",
                total, total / rain.length, max, maxIndex + 1);

        // enhanced for: count wet hours (more than 5 mm)
        int wetHours = 0;
        for (double r : rain) {
            if (r > 5.0) wetHours++;
        }
        System.out.println("hours above 5 mm = " + wetHours);

        // copy, sort, and the reference trap
        double[] sorted = Arrays.copyOf(rain, rain.length);
        Arrays.sort(sorted);
        System.out.println("sorted copy = " + Arrays.toString(sorted));
        double[] alias = rain;          // NOT a copy: same array
        alias[0] = 99.0;
        System.out.println("after alias[0] = 99: rain[0] = " + rain[0]);

        // 2-D array: elevations (m) of a 3 x 3 patch, 30 m cells
        double[][] z = {
            {112.0, 108.5, 104.0},
            {109.0, 105.0, 101.5},
            {106.5, 102.0,  99.0}
        };
        double cell = 30.0;
        int[] dr = {-1, -1, 0, 1, 1, 1, 0, -1};
        int[] dc = {0, 1, 1, 1, 0, -1, -1, -1};
        String[] names = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int r = 1, c = 1;
        double bestSlope = 0;
        String bestName = "none (pit)";
        for (int k = 0; k < 8; k++) {
            double distance = (dr[k] != 0 && dc[k] != 0) ? cell * Math.sqrt(2) : cell;
            double slope = (z[r][c] - z[r + dr[k]][c + dc[k]]) / distance;
            if (slope > bestSlope) {
                bestSlope = slope;
                bestName = names[k];
            }
        }
        System.out.println("grid is " + z.length + " x " + z[0].length);
        System.out.printf("centre %.1f m drains %s with slope %.4f%n", z[r][c], bestName, bestSlope);
    }
}
