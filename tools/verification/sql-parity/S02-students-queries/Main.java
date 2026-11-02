import java.sql.*;

/** More JDBC behaviour checked against real MySQL 8.4 (CI) and the browser's embedded database. */
public class Main {
    static void q(Connection c, String sql) {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            int n = rs.getMetaData().getColumnCount();
            System.out.println("> " + sql);
            while (rs.next()) {
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= n; i++) {
                    row.append(i > 1 ? " | " : "").append(rs.getString(i));
                }
                System.out.println(row);
            }
        } catch (SQLException e) {
            System.out.println("> " + sql + "\nERROR " + e.getErrorCode() + ": " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        String url = System.getenv("DB_URL") == null ? "jdbc:mysql://localhost:3306/lab_db" : System.getenv("DB_URL");
        try (Connection c = DriverManager.getConnection(url, "root", "password")) {
            try (Statement st = c.createStatement()) {
                st.executeUpdate("DROP TABLE IF EXISTS student");
                st.executeUpdate("CREATE TABLE student (regno VARCHAR(10) PRIMARY KEY, name VARCHAR(40) NOT NULL, "
                        + "branch CHAR(3), marks INT, cgpa DECIMAL(4,2), fee DOUBLE)");
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO student VALUES (?, ?, ?, ?, ?, ?)")) {
                Object[][] rows = {{"24BCL0001", "Anitha", "BCL", 78, 8.45, 125000.0}, {"24BCL0002", "Bala", "BCL", 91, 9.12, 125000.0},
                    {"24BCE0003", "Chitra", "BCE", 66, 7.05, 98000.5}, {"24BCE0004", "Dinesh", "BCE", 85, 8.8, 98000.5},
                    {"24BME0005", "Ezhil", "BME", 59, 6.3, 87000.0}};
                for (Object[] r : rows) {
                    ps.setString(1, (String) r[0]);
                    ps.setString(2, (String) r[1]);
                    ps.setString(3, (String) r[2]);
                    ps.setInt(4, (Integer) r[3]);
                    ps.setDouble(5, (Double) r[4]);
                    ps.setDouble(6, (Double) r[5]);
                    ps.addBatch();
                }
                int[] counts = ps.executeBatch();
                System.out.println("batch " + counts.length + " " + counts[0]);
            }
            q(c, "SELECT branch, COUNT(*), AVG(marks), MAX(cgpa), SUM(fee) FROM student GROUP BY branch HAVING COUNT(*) > 1 ORDER BY branch");
            q(c, "SELECT AVG(cgpa), SUM(cgpa), MIN(marks) FROM student");
            q(c, "SELECT DISTINCT branch FROM student ORDER BY branch DESC");
            q(c, "SELECT name, marks FROM student WHERE marks >= 70 AND branch <> 'BME' ORDER BY marks DESC, name LIMIT 3");
            q(c, "SELECT name FROM student WHERE name LIKE 'b%' OR name LIKE '%h_l'");
            q(c, "SELECT regno, marks / 10, marks DIV 10, marks % 10 FROM student WHERE regno = '24BCL0001'");
            q(c, "SELECT UPPER(name), LENGTH(name), CONCAT(name, '-', branch), ROUND(cgpa, 1) FROM student WHERE marks < 70 ORDER BY name");
            q(c, "SELECT COUNT(*) FROM student WHERE cgpa BETWEEN 7 AND 9");
            q(c, "SELECT name FROM student WHERE branch IN ('BCE', 'BME') AND NOT marks > 80 ORDER BY regno");
            c.setAutoCommit(false);
            try (Statement st = c.createStatement()) {
                System.out.println("updated " + st.executeUpdate("UPDATE student SET fee = fee * 1.1 WHERE branch = 'BCL'"));
                System.out.println("updated " + st.executeUpdate("UPDATE student SET marks = marks + 5 WHERE marks < 60"));
            }
            c.commit();
            c.setAutoCommit(true);
            q(c, "SELECT regno, marks, fee FROM student ORDER BY regno");
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT marks, cgpa, fee FROM student WHERE regno = '24BCE0004'")) {
                rs.next();
                System.out.println(rs.getInt(1) + " " + rs.getDouble(2) + " " + rs.getLong(3) + " " + rs.getString(2) + " "
                        + rs.getBigDecimal(2) + " " + rs.getObject(2).getClass().getSimpleName());
            }
            try (Statement st = c.createStatement()) {
                st.executeUpdate("INSERT INTO student (regno, name, marks) VALUES ('24BCL0001', 'Dup', 1)");
            } catch (SQLIntegrityConstraintViolationException e) {
                System.out.println("integrity: " + e.getMessage());
            }
            try (Statement st = c.createStatement()) {
                st.executeQuery("SELEC name FROM student");
            } catch (SQLSyntaxErrorException e) {
                System.out.println("syntax error " + e.getErrorCode());
            }
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM student WHERE regno = 'none'")) {
                System.out.println("found: " + rs.next());
            }
        }
    }
}
