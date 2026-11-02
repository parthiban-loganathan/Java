package org.teavm.classlib.java.sql;

/** java.sql.SQLSyntaxErrorException for programs run in the browser. */
public class TSQLSyntaxErrorException extends TSQLNonTransientException {
    private static final long serialVersionUID = 1L;

    public TSQLSyntaxErrorException() {
        super();
    }

    public TSQLSyntaxErrorException(String reason) {
        super(reason);
    }

    public TSQLSyntaxErrorException(String reason, String sqlState) {
        super(reason, sqlState);
    }

    public TSQLSyntaxErrorException(String reason, String sqlState, int vendorCode) {
        super(reason, sqlState, vendorCode);
    }

    public TSQLSyntaxErrorException(Throwable cause) {
        super(cause);
    }

    public TSQLSyntaxErrorException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
