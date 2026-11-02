import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Step 1: Read Principal and Rate for Financing Option 1
        double principal1 = sc.nextDouble();
        double rate1 = sc.nextDouble();

        // Step 2: Read Principal and Rate for Financing Option 2
        double principal2 = sc.nextDouble();
        double rate2 = sc.nextDouble();

        // Step 3: Calculate Interest = (Principal * Rate) / 100.0
        double interest1 = (principal1 * rate1) / 100.0;
        double interest2 = (principal2 * rate2) / 100.0;

        // Step 4: Print both calculated interests to 2 decimal places
        System.out.printf("Interest for Option 1: %.2f%n", interest1);
        System.out.printf("Interest for Option 2: %.2f%n", interest2);

        sc.close();
    }
}
