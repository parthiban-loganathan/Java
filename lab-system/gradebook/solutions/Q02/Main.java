import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int fck = in.nextInt();
        int n = in.nextInt();
        double[] strength = new double[n];
        double sum = 0;
        int lowest = 0;
        for (int k = 0; k < n; k++) {
            strength[k] = in.nextDouble() * 1000.0 / (150 * 150);
            sum += strength[k];
            if (strength[k] < strength[lowest]) {
                lowest = k;
            }
            System.out.printf("Cube %d: %.2f N/mm2%n", k + 1, strength[k]);
        }
        double mean = sum / n;
        boolean ok = mean >= fck + 4 && strength[lowest] >= fck - 4;
        System.out.printf("Mean strength: %.2f N/mm2%n", mean);
        System.out.println("Lowest cube: " + (lowest + 1));
        System.out.println("Result: " + (ok ? "ACCEPTED" : "REJECTED"));
    }
}
