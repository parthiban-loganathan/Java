package lab.sql;

/** An error of the embedded database, with MySQL's error code, SQLSTATE and message. */
public final class SqlError extends RuntimeException {
    public static final int GENERAL = 0;
    public static final int SYNTAX = 1;
    public static final int INTEGRITY = 2;

    private static final long serialVersionUID = 1L;
    public final int code;
    public final String state;
    public final int kind;

    public SqlError(String message, int code, String state, int kind) {
        super(message);
        this.code = code;
        this.state = state;
        this.kind = kind;
    }

    public static SqlError syntax(String near) {
        String text = near.length() > 80 ? near.substring(0, 80) : near;
        return new SqlError("You have an error in your SQL syntax; check the manual that corresponds to your MySQL "
                + "server version for the right syntax to use near '" + text + "' at line 1", 1064, "42000", SYNTAX);
    }

    public static SqlError general(String message) {
        return new SqlError(message, 0, "S1000", GENERAL);
    }

    public static SqlError noTable(String schema, String table) {
        return new SqlError("Table '" + schema + "." + table + "' doesn't exist", 1146, "42S02", SYNTAX);
    }

    public static SqlError unknownColumn(String column, String clause) {
        return new SqlError("Unknown column '" + column + "' in '" + clause + "'", 1054, "42S22", SYNTAX);
    }
}
