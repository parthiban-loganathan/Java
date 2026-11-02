import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Step 1: Read the start and end of the range
        int start = sc.nextInt();
        int end = sc.nextInt();

        // Step 2: Outer loop checks every number 'i' from start to end
        for (int i = start; i <= end; i++) {
            if (i >= 2) { // Numbers less than 2 are never prime
                boolean isPrime = true;

                // Step 3: Try dividing 'i' by every number 'j' from 2 up to sqrt(i)
                for (int j = 2; j * j <= i; j++) {
                    if (i % j == 0) {
                        isPrime = false; // Divisor found! Not a prime number
                        break;           // Stop testing further divisors
                    }
                }

                // Step 4: If no divisor was found, print the prime number
                if (isPrime) {
                    System.out.print(i + " ");
                }
            }
        }
        System.out.println();
        sc.close();
    }
}
