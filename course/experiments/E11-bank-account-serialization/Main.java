import java.io.*;
import java.util.Scanner;

// Serializable allows BankAccount objects to be saved directly to a file
class BankAccount implements Serializable {
    private static final long serialVersionUID = 1L;
    private double balance;

    public BankAccount(double initialBalance) {
        this.balance = initialBalance;
    }

    public void deposit(double amount) {
        if (amount > 0) balance += amount;
    }

    public void withdraw(double amount) {
        if (amount > 0 && amount <= balance) balance -= amount;
    }

    public double getBalance() {
        return balance;
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        double initialBalance = sc.nextDouble();
        double depositAmount = sc.nextDouble();
        double withdrawAmount = sc.nextDouble();

        BankAccount account = new BankAccount(initialBalance);
        account.deposit(depositAmount);
        account.withdraw(withdrawAmount);

        // Step 1: Serialize (Save) BankAccount object to bankAccount.ser
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("bankAccount.ser"))) {
            oos.writeObject(account);
        } catch (IOException e) {
            System.out.println("Serialization Error: " + e.getMessage());
        }

        // Step 2: Deserialize (Load) BankAccount object from bankAccount.ser
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream("bankAccount.ser"))) {
            BankAccount loaded = (BankAccount) ois.readObject();
            System.out.printf("Final Balance: %.2f%n", loaded.getBalance());
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Deserialization Error: " + e.getMessage());
        }

        sc.close();
    }
}
