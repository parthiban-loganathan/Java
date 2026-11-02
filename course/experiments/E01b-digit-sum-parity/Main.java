import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Step 1: Read the 3-digit integer (Math.abs handles negative sign if any)
        int num = Math.abs(sc.nextInt());

        // Step 2: Extract each digit using / and %
        int units = num % 10;
        int tens = (num / 10) % 10;
        int hundreds = (num / 100) % 10;

        // Step 3: Add the three digits together
        int sum = hundreds + tens + units;
        System.out.println("Sum of digits: " + sum);

        // Step 4: Check if sum is divisible by 2 (Even) or not (Odd)
        if (sum % 2 == 0) {
            System.out.println("Parity: Even");
        } else {
            System.out.println("Parity: Odd");
        }

        sc.close();
    }
}
