package org.teavm.classlib.java.sql;

import lab.sql.SqlError;

/** SqlError to the SQLException subclass MySQL Connector/J would throw. */
final class TLabSqlErrors {
    private TLabSqlErrors() {
    }

    static TSQLException of(SqlError e) {
        switch (e.kind) {
            case SqlError.SYNTAX:
                return new TSQLSyntaxErrorException(e.getMessage(), e.state, e.code);
            case SqlError.INTEGRITY:
                return new TSQLIntegrityConstraintViolationException(e.getMessage(), e.state, e.code);
            default:
                return new TSQLException(e.getMessage(), e.state, e.code);
        }
    }
}
