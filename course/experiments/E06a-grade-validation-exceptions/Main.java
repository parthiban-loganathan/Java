import java.util.Scanner;

public class Main {
    public static void validateGrade(int grade) {
        if (grade < 0 || grade > 100) {
            throw new IllegalArgumentException("Grade must be between 0 and 100.");
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        String name = sc.nextLine();
        String gradeInput = sc.nextLine();

        try {
            int grade = Integer.parseInt(gradeInput);
            validateGrade(grade);
            System.out.println("Student: " + name + ", Grade: " + grade);
        } catch (NumberFormatException e) {
            // Non-integer text (thrown by Integer.parseInt). Must come first:
            // NumberFormatException is a subclass of IllegalArgumentException.
            System.out.println(e.toString());
        } catch (IllegalArgumentException e) {
            // Out-of-range grade (thrown by validateGrade)
            System.out.println(e.toString());
        }

        sc.close();
    }
}
