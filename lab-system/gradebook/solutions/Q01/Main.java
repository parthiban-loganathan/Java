import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        double c = in.nextDouble();
        double i = in.nextDouble();
        double a = in.nextDouble();
        if (c < 0 || c > 1) {
            System.out.println("Invalid runoff coefficient");
            return;
        }
        double q = c * i * a / 360.0;
        String size = q < 1 ? "small" : (q < 5 ? "medium" : "large");
        System.out.printf("Runoff coefficient C = %.2f%n", c);
        System.out.printf("Peak runoff Q = %.3f m3/s%n", q);
        System.out.println("Culvert class: " + size);
    }
}
