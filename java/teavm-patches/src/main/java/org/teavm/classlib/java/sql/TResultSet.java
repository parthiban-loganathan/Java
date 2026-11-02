package org.teavm.classlib.java.sql;

/** java.sql.ResultSet (the commonly used methods). */
public interface TResultSet extends AutoCloseable {
    int TYPE_FORWARD_ONLY = 1003;
    int TYPE_SCROLL_INSENSITIVE = 1004;
    int TYPE_SCROLL_SENSITIVE = 1005;
    int CONCUR_READ_ONLY = 1007;
    int CONCUR_UPDATABLE = 1008;

    boolean next() throws TSQLException;

    boolean previous() throws TSQLException;

    boolean first() throws TSQLException;

    boolean last() throws TSQLException;

    void beforeFirst() throws TSQLException;

    void afterLast() throws TSQLException;

    boolean absolute(int row) throws TSQLException;

    boolean isBeforeFirst() throws TSQLException;

    boolean isAfterLast() throws TSQLException;

    int getRow() throws TSQLException;

    @Override
    void close() throws TSQLException;

    boolean isClosed() throws TSQLException;

    boolean wasNull() throws TSQLException;

    int findColumn(String columnLabel) throws TSQLException;

    TResultSetMetaData getMetaData() throws TSQLException;

    TStatement getStatement() throws TSQLException;

    String getString(int columnIndex) throws TSQLException;

    String getString(String columnLabel) throws TSQLException;

    boolean getBoolean(int columnIndex) throws TSQLException;

    boolean getBoolean(String columnLabel) throws TSQLException;

    byte getByte(int columnIndex) throws TSQLException;

    byte getByte(String columnLabel) throws TSQLException;

    short getShort(int columnIndex) throws TSQLException;

    short getShort(String columnLabel) throws TSQLException;

    int getInt(int columnIndex) throws TSQLException;

    int getInt(String columnLabel) throws TSQLException;

    long getLong(int columnIndex) throws TSQLException;

    long getLong(String columnLabel) throws TSQLException;

    float getFloat(int columnIndex) throws TSQLException;

    float getFloat(String columnLabel) throws TSQLException;

    double getDouble(int columnIndex) throws TSQLException;

    double getDouble(String columnLabel) throws TSQLException;

    java.math.BigDecimal getBigDecimal(int columnIndex) throws TSQLException;

    java.math.BigDecimal getBigDecimal(String columnLabel) throws TSQLException;

    Object getObject(int columnIndex) throws TSQLException;

    Object getObject(String columnLabel) throws TSQLException;
}
