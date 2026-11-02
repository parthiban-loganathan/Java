import java.sql.*;

/** JDBC behaviour checked against real MySQL 8.4 (CI) and the browser's embedded database. */
public class Main {
    static void show(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        StringBuilder head = new StringBuilder();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            head.append(i > 1 ? " | " : "").append(md.getColumnLabel(i));
        }
        System.out.println(head);
        while (rs.next()) {
            StringBuilder row = new StringBuilder();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                row.append(i > 1 ? " | " : "").append(rs.getString(i));
            }
            System.out.println(row);
        }
    }

    static void query(Connection c, String sql) {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            System.out.println("> " + sql);
            show(rs);
        } catch (SQLException e) {
            System.out.println("> " + sql + "\nERROR " + e.getErrorCode() + " " + e.getSQLState() + ": " + e.getMessage());
        }
    }

    static void update(Connection c, String sql) {
        try (Statement st = c.createStatement()) {
            System.out.println("> " + sql + "\nrows " + st.executeUpdate(sql));
        } catch (SQLException e) {
            System.out.println("> " + sql + "\nERROR " + e.getErrorCode() + " " + e.getSQLState() + ": " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        String url = System.getenv("DB_URL") == null ? "jdbc:mysql://localhost:3306/lab_db" : System.getenv("DB_URL");
        String pass = System.getenv("DB_PASSWORD") == null ? "password" : System.getenv("DB_PASSWORD");
        try (Connection c = DriverManager.getConnection(url, "root", pass)) {
            update(c, "DROP TABLE IF EXISTS gauge");
            update(c, "CREATE TABLE gauge (id INT PRIMARY KEY AUTO_INCREMENT, station VARCHAR(30) NOT NULL UNIQUE, "
                    + "district VARCHAR(30), rain_mm DOUBLE, area DECIMAL(8,2), active BOOLEAN DEFAULT TRUE)");
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO gauge (station, district, rain_mm, area) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                Object[][] data = {{"Katpadi", "Vellore", 41.5, 12.345}, {"Gudiyatham", "Vellore", 12.0, 8.1},
                    {"Arni", "Tiruvannamalai", 88.25, 30.0}, {"Polur", "Tiruvannamalai", null, 5.555}, {"Ambur", "Tirupattur", 0.0, 7.0}};
                for (Object[] d : data) {
                    ps.setString(1, (String) d[0]);
                    ps.setString(2, (String) d[1]);
                    if (d[2] == null) {
                        ps.setNull(3, Types.DOUBLE);
                    } else {
                        ps.setDouble(3, (Double) d[2]);
                    }
                    ps.setDouble(4, (Double) d[3]);
                    int n = ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) {
                        k.next();
                        System.out.println("inserted " + n + " key " + k.getLong(1));
                    }
                }
            }
            query(c, "SELECT * FROM gauge");
            query(c, "SELECT station, rain_mm FROM gauge WHERE rain_mm > 10 ORDER BY rain_mm DESC");
            query(c, "SELECT district, COUNT(*) AS n, SUM(rain_mm) AS total, MAX(rain_mm) FROM gauge GROUP BY district ORDER BY district");
            query(c, "SELECT COUNT(*), COUNT(rain_mm), MIN(station) FROM gauge");
            query(c, "SELECT station FROM gauge WHERE station LIKE '%a%' AND district IN ('Vellore', 'Tirupattur') ORDER BY station");
            query(c, "SELECT station, rain_mm FROM gauge WHERE rain_mm IS NULL OR rain_mm BETWEEN 0 AND 12 ORDER BY id");
            query(c, "SELECT station FROM gauge ORDER BY station LIMIT 2 OFFSET 1");
            query(c, "SELECT station, area * 2 AS twice, rain_mm / 2 FROM gauge WHERE id <= 2");
            query(c, "SELECT nosuch FROM gauge");
            query(c, "SELECT * FROM missing_table");
            update(c, "UPDATE gauge SET rain_mm = rain_mm + 1 WHERE district = 'Vellore'");
            update(c, "UPDATE gauge SET district = 'Vellore' WHERE id = 1");
            update(c, "DELETE FROM gauge WHERE rain_mm IS NULL");
            update(c, "INSERT INTO gauge (station) VALUES ('Katpadi')");
            update(c, "INSERT INTO gauge (district) VALUES ('X')");
            update(c, "INSERT INTO gauge (station, rain_mm) VALUES ('Vaniyambadi', 3), ('Jolarpet', 4.5)");
            c.setAutoCommit(false);
            update(c, "DELETE FROM gauge");
            c.rollback();
            c.setAutoCommit(true);
            query(c, "SELECT id, station, district, rain_mm, area, active FROM gauge ORDER BY id");
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT id, rain_mm, area, active FROM gauge WHERE id = 1")) {
                rs.next();
                System.out.println(rs.getObject("id").getClass().getSimpleName() + " " + rs.getObject("rain_mm").getClass().getSimpleName()
                        + " " + rs.getObject("active") + " " + rs.getInt("rain_mm") + " " + rs.getDouble("area"));
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM gauge WHERE id = ?")) {
                ps.setInt(2, 1);
            } catch (SQLException e) {
                System.out.println("ERROR " + e.getMessage());
            }
        }
    }
}
