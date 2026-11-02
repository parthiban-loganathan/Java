// L13: try / catch / finally, throwing exceptions, and the order of catch blocks.
import java.util.Scanner;

public class Main {

    static double validated(double rainMm) {
        if (rainMm < 0) {
            throw new IllegalArgumentException("rainfall cannot be negative: " + rainMm);
        }
        return rainMm;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        double total = 0;
        int good = 0, bad = 0;
        while (sc.hasNextLine()) {
            String line = sc.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                double value = validated(Double.parseDouble(line));
                total += value;
                good++;
            } catch (NumberFormatException e) {            // subclass first
                System.out.println("skipped \"" + line + "\": not a number");
                bad++;
            } catch (IllegalArgumentException e) {         // then the parent class
                System.out.println("skipped \"" + line + "\": " + e.getMessage());
                bad++;
            } finally {
                // runs after every line, whether or not an exception happened
            }
        }
        System.out.printf("valid values: %d, rejected: %d, total rain: %.1f mm%n", good, bad, total);

        int[] gauges = new int[3];
        try {
            gauges[3] = 5;                                 // valid indices are 0, 1, 2
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("caught " + e.getClass().getSimpleName());
        }
        System.out.println("5.0 / 0 = " + (5.0 / 0));
        try {
            System.out.println(5 / (good - good));
        } catch (ArithmeticException e) {
            System.out.println("integer division by zero -> ArithmeticException");
        }
        sc.close();
    }
}
