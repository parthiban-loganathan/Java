package org.teavm.classlib.java.sql;

/** java.sql.SQLException for programs run in the browser (TeaVM has no java.sql). */
public class TSQLException extends Exception {
    private static final long serialVersionUID = 1L;
    private final String sqlState;
    private final int vendorCode;
    private TSQLException next;

    public TSQLException() {
        this(null, null, 0);
    }

    public TSQLException(String reason) {
        this(reason, null, 0);
    }

    public TSQLException(String reason, String sqlState) {
        this(reason, sqlState, 0);
    }

    public TSQLException(String reason, String sqlState, int vendorCode) {
        super(reason);
        this.sqlState = sqlState;
        this.vendorCode = vendorCode;
    }

    public TSQLException(Throwable cause) {
        super(cause);
        this.sqlState = null;
        this.vendorCode = 0;
    }

    public TSQLException(String reason, Throwable cause) {
        super(reason, cause);
        this.sqlState = null;
        this.vendorCode = 0;
    }

    public TSQLException(String reason, String sqlState, Throwable cause) {
        super(reason, cause);
        this.sqlState = sqlState;
        this.vendorCode = 0;
    }

    public TSQLException(String reason, String sqlState, int vendorCode, Throwable cause) {
        super(reason, cause);
        this.sqlState = sqlState;
        this.vendorCode = vendorCode;
    }

    public String getSQLState() {
        return sqlState;
    }

    public int getErrorCode() {
        return vendorCode;
    }

    public TSQLException getNextException() {
        return next;
    }

    public void setNextException(TSQLException ex) {
        if (next == null) {
            next = ex;
        } else {
            next.setNextException(ex);
        }
    }
}
