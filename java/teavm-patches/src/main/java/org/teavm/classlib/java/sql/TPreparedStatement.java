package org.teavm.classlib.java.sql;

/** java.sql.PreparedStatement (the commonly used methods). */
public interface TPreparedStatement extends TStatement {
    TResultSet executeQuery() throws TSQLException;

    int executeUpdate() throws TSQLException;

    long executeLargeUpdate() throws TSQLException;

    boolean execute() throws TSQLException;

    void addBatch() throws TSQLException;

    void clearParameters() throws TSQLException;

    void setNull(int parameterIndex, int sqlType) throws TSQLException;

    void setBoolean(int parameterIndex, boolean x) throws TSQLException;

    void setByte(int parameterIndex, byte x) throws TSQLException;

    void setShort(int parameterIndex, short x) throws TSQLException;

    void setInt(int parameterIndex, int x) throws TSQLException;

    void setLong(int parameterIndex, long x) throws TSQLException;

    void setFloat(int parameterIndex, float x) throws TSQLException;

    void setDouble(int parameterIndex, double x) throws TSQLException;

    void setBigDecimal(int parameterIndex, java.math.BigDecimal x) throws TSQLException;

    void setString(int parameterIndex, String x) throws TSQLException;

    void setObject(int parameterIndex, Object x) throws TSQLException;

    void setObject(int parameterIndex, Object x, int targetSqlType) throws TSQLException;

    TResultSetMetaData getMetaData() throws TSQLException;
}
