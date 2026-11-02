package org.teavm.classlib.java.sql;

/** java.sql.SQLIntegrityConstraintViolationException for programs run in the browser. */
public class TSQLIntegrityConstraintViolationException extends TSQLNonTransientException {
    private static final long serialVersionUID = 1L;

    public TSQLIntegrityConstraintViolationException() {
        super();
    }

    public TSQLIntegrityConstraintViolationException(String reason) {
        super(reason);
    }

    public TSQLIntegrityConstraintViolationException(String reason, String sqlState) {
        super(reason, sqlState);
    }

    public TSQLIntegrityConstraintViolationException(String reason, String sqlState, int vendorCode) {
        super(reason, sqlState, vendorCode);
    }

    public TSQLIntegrityConstraintViolationException(Throwable cause) {
        super(cause);
    }

    public TSQLIntegrityConstraintViolationException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
