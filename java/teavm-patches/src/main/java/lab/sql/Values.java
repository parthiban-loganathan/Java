package lab.sql;

/** Values of the embedded database: Long, Double, String or null, with MySQL's conversions. */
final class Values {
    private Values() {
    }

    static boolean isNumber(Object v) {
        return v instanceof Long || v instanceof Double;
    }

    /** Number from a value; strings are read like MySQL does (leading numeric part, otherwise 0). */
    static Object number(Object v) {
        if (v == null || v instanceof Long || v instanceof Double) {
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
        boolean integer = true;
        if (i < n && s.charAt(i) == '.') {
            integer = false;
            i++;
            while (i < n && Character.isDigit(s.charAt(i))) {
                i++;
            }
        }
        if (i == digitsStart) {
            return 0L;
        }
        if (i < n && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
            int j = i + 1;
            if (j < n && (s.charAt(j) == '+' || s.charAt(j) == '-')) {
                j++;
            }
            if (j < n && Character.isDigit(s.charAt(j))) {
                integer = false;
                i = j;
                while (i < n && Character.isDigit(s.charAt(i))) {
                    i++;
                }
            }
        }
        String t = s.substring(0, i);
        if (integer) {
            try {
                return Long.parseLong(t.startsWith("+") ? t.substring(1) : t);
            } catch (NumberFormatException e) {
                return Double.parseDouble(t);
            }
        }
        return Double.parseDouble(t);
    }

    static double toDouble(Object v) {
        Object n = number(v);
        return n == null ? 0 : n instanceof Long ? (double) (Long) n : (Double) n;
    }

    static long toLong(Object v) {
        Object n = number(v);
        if (n == null) {
            return 0;
        }
        if (n instanceof Long) {
            return (Long) n;
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
        return toDouble(v) != 0;
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

    /** Text of a value as MySQL returns it (DOUBLE 55000 is "55000"). */
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
        if (n instanceof Double && (Double) n == Math.rint((Double) n) && Math.abs((Double) n) < 1e15) {
            return "n" + (long) (double) (Double) n;
        }
        return "n" + n;
    }
}
