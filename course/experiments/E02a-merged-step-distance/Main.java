import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Step 1: Read step counts for Day 1 and Day 2
        int day1Steps = sc.nextInt();
        int day2Steps = sc.nextInt();

        // Step 2: Merge both daily step counts into an integer array
        int[] mergedSteps = {day1Steps, day2Steps};

        // Step 3: Sum the merged step counts (1 step = 1 unit distance)
        int totalDistance = mergedSteps[0] + mergedSteps[1];

        System.out.println("Total Distance Covered: " + totalDistance);

        sc.close();
    }
}
