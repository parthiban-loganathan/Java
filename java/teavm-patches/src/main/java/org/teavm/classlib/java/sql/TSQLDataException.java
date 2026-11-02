package org.teavm.classlib.java.sql;

/** java.sql.SQLDataException for programs run in the browser. */
public class TSQLDataException extends TSQLNonTransientException {
    private static final long serialVersionUID = 1L;

    public TSQLDataException() {
        super();
    }

    public TSQLDataException(String reason) {
        super(reason);
    }

    public TSQLDataException(String reason, String sqlState) {
        super(reason, sqlState);
    }

    public TSQLDataException(String reason, String sqlState, int vendorCode) {
        super(reason, sqlState, vendorCode);
    }

    public TSQLDataException(Throwable cause) {
        super(cause);
    }

    public TSQLDataException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
