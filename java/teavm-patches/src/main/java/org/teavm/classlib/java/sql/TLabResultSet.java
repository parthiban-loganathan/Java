package org.teavm.classlib.java.sql;

import lab.sql.Result;

/** Result set of the embedded database; getters convert like MySQL Connector/J. */
final class TLabResultSet implements TResultSet, TResultSetMetaData {
    private final TStatement statement;
    private final Result result;
    private int row = -1;
    private boolean closed;
    private boolean lastNull;

    TLabResultSet(TStatement statement, Result result) {
        this.statement = statement;
        this.result = result;
    }

    private void check() throws TSQLException {
        if (closed) {
            throw new TSQLException("Operation not allowed after ResultSet closed", "S1000");
        }
    }

    private Object value(int column) throws TSQLException {
        check();
        if (row < 0 || row >= result.rows.size()) {
            throw new TSQLException(row < 0 ? "Before start of result set" : "After end of result set", "S1000");
        }
        if (column < 1 || column > result.labels.length) {
            throw new TSQLException("Column Index out of range, " + column + " > " + result.labels.length + ". ", "S1009");
        }
        Object v = result.rows.get(row)[column - 1];
        lastNull = v == null;
        return v;
    }

    @Override
    public int findColumn(String label) throws TSQLException {
        check();
        for (int i = 0; i < result.labels.length; i++) {
            if (result.labels[i].equalsIgnoreCase(label)) {
                return i + 1;
            }
        }
        for (int i = 0; i < result.names.length; i++) {
            if (result.names[i].equalsIgnoreCase(label)) {
                return i + 1;
            }
        }
        throw new TSQLException("Column '" + label + "' not found.", "S0022");
    }

    // ------------------------------------------------------------ cursor
    @Override
    public boolean next() throws TSQLException {
        check();
        if (row < result.rows.size()) {
            row++;
        }
        return row < result.rows.size();
    }

    @Override
    public boolean previous() throws TSQLException {
        check();
        if (row >= 0) {
            row--;
        }
        return row >= 0;
    }

    @Override
    public boolean first() throws TSQLException {
        check();
        row = 0;
        return !result.rows.isEmpty();
    }

    @Override
    public boolean last() throws TSQLException {
        check();
        row = result.rows.size() - 1;
        return row >= 0;
    }

    @Override
    public void beforeFirst() throws TSQLException {
        check();
        row = -1;
    }

    @Override
    public void afterLast() throws TSQLException {
        check();
        row = result.rows.size();
    }

    @Override
    public boolean absolute(int r) throws TSQLException {
        check();
        row = r > 0 ? r - 1 : result.rows.size() + r;
        if (row < 0) {
            row = -1;
            return false;
        }
        if (row >= result.rows.size()) {
            row = result.rows.size();
            return false;
        }
        return true;
    }

    @Override
    public boolean isBeforeFirst() throws TSQLException {
        check();
        return row < 0 && !result.rows.isEmpty();
    }

    @Override
    public boolean isAfterLast() throws TSQLException {
        check();
        return row >= result.rows.size() && !result.rows.isEmpty();
    }

    @Override
    public int getRow() throws TSQLException {
        check();
        return row >= 0 && row < result.rows.size() ? row + 1 : 0;
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean wasNull() throws TSQLException {
        check();
        return lastNull;
    }

    @Override
    public TResultSetMetaData getMetaData() throws TSQLException {
        check();
        return this;
    }

    @Override
    public TStatement getStatement() {
        return statement;
    }

    // ------------------------------------------------------------ getters
    private static double number(Object v, String target) throws TSQLException {
        if (v instanceof Long) {
            return (Long) v;
        }
        if (v instanceof Double) {
            return (Double) v;
        }
        String s = v.toString().trim();
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new TSQLException("Cannot determine value type from string '" + v + "'", "S1009");
        }
    }

    private static long integral(Object v, String target, long min, long max) throws TSQLException {
        long r;
        if (v instanceof Long) {
            r = (Long) v;
        } else {
            double d = number(v, target);
            r = (long) d;
            if (Math.abs(d) >= 9.2e18) {
                throw new TSQLException("Value '" + v + "' is outside of valid range for type " + target, "22003");
            }
        }
        if (r < min || r > max) {
            throw new TSQLException("Value '" + v + "' is outside of valid range for type " + target, "22003");
        }
        return r;
    }

    @Override
    public String getString(int c) throws TSQLException {
        Object v = value(c);
        if (v == null) {
            return null;
        }
        int scale = c - 1 < result.scales.length ? result.scales[c - 1] : -1;
        if (scale >= 0) {               // DECIMAL(p,s): always s decimals, as MySQL returns it
            return new java.math.BigDecimal(v instanceof Double ? Double.toString((Double) v) : v.toString())
                    .setScale(scale, java.math.RoundingMode.HALF_UP).toPlainString();
        }
        if (v instanceof Double) {
            double d = (Double) v;
            if (d == Math.rint(d) && Math.abs(d) < 1e15) {
                return Long.toString((long) d);
            }
            return Double.toString(d);
        }
        return v.toString();
    }

