import java.util.*;

public class Main {
    public static double findMedian(List<Double> temps) {
        Collections.sort(temps);
        int n = temps.size();
        if (n % 2 != 0) {
            return temps.get(n / 2);
        } else {
            return (temps.get((n / 2) - 1) + temps.get(n / 2)) / 2.0;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        HashMap<String, List<Double>> cityData = new LinkedHashMap<>();

        int cities = sc.nextInt();
        for (int i = 0; i < cities; i++) {
            String city = sc.next();
            int count = sc.nextInt();
            List<Double> temps = new ArrayList<>();
            for (int j = 0; j < count; j++) {
                temps.add(sc.nextDouble());
            }
            cityData.put(city, temps);
        }

        for (Map.Entry<String, List<Double>> entry : cityData.entrySet()) {
            double median = findMedian(entry.getValue());
            System.out.printf("City: %s, Median Temperature: %.2f%n",
                    entry.getKey(), median);
        }

        sc.close();
    }
}
