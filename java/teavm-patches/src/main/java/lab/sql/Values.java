package lab.sql;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Values of the embedded database: Long (integer types), BigDecimal (DECIMAL and exact numeric
 * literals such as 1.5), Double (DOUBLE, FLOAT), String or null, with MySQL's conversions.
 */
final class Values {
    /** MySQL's div_precision_increment: decimal places added by '/' and AVG. */
    static final int DIV_SCALE = 4;

    private Values() {
    }

    static boolean isNumber(Object v) {
        return v instanceof Long || v instanceof Double || v instanceof BigDecimal;
    }

    /** Number from a value; strings are read like MySQL does (leading numeric part, otherwise 0). */
    static Object number(Object v) {
        if (v == null || v instanceof Long || v instanceof Double || v instanceof BigDecimal) {
            return v;
        }
        if (v instanceof Boolean) {
            return ((Boolean) v) ? 1L : 0L;
        }
        String s = v.toString().trim();
        int i = 0;
        int n = s.length();
        if (i < n && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
            i++;
        }
        int digitsStart = i;
        while (i < n && Character.isDigit(s.charAt(i))) {
            i++;
        }
        if (i < n && s.charAt(i) == '.') {
            i++;
            while (i < n && Character.isDigit(s.charAt(i))) {
                i++;
            }
        }
        if (i == digitsStart || (i == digitsStart + 1 && s.charAt(digitsStart) == '.')) {
            return 0L;
        }
        if (i < n && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
            int j = i + 1;
            if (j < n && (s.charAt(j) == '+' || s.charAt(j) == '-')) {
                j++;
            }
            if (j < n && Character.isDigit(s.charAt(j))) {
                i = j;
                while (i < n && Character.isDigit(s.charAt(i))) {
                    i++;
                }
            }
        }
        // strings in numeric context are DOUBLE in MySQL
        return Double.parseDouble(s.substring(0, i));
    }

    /** Numeric literal as written in SQL: 12 -> Long, 1.5 -> DECIMAL, 1e3 -> DOUBLE. */
    static Object literal(String text) {
        if (text.indexOf('e') >= 0 || text.indexOf('E') >= 0) {
            return Double.parseDouble(text);
        }
        if (text.indexOf('.') >= 0) {
            return new BigDecimal(text.startsWith(".") ? "0" + text : text);
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return new BigDecimal(text);
        }
    }

    static double toDouble(Object v) {
        Object n = number(v);
        if (n == null) {
            return 0;
        }
        if (n instanceof Long) {
            return (Long) n;
        }
        if (n instanceof BigDecimal) {
            return ((BigDecimal) n).doubleValue();
        }
        return (Double) n;
    }

    static BigDecimal toDecimal(Object v) {
        Object n = number(v);
        if (n instanceof BigDecimal) {
            return (BigDecimal) n;
        }
        if (n instanceof Long) {
            return BigDecimal.valueOf((Long) n);
        }
        return new BigDecimal(Double.toString(toDouble(n)));
    }

    static long toLong(Object v) {
        Object n = number(v);
        if (n == null) {
            return 0;
        }
        if (n instanceof Long) {
            return (Long) n;
        }
        if (n instanceof BigDecimal) {
            return ((BigDecimal) n).setScale(0, RoundingMode.HALF_UP).longValue();
        }
        return Math.round((Double) n);
    }

    /** MySQL truth: NULL is unknown (null), numbers are true when not 0. */
    static Boolean truth(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        Object n = number(v);
        if (n instanceof BigDecimal) {
            return ((BigDecimal) n).signum() != 0;
        }
        return toDouble(n) != 0;
    }

    static Object bool(Boolean b) {
        return b == null ? null : (b ? 1L : 0L);
    }

    /** Comparison; null when either side is NULL. Strings compare without regard to case. */
    static Integer compare(Object a, Object b) {
        if (a == null || b == null) {
            return null;
        }
        if (a instanceof String && b instanceof String) {
            int c = ((String) a).compareToIgnoreCase((String) b);
            return c < 0 ? -1 : c > 0 ? 1 : 0;
        }
        Object x = number(a);
        Object y = number(b);
        if (x instanceof Long && y instanceof Long) {
            return Long.compare((Long) x, (Long) y);
        }
        if (!(x instanceof Double) && !(y instanceof Double)) {
            int c = toDecimal(x).compareTo(toDecimal(y));
            return c < 0 ? -1 : c > 0 ? 1 : 0;
        }
        return Double.compare(toDouble(x), toDouble(y));
    }

    /** Ordering for ORDER BY: NULL first, like MySQL ascending order. */
    static int order(Object a, Object b) {
        if (a == null) {
            return b == null ? 0 : -1;
        }
        if (b == null) {
            return 1;
        }
        return compare(a, b);
    }

    /** Arithmetic with MySQL's result types (integer, exact DECIMAL, or DOUBLE). */
    static Object arithmetic(String op, Object a, Object b) {
        if (a == null || b == null) {
            return null;
        }
        Object x = number(a);
        Object y = number(b);
        if (x instanceof Double || y instanceof Double) {
            double p = toDouble(x);
            double q = toDouble(y);
            switch (op) {
                case "+":
                    return p + q;
                case "-":
                    return p - q;
                case "*":
                    return p * q;
                case "/":
                    return q == 0 ? null : (Object) (p / q);
                case "%":
                    return q == 0 ? null : (Object) (p % q);
                default:
                    return q == 0 ? null : (Object) (long) (p / q);      // DIV
            }
        }
        if (x instanceof Long && y instanceof Long && !op.equals("/")) {
            long p = (Long) x;
            long q = (Long) y;
            switch (op) {
                case "+":
                    return p + q;
                case "-":
                    return p - q;
                case "*":
                    return p * q;
                case "%":
                    return q == 0 ? null : (Object) (p % q);
                default:
                    return q == 0 ? null : (Object) (p / q);           // DIV
            }
        }
        BigDecimal p = toDecimal(x);
        BigDecimal q = toDecimal(y);
        switch (op) {
            case "+":
                return p.add(q);
            case "-":
                return p.subtract(q);
            case "*":
                return p.multiply(q);
            case "/":
                return q.signum() == 0 ? null : (Object) p.divide(q, Math.max(0, p.scale()) + DIV_SCALE, RoundingMode.HALF_UP);
            case "%":
                return q.signum() == 0 ? null : (Object) p.remainder(q);
            default:
                return q.signum() == 0 ? null : (Object) p.divide(q, 0, RoundingMode.DOWN).longValue();
        }
    }

    /** Text of a value as MySQL converts it to a string (DOUBLE 55000 is "55000"). */
    static String text(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Double) {
            double d = (Double) v;
            if (d == Math.rint(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
                return Long.toString((long) d);
            }
            return Double.toString(d);
        }
        if (v instanceof BigDecimal) {
            return ((BigDecimal) v).toPlainString();
        }
        return v.toString();
    }

    static String key(Object v) {
        if (v == null) {
            return "\0null";
        }
        if (v instanceof String) {
            return "s" + ((String) v).toLowerCase();
        }
        Object n = number(v);
        if (n instanceof Double) {
            double d = (Double) n;
            if (d == Math.rint(d) && Math.abs(d) < 1e15) {
                return "n" + (long) d;
            }
            return "n" + new BigDecimal(Double.toString(d)).stripTrailingZeros().toPlainString();
        }
        BigDecimal b = toDecimal(n).stripTrailingZeros();
        return "n" + (b.scale() <= 0 ? b.toBigInteger().toString() : b.toPlainString());
    }
}
