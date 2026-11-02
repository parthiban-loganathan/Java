import java.util.Scanner;

// Parent Class
class Employee {
    double baseSalary;

    Employee(double baseSalary) {
        this.baseSalary = baseSalary;
    }
}

// Child Class 1: Developer inherits baseSalary from Employee
class Developer extends Employee {
    double bonusPercentage = 10.0;
    int workHours;

    Developer(double baseSalary, int workHours) {
        super(baseSalary); // Pass baseSalary to parent Employee constructor
        this.workHours = workHours;
    }

    void displayFinalIncome() {
        double income = baseSalary + (baseSalary * bonusPercentage * workHours / 100.0);
        System.out.printf("Developer Final Income: %.2f%n", income);
    }
}

// Child Class 2: Designer inherits baseSalary from Employee
class Designer extends Employee {
    double bonusPercentage = 5.0;
    int workHours;

    Designer(double baseSalary, int workHours) {
        super(baseSalary); // Pass baseSalary to parent Employee constructor
        this.workHours = workHours;
    }

    void displayFinalIncome() {
        double income = baseSalary + (baseSalary * bonusPercentage * workHours / 100.0);
        System.out.printf("Designer Final Income: %.2f%n", income);
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        double devSalary = sc.nextDouble();
        int devHours = sc.nextInt();
        Developer dev = new Developer(devSalary, devHours);

        double desSalary = sc.nextDouble();
        int desHours = sc.nextInt();
        Designer des = new Designer(desSalary, desHours);

        dev.displayFinalIncome();
        des.displayFinalIncome();

        sc.close();
    }
}
