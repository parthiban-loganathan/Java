# Experiment 16: Employee Management System using Java and JDBC

## Problem statement (syllabus)
Develop a menu-driven program (`EmployeeApp.java`) using Java and JDBC that allows users to perform operations on employee records stored in a MySQL database: 1. Add Employee, 2. View Employees, 3. Update Employee Salary, 4. Delete Employee, 5. Exit.

### Concept: What is JDBC (Java Database Connectivity)?

JDBC is the bridge between your Java program and a MySQL database table:
1. `Connection conn = DriverManager.getConnection(URL, USER, PASS);` opens a live connection to the MySQL server.
2. `PreparedStatement ps = conn.prepareStatement("INSERT INTO ... VALUES (?, ?)");` prepares an SQL query with `?` placeholders, fills them safely using `ps.setString(1, name)` and `ps.setDouble(2, salary)`, and runs the change via `ps.executeUpdate()`.
3. `ResultSet rs = st.executeQuery("SELECT * FROM employees");` fetches all rows from the table, and `while (rs.next())` steps through each row to print `id`, `name`, and `salary`.

## Step 1: MySQL database setup
Run these 4 lines inside MySQL Workbench or the MySQL Command Line Client before running the Java program:

```sql
CREATE DATABASE IF NOT EXISTS company_db;
USE company_db;
CREATE TABLE IF NOT EXISTS employees (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    salary DOUBLE NOT NULL
);
```

## Line-by-line notes

```bash
javac EmployeeApp.java
java -cp ".;mysql-connector-j-x.x.x.jar" EmployeeApp
```

## Sample input and expected output

These cases are checked automatically on the JDK by `tools/CourseCheck.java`.

**Case 1**

Input:

```text
1
Asha Raman
55000
1
Vikram Iyer
48000
2
3
1 60000
2
4
2
2
3
9 1000
6
5
```

Output:

```text
1. Add  2. View  3. Update Salary  4. Delete  5. Exit
Employee added.

1. Add  2. View  3. Update Salary  4. Delete  5. Exit
Employee added.

1. Add  2. View  3. Update Salary  4. Delete  5. Exit
ID: 1 | Name: Asha Raman | Salary: 55000.00
ID: 2 | Name: Vikram Iyer | Salary: 48000.00

1. Add  2. View  3. Update Salary  4. Delete  5. Exit
Updated.

1. Add  2. View  3. Update Salary  4. Delete  5. Exit
ID: 1 | Name: Asha Raman | Salary: 60000.00
ID: 2 | Name: Vikram Iyer | Salary: 48000.00

1. Add  2. View  3. Update Salary  4. Delete  5. Exit
Deleted.

1. Add  2. View  3. Update Salary  4. Delete  5. Exit
ID: 1 | Name: Asha Raman | Salary: 60000.00

1. Add  2. View  3. Update Salary  4. Delete  5. Exit
Not found.

1. Add  2. View  3. Update Salary  4. Delete  5. Exit
Invalid choice.

1. Add  2. View  3. Update Salary  4. Delete  5. Exit
```

## Notes

### Running Experiment 16

The program needs a MySQL server and the MySQL Connector/J driver (`mysql-connector-j-<version>.jar`).

* **Classpath separator**: Windows uses `;`, Linux and macOS use `:`.

```bash
javac EmployeeApp.java
java -cp ".:mysql-connector-j-9.1.0.jar" EmployeeApp
```

On Windows (Command Prompt): `java -cp ".;mysql-connector-j-9.1.0.jar" EmployeeApp`

* **Credentials**: this version reads `DB_URL`, `DB_USER` and `DB_PASSWORD` from environment
  variables and falls back to the handout's defaults. Never commit a real password to GitHub.
* **GitHub Codespaces**: the repository's dev container starts a MySQL 8.4 service. `setup.sql` in
  this folder creates the database; the Codespaces guide in `docs/` lists the exact commands.
* The automated test runs this program against a real MySQL service in GitHub Actions with the
  input below.
