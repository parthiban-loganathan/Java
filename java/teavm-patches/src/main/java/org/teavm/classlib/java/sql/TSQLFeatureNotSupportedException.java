package org.teavm.classlib.java.sql;

/** java.sql.SQLFeatureNotSupportedException for programs run in the browser. */
public class TSQLFeatureNotSupportedException extends TSQLNonTransientException {
    private static final long serialVersionUID = 1L;

    public TSQLFeatureNotSupportedException() {
        super();
    }

    public TSQLFeatureNotSupportedException(String reason) {
        super(reason);
    }

    public TSQLFeatureNotSupportedException(String reason, String sqlState) {
        super(reason, sqlState);
    }

    public TSQLFeatureNotSupportedException(String reason, String sqlState, int vendorCode) {
        super(reason, sqlState, vendorCode);
    }

    public TSQLFeatureNotSupportedException(Throwable cause) {
        super(cause);
    }

    public TSQLFeatureNotSupportedException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
