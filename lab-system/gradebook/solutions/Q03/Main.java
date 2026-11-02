import java.util.Scanner;

public class Main {
    static double averageIncrease(long[] population) {
        double total = 0;
        for (int j = 1; j < population.length; j++) {
            total += population[j] - population[j - 1];
        }
        return total / (population.length - 1);
    }

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int k = in.nextInt();
        long[] population = new long[k];
        for (int j = 0; j < k; j++) {
            population[j] = in.nextLong();
        }
        int d = in.nextInt();
        double q = in.nextDouble();
        double x = averageIncrease(population);
        System.out.println("Average increase per decade: " + Math.round(x));
        for (int m = 1; m <= d; m++) {
            long p = Math.round(population[k - 1] + m * x);
            System.out.printf("Year +%d: population %d, demand %.2f MLD%n", 10 * m, p, p * q / 1_000_000.0);
        }
    }
}
