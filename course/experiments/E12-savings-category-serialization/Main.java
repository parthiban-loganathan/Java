import java.io.*;
import java.util.Scanner;

class SavingsData implements Serializable {
    private static final long serialVersionUID = 1L;
    double salary;
    double savings;

    public SavingsData(double salary, double savings) {
        this.salary = salary;
        this.savings = savings;
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        double salary = sc.nextDouble();
        double savings = sc.nextDouble();

        SavingsData data = new SavingsData(salary, savings);

        // Step 1: Serialize object to savings.ser
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("savings.ser"))) {
            oos.writeObject(data);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        // Step 2: Deserialize object from savings.ser and determine category
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream("savings.ser"))) {
            SavingsData loaded = (SavingsData) ois.readObject();
            if (loaded.salary <= 0 || loaded.savings < 0) {
                System.out.println("Invalid input");
            } else {
                double pct = (loaded.savings / loaded.salary) * 100.0;
                if (pct >= 1.0 && pct < 10.0) {
                    System.out.println("Poor savings");
                } else if (pct >= 10.0 && pct < 20.0) {
                    System.out.println("Good savings");
                } else if (pct >= 20.0) {
                    System.out.println("High savings");
                } else {
                    System.out.println("Invalid input");
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }

        sc.close();
    }
}
