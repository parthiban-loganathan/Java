package lab.sql;

import java.util.ArrayList;
import java.util.List;

/** A table of the embedded database. */
final class Table {
    static final class Column {
        String name;
        String type = "VARCHAR";     // upper-case SQL type name as declared
        int length = -1;
        int scale = -1;
        boolean notNull;
        boolean autoIncrement;
        boolean primaryKey;
        boolean unique;
        Object defaultValue;

        Column copy() {
            Column c = new Column();
            c.name = name;
            c.type = type;
            c.length = length;
            c.scale = scale;
            c.notNull = notNull;
            c.autoIncrement = autoIncrement;
            c.primaryKey = primaryKey;
            c.unique = unique;
            c.defaultValue = defaultValue;
            return c;
        }

        boolean isInteger() {
            return type.equals("INT") || type.equals("INTEGER") || type.equals("BIGINT") || type.equals("SMALLINT")
                    || type.equals("TINYINT") || type.equals("MEDIUMINT") || type.equals("BOOLEAN") || type.equals("BOOL")
                    || type.equals("BIT") || type.equals("SERIAL") || type.equals("YEAR");
        }

        boolean isDecimal() {
            return type.equals("DECIMAL") || type.equals("NUMERIC") || type.equals("DEC") || type.equals("FIXED");
        }

        boolean isFloating() {
            return type.equals("DOUBLE") || type.equals("FLOAT") || type.equals("REAL") || isDecimal();
        }

        /** java.sql.Types code. */
        int sqlType() {
            switch (type) {
                case "INT":
                case "INTEGER":
                case "MEDIUMINT":
                case "YEAR":
                    return 4;
                case "BIGINT":
                case "SERIAL":
                    return -5;
                case "SMALLINT":
                    return 5;
                case "TINYINT":
                    return length == 1 ? -7 : -6;
                case "BOOLEAN":
                case "BOOL":
                case "BIT":
                    return -7;
                case "DOUBLE":
                case "REAL":
                    return 8;
                case "FLOAT":
                    return 7;
                case "DECIMAL":
                case "NUMERIC":
                case "DEC":
                case "FIXED":
                    return 3;
                case "DATE":
                    return 91;
                case "DATETIME":
                case "TIMESTAMP":
                    return 93;
                case "TIME":
                    return 92;
                case "CHAR":
                    return 1;
                case "TEXT":
                case "LONGTEXT":
                case "MEDIUMTEXT":
                    return -1;
                default:
                    return 12;
            }
        }

        /** Value converted for storing in this column (MySQL rounds numbers for integer columns). */
        Object store(Object v, String table) {
            if (v == null) {
                return null;
            }
            if (isInteger()) {
                Object n = Values.number(v);
                if (v instanceof String && !isNumericText((String) v)) {
                    throw new SqlError("Incorrect integer value: '" + v + "' for column '" + name + "' at row 1",
                            1366, "HY000", SqlError.GENERAL);
                }
                return n instanceof Long ? n : Math.round((Double) n);
            }
            if (isFloating()) {
                if (v instanceof String && !isNumericText((String) v)) {
                    throw new SqlError("Incorrect " + (isDecimal() ? "decimal" : "double") + " value: '" + v
                            + "' for column '" + name + "' at row 1", 1366, "HY000", SqlError.GENERAL);
                }
                double d = Values.toDouble(v);
                if (isDecimal()) {
                    int s = scale < 0 ? 0 : scale;
                    d = new java.math.BigDecimal(Double.toString(d)).setScale(s, java.math.RoundingMode.HALF_UP).doubleValue();
                }
                return d;
            }
            String s = Values.text(v);
            if (length >= 0 && s.length() > length && (type.equals("VARCHAR") || type.equals("CHAR"))) {
                throw new SqlError("Data too long for column '" + name + "' at row 1", 1406, "22001", SqlError.GENERAL);
            }
            return s;
        }

        private static boolean isNumericText(String s) {
            String t = s.trim();
            if (t.isEmpty()) {
                return false;
            }
            try {
                Double.parseDouble(t);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    final String name;
    final List<Column> columns = new ArrayList<>();
    List<Object[]> rows = new ArrayList<>();
    long autoIncrement = 1;

    Table(String name) {
        this.name = name;
    }

    int index(String column) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).name.equalsIgnoreCase(column)) {
                return i;
            }
        }
        return -1;
    }

    Table copy() {
        Table t = new Table(name);
        for (Column c : columns) {
            t.columns.add(c.copy());
        }
        for (Object[] r : rows) {
            t.rows.add(r.clone());
        }
        t.autoIncrement = autoIncrement;
        return t;
    }
}
