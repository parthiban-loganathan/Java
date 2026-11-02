import java.util.Scanner;

interface Motor {
    void run();
    double consume(double capacity);
}

class WashingMachine implements Motor {
    @Override
    public void run() {
        System.out.println("Washing machine is running.");
    }

    public void consume() {
        System.out.println("Washing machine is consuming electricity.");
    }

    @Override
    public double consume(double capacity) {
        return capacity * 0.05;
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        double capacity = sc.nextDouble();

        WashingMachine wm = new WashingMachine();
        wm.run();
        wm.consume();
        System.out.printf("Electricity consumption: %.2f kWh%n", wm.consume(capacity));

        sc.close();
    }
}
