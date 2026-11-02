import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ArrayList<String> students = new ArrayList<>();

        int n = Integer.parseInt(sc.nextLine());
        for (int i = 0; i < n; i++) {
            students.add(sc.nextLine());
        }

        int index = sc.nextInt();
        if (index >= 0 && index < students.size()) {
            System.out.println("Student at index " + index + ": " + students.get(index));
        } else {
            System.out.println("Invalid index");
        }

        sc.close();
    }
}
