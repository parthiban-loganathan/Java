import java.util.Scanner;

public class Main {
    // Helper method: returns true if 'n' is prime, false otherwise
    public static boolean isPrime(int n) {
        if (n < 2) return false;
        for (int i = 2; i * i <= n; i++) {
            if (n % i == 0) return false;
        }
        return true;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Step 1: Read number of rows and columns
        int rows = sc.nextInt();
        int cols = sc.nextInt();

        // Step 2: Create the 2D grid and fill it with input numbers
        int[][] grid = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = sc.nextInt();
            }
        }

        // Step 3: Check every cell (i, j) and print coordinates if prime
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (isPrime(grid[i][j])) {
                    System.out.println(grid[i][j] + " found at (" + i + ", " + j + ")");
                }
            }
        }

        sc.close();
    }
}
