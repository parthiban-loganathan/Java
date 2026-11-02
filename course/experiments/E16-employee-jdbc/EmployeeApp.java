import java.sql.*;
import java.util.Scanner;

public class EmployeeApp {
    // Connection settings are read from environment variables when they are set,
    // so passwords never have to be written into source code that is pushed to GitHub.
    private static final String URL = env("DB_URL", "jdbc:mysql://localhost:3306/company_db");
    private static final String USER = env("DB_USER", "root");
    private static final String PASS = env("DB_PASSWORD", "password"); // default for a local lab PC

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isEmpty()) ? fallback : value;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            while (true) {
                System.out.println("\n1. Add  2. View  3. Update Salary  4. Delete  5. Exit");
                int choice = sc.nextInt();

                switch (choice) {
                    case 1:
                        sc.nextLine(); // Clear newline after nextInt()
                        String name = sc.nextLine();
                        double salary = sc.nextDouble();
                        String ins = "INSERT INTO employees (name, salary) VALUES (?, ?)";
                        try (PreparedStatement ps = conn.prepareStatement(ins)) {
                            ps.setString(1, name);
                            ps.setDouble(2, salary);
                            ps.executeUpdate();
                            System.out.println("Employee added.");
                        }
                        break;

                    case 2:
                        String sel = "SELECT * FROM employees";
                        try (Statement st = conn.createStatement();
                             ResultSet rs = st.executeQuery(sel)) {
                            while (rs.next()) {
                                System.out.printf("ID: %d | Name: %s | Salary: %.2f%n",
                                        rs.getInt("id"),
                                        rs.getString("name"),
                                        rs.getDouble("salary"));
                            }
                        }
                        break;

                    case 3:
                        int upId = sc.nextInt();
                        double newSal = sc.nextDouble();
                        String upd = "UPDATE employees SET salary = ? WHERE id = ?";
                        try (PreparedStatement ps = conn.prepareStatement(upd)) {
                            ps.setDouble(1, newSal);
                            ps.setInt(2, upId);
                            int rows = ps.executeUpdate();
                            System.out.println(rows > 0 ? "Updated." : "Not found.");
                        }
                        break;

                    case 4:
                        int delId = sc.nextInt();
                        String del = "DELETE FROM employees WHERE id = ?";
                        try (PreparedStatement ps = conn.prepareStatement(del)) {
                            ps.setInt(1, delId);
                            int rows = ps.executeUpdate();
                            System.out.println(rows > 0 ? "Deleted." : "Not found.");
                        }
                        break;

                    case 5:
                        sc.close();
                        return;

                    default:
                        System.out.println("Invalid choice.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database Error: " + e.getMessage());
        }
    }
}
