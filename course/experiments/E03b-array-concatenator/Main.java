import java.util.Scanner;

class ArrayConcatenator {
    int[] mergedArray;

    // Constructor joins arr1 and arr2 into mergedArray
    ArrayConcatenator(int[] arr1, int[] arr2) {
        mergedArray = new int[arr1.length + arr2.length];
        int pos = 0;

        // Copy elements of first array
        for (int i = 0; i < arr1.length; i++) {
            mergedArray[pos] = arr1[i];
            pos++;
        }

        // Copy elements of second array
        for (int i = 0; i < arr2.length; i++) {
            mergedArray[pos] = arr2[i];
            pos++;
        }
    }

    void displayResult() {
        for (int i = 0; i < mergedArray.length; i++) {
            System.out.print(mergedArray[i] + (i == mergedArray.length - 1 ? "" : " "));
        }
        System.out.println();
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Read first array size N and its N elements
        int n = sc.nextInt();
        int[] arr1 = new int[n];
        for (int i = 0; i < n; i++) {
            arr1[i] = sc.nextInt();
        }

        // Read second array size M and its M elements
        int m = sc.nextInt();
        int[] arr2 = new int[m];
        for (int i = 0; i < m; i++) {
            arr2[i] = sc.nextInt();
        }

        // Create ArrayConcatenator object and display merged array
        ArrayConcatenator obj = new ArrayConcatenator(arr1, arr2);
        obj.displayResult();

        sc.close();
    }
}