    @Override
    public boolean getBoolean(int c) throws TSQLException {
        Object v = value(c);
        if (v == null) {
            return false;
        }
        if (v instanceof String) {
            String s = ((String) v).trim();
            if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("y") || s.equalsIgnoreCase("yes")) {
                return true;
            }
            if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("n") || s.equalsIgnoreCase("no") || s.isEmpty()) {
                return false;
            }
        }
        return number(v, "BOOLEAN") != 0;
    }

    @Override
    public byte getByte(int c) throws TSQLException {
        Object v = value(c);
        return v == null ? 0 : (byte) integral(v, "TINYINT", Byte.MIN_VALUE, Byte.MAX_VALUE);
    }

    @Override
    public short getShort(int c) throws TSQLException {
        Object v = value(c);
        return v == null ? 0 : (short) integral(v, "SMALLINT", Short.MIN_VALUE, Short.MAX_VALUE);
    }

    @Override
    public int getInt(int c) throws TSQLException {
        Object v = value(c);
        return v == null ? 0 : (int) integral(v, "INTEGER", Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public long getLong(int c) throws TSQLException {
        Object v = value(c);
        return v == null ? 0 : integral(v, "BIGINT", Long.MIN_VALUE, Long.MAX_VALUE);
    }

    @Override
    public float getFloat(int c) throws TSQLException {
        Object v = value(c);
        return v == null ? 0 : (float) number(v, "FLOAT");
    }

    @Override
    public double getDouble(int c) throws TSQLException {
        Object v = value(c);
        return v == null ? 0 : number(v, "DOUBLE");
    }

    @Override
    public java.math.BigDecimal getBigDecimal(int c) throws TSQLException {
        Object v = value(c);
        if (v == null) {
            return null;
        }
        if (v instanceof Long) {
            return java.math.BigDecimal.valueOf((Long) v);
        }
        return new java.math.BigDecimal(getString(c));
    }

    @Override
    public Object getObject(int c) throws TSQLException {
        Object v = value(c);
        if (v == null) {
            return null;
        }
        int type = result.sqlTypes[c - 1];
        switch (type) {
            case 4:
            case 5:
            case -6:
                return (int) (long) (Long) v;
            case -7:
                return ((Long) v) != 0;
            case 3:
                return getBigDecimal(c);
            case 7:
                return (float) (double) (Double) v;
            default:
                return v;
        }
    }

    @Override
    public String getString(String label) throws TSQLException {
        return getString(findColumn(label));
    }

    @Override
    public boolean getBoolean(String label) throws TSQLException {
        return getBoolean(findColumn(label));
    }

    @Override
    public byte getByte(String label) throws TSQLException {
        return getByte(findColumn(label));
    }

    @Override
    public short getShort(String label) throws TSQLException {
        return getShort(findColumn(label));
    }

    @Override
    public int getInt(String label) throws TSQLException {
        return getInt(findColumn(label));
    }

    @Override
    public long getLong(String label) throws TSQLException {
        return getLong(findColumn(label));
    }

    @Override
    public float getFloat(String label) throws TSQLException {
        return getFloat(findColumn(label));
    }

    @Override
    public double getDouble(String label) throws TSQLException {
        return getDouble(findColumn(label));
    }

    @Override
    public java.math.BigDecimal getBigDecimal(String label) throws TSQLException {
        return getBigDecimal(findColumn(label));
    }

    @Override
    public Object getObject(String label) throws TSQLException {
        return getObject(findColumn(label));
    }

    // ------------------------------------------------------------ metadata
    private void checkColumn(int c) throws TSQLException {
        if (c < 1 || c > result.labels.length) {
            throw new TSQLException("Column index out of range.", "S1002");
        }
    }

    @Override
    public int getColumnCount() {
        return result.labels.length;
    }

    @Override
    public String getColumnName(int c) throws TSQLException {
        checkColumn(c);
        return result.names[c - 1];
    }

    @Override
    public String getColumnLabel(int c) throws TSQLException {
        checkColumn(c);
        return result.labels[c - 1];
    }

    @Override
    public int getColumnType(int c) throws TSQLException {
        checkColumn(c);
        return result.sqlTypes[c - 1];
    }

    @Override
    public String getColumnTypeName(int c) throws TSQLException {
        checkColumn(c);
        return result.typeNames[c - 1];
    }

    @Override
    public String getTableName(int c) throws TSQLException {
        checkColumn(c);
        return result.table;
    }

    @Override
    public int getColumnDisplaySize(int c) throws TSQLException {
        checkColumn(c);
        return 255;
    }

    @Override
    public int isNullable(int c) throws TSQLException {
        checkColumn(c);
        return columnNullableUnknown;
    }

    @Override
    public boolean isAutoIncrement(int c) throws TSQLException {
        checkColumn(c);
        return false;
    }
}
