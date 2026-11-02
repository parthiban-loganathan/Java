package org.teavm.classlib.java.sql;

/** java.sql.SQLNonTransientException for programs run in the browser. */
public class TSQLNonTransientException extends TSQLException {
    private static final long serialVersionUID = 1L;

    public TSQLNonTransientException() {
        super();
    }

    public TSQLNonTransientException(String reason) {
        super(reason);
    }

    public TSQLNonTransientException(String reason, String sqlState) {
        super(reason, sqlState);
    }

    public TSQLNonTransientException(String reason, String sqlState, int vendorCode) {
        super(reason, sqlState, vendorCode);
    }

    public TSQLNonTransientException(Throwable cause) {
        super(cause);
    }

    public TSQLNonTransientException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
