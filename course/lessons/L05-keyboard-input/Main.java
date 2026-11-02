// L05: reading numbers, words and whole lines from the keyboard.
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("Enter span (m) and load (kN/m):");
        double span = sc.nextDouble();
        double load = sc.nextDouble();

        System.out.println("Enter number of bars and soil group letter:");
        int bars = sc.nextInt();
        char soilGroup = sc.next().charAt(0);

        sc.nextLine();                        // discard the rest of the current line (the trap!)
        System.out.println("Enter site name (may contain spaces):");
        String site = sc.nextLine();

        double moment = load * span * span / 8;
        System.out.printf("Site: %s%n", site);
        System.out.printf("Span %.2f m, load %.2f kN/m -> M = %.2f kN m%n", span, load, moment);
        System.out.println("Bars: " + bars + ", hydrologic soil group: " + soilGroup);
        sc.close();
    }
}
