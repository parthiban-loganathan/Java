package org.teavm.classlib.java.sql;


/**
 * java.sql.DriverManager for programs run in the browser: every jdbc: URL (jdbc:mysql://host/db,
 * jdbc:sqlite:..., ...) connects to the page's embedded database (lab.sql), which speaks the
 * MySQL dialect used in the course. User name and password are accepted and not checked.
 */
public class TDriverManager {
    private static int loginTimeout;

    private TDriverManager() {
    }

    public static TConnection getConnection(String url) throws TSQLException {
        return TLabConnection.open(url);
    }

    public static TConnection getConnection(String url, String user, String password) throws TSQLException {
        return TLabConnection.open(url);
    }

    public static TConnection getConnection(String url, java.util.Properties info) throws TSQLException {
        return TLabConnection.open(url);
    }

    public static void setLoginTimeout(int seconds) {
        loginTimeout = seconds;
    }

    public static int getLoginTimeout() {
        return loginTimeout;
    }
}
