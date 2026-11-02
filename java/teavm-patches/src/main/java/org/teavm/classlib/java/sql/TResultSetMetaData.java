package org.teavm.classlib.java.sql;

/** java.sql.ResultSetMetaData (the commonly used methods). */
public interface TResultSetMetaData {
    int columnNoNulls = 0;
    int columnNullable = 1;
    int columnNullableUnknown = 2;

    int getColumnCount() throws TSQLException;

    String getColumnName(int column) throws TSQLException;

    String getColumnLabel(int column) throws TSQLException;

    int getColumnType(int column) throws TSQLException;

    String getColumnTypeName(int column) throws TSQLException;

    String getTableName(int column) throws TSQLException;

    int getColumnDisplaySize(int column) throws TSQLException;

    int isNullable(int column) throws TSQLException;

    boolean isAutoIncrement(int column) throws TSQLException;
}
