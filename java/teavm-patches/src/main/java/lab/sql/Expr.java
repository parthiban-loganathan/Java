package lab.sql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** SQL expressions. */
abstract class Expr {
    /** Evaluation context: one source row, or a group of rows for aggregates. */
    static final class Ctx {
        Object[] row;
        List<Object[]> group;
        Object[] params;
    }

    String text = "";           // the expression as written (column label in results)

    abstract Object eval(Ctx ctx);

    /** Resolves column names; clause is used in "Unknown column" messages. */
    void bind(Table table, String clause) {
    }

    boolean hasAggregate() {
        return false;
    }

    static final class Literal extends Expr {
        final Object value;

        Literal(Object value) {
            this.value = value;
        }

        @Override
        Object eval(Ctx ctx) {
            return value;
        }
    }

    static final class Param extends Expr {
        final int index;

        Param(int index) {
            this.index = index;
        }

        @Override
        Object eval(Ctx ctx) {
            return ctx.params[index];
        }
    }

    static final class Column extends Expr {
        final String qualifier;
        final String name;
        int index = -1;

        Column(String qualifier, String name) {
            this.qualifier = qualifier;
            this.name = name;
        }

        @Override
        void bind(Table table, String clause) {
            // one table per query: a qualifier (table name or alias) is accepted as written
            index = table == null ? -1 : table.index(name);
            if (index < 0) {
                throw SqlError.unknownColumn(qualifier == null ? name : qualifier + "." + name, clause);
            }
        }

        @Override
        Object eval(Ctx ctx) {
            Object[] row = ctx.row != null ? ctx.row : (ctx.group != null && !ctx.group.isEmpty() ? ctx.group.get(0) : null);
            return row == null ? null : row[index];
        }
    }

    static final class Unary extends Expr {
        final String op;
        final Expr operand;

        Unary(String op, Expr operand) {
            this.op = op;
            this.operand = operand;
        }

        @Override
        void bind(Table table, String clause) {
            operand.bind(table, clause);
        }

        @Override
        boolean hasAggregate() {
            return operand.hasAggregate();
        }

        @Override
        Object eval(Ctx ctx) {
            Object v = operand.eval(ctx);
            if (op.equals("NOT")) {
                Boolean b = Values.truth(v);
                return b == null ? null : Values.bool(!b);
            }
            Object n = Values.number(v);
            if (n == null) {
                return null;
            }
            if (n instanceof Long) {
                return -(Long) n;
            }
            if (n instanceof java.math.BigDecimal) {
                return ((java.math.BigDecimal) n).negate();
            }
            return -(Double) n;
        }
    }

    static final class Binary extends Expr {
        final String op;
        final Expr left;
        final Expr right;

        Binary(String op, Expr left, Expr right) {
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @Override
        void bind(Table table, String clause) {
            left.bind(table, clause);
            right.bind(table, clause);
        }

        @Override
        boolean hasAggregate() {
            return left.hasAggregate() || right.hasAggregate();
        }

        @Override
        Object eval(Ctx ctx) {
            switch (op) {
                case "AND": {
                    Boolean a = Values.truth(left.eval(ctx));
                    if (a != null && !a) {
                        return 0L;
                    }
                    Boolean b = Values.truth(right.eval(ctx));
                    if (b != null && !b) {
                        return 0L;
                    }
                    return a == null || b == null ? null : 1L;
                }
                case "OR": {
                    Boolean a = Values.truth(left.eval(ctx));
                    if (a != null && a) {
                        return 1L;
                    }
                    Boolean b = Values.truth(right.eval(ctx));
                    if (b != null && b) {
                        return 1L;
                    }
                    return a == null || b == null ? null : 0L;
                }
                default:
                    break;
            }
            Object a = left.eval(ctx);
            Object b = right.eval(ctx);
            switch (op) {
                case "=":
                case "<>":
                case "<":
                case "<=":
                case ">":
                case ">=": {
                    Integer c = Values.compare(a, b);
                    if (c == null) {
                        return null;
                    }
                    boolean r = op.equals("=") ? c == 0 : op.equals("<>") ? c != 0 : op.equals("<") ? c < 0
                            : op.equals("<=") ? c <= 0 : op.equals(">") ? c > 0 : c >= 0;
                    return r ? 1L : 0L;
                }
                case "<=>": {
                    if (a == null || b == null) {
                        return a == null && b == null ? 1L : 0L;
                    }
                    return Values.compare(a, b) == 0 ? 1L : 0L;
                }
                default:
                    break;
            }
            return Values.arithmetic(op, a, b);
        }
    }

    static final class IsNull extends Expr {
        final Expr operand;
        final boolean not;

        IsNull(Expr operand, boolean not) {
            this.operand = operand;
            this.not = not;
        }

        @Override
        void bind(Table table, String clause) {
            operand.bind(table, clause);
        }

        @Override
        Object eval(Ctx ctx) {
            boolean isNull = operand.eval(ctx) == null;
            return isNull != not ? 1L : 0L;
        }
    }

    static final class Like extends Expr {
        final Expr operand;
        final Expr pattern;
        final boolean not;

        Like(Expr operand, Expr pattern, boolean not) {
            this.operand = operand;
            this.pattern = pattern;
            this.not = not;
        }

        @Override
        void bind(Table table, String clause) {
            operand.bind(table, clause);
            pattern.bind(table, clause);
        }

        @Override
        Object eval(Ctx ctx) {
            Object v = operand.eval(ctx);
            Object p = pattern.eval(ctx);
            if (v == null || p == null) {
                return null;
            }
            boolean m = matches(Values.text(v).toLowerCase(), 0, Values.text(p).toLowerCase(), 0);
            return m != not ? 1L : 0L;
        }

        private static boolean matches(String s, int i, String p, int j) {
            while (j < p.length()) {
                char c = p.charAt(j);
                if (c == '%') {
                    for (int k = i; k <= s.length(); k++) {
                        if (matches(s, k, p, j + 1)) {
                            return true;
                        }
                    }
                    return false;
                }
                if (i >= s.length()) {
                    return false;
                }
                if (c == '\\' && j + 1 < p.length()) {
                    j++;
                    c = p.charAt(j);
                    if (s.charAt(i) != c) {
                        return false;
                    }
                } else if (c != '_' && s.charAt(i) != c) {
                    return false;
                }
                i++;
                j++;
            }
            return i == s.length();
        }
    }

    static final class In extends Expr {
        final Expr operand;
        final List<Expr> list;
        final boolean not;

        In(Expr operand, List<Expr> list, boolean not) {
            this.operand = operand;
            this.list = list;
            this.not = not;
        }

        @Override
        void bind(Table table, String clause) {
            operand.bind(table, clause);
            for (Expr e : list) {
                e.bind(table, clause);
            }
        }

        @Override
        Object eval(Ctx ctx) {
            Object v = operand.eval(ctx);
            if (v == null) {
                return null;
            }
            boolean sawNull = false;
            for (Expr e : list) {
                Object w = e.eval(ctx);
                Integer c = Values.compare(v, w);
                if (c == null) {
                    sawNull = true;
                } else if (c == 0) {
                    return not ? 0L : 1L;
                }
            }
            return sawNull ? null : (not ? 1L : 0L);
        }
    }

    static final class Between extends Expr {
        final Expr operand;
        final Expr low;
        final Expr high;
        final boolean not;

        Between(Expr operand, Expr low, Expr high, boolean not) {
            this.operand = operand;
            this.low = low;
            this.high = high;
            this.not = not;
        }

        @Override
        void bind(Table table, String clause) {
            operand.bind(table, clause);
            low.bind(table, clause);
            high.bind(table, clause);
        }

        @Override
        Object eval(Ctx ctx) {
            Object v = operand.eval(ctx);
            Integer a = Values.compare(v, low.eval(ctx));
            Integer b = Values.compare(v, high.eval(ctx));
            if (a == null || b == null) {
                return null;
            }
            boolean in = a >= 0 && b <= 0;
            return in != not ? 1L : 0L;
        }
    }

    static final class Function extends Expr {
        final String name;
        final List<Expr> args;

        Function(String name, List<Expr> args) {
            this.name = name;
            this.args = args;
        }

        @Override
        void bind(Table table, String clause) {
            for (Expr e : args) {
                e.bind(table, clause);
            }
        }

        @Override
        boolean hasAggregate() {
            for (Expr e : args) {
                if (e.hasAggregate()) {
                    return true;
                }
            }
            return false;
        }

        private Object arg(int i, Ctx ctx) {
            if (i >= args.size()) {
                throw new SqlError("Incorrect parameter count in the call to native function '" + name + "'",
                        1582, "42000", SqlError.SYNTAX);
            }
            return args.get(i).eval(ctx);
        }

        @Override
        Object eval(Ctx ctx) {
            switch (name) {
                case "UPPER":
                case "UCASE": {
                    Object v = arg(0, ctx);
                    return v == null ? null : Values.text(v).toUpperCase();
                }
                case "LOWER":
                case "LCASE": {
                    Object v = arg(0, ctx);
                    return v == null ? null : Values.text(v).toLowerCase();
                }
                case "LENGTH":
                case "CHAR_LENGTH": {
                    Object v = arg(0, ctx);
                    return v == null ? null : (Object) (long) Values.text(v).length();
                }
                case "TRIM": {
                    Object v = arg(0, ctx);
                    return v == null ? null : Values.text(v).trim();
                }
                case "ABS": {
                    Object v = Values.number(arg(0, ctx));
                    if (v == null) {
                        return null;
                    }
                    return v instanceof Long ? (Object) Math.abs((Long) v) : v instanceof java.math.BigDecimal
                            ? (Object) ((java.math.BigDecimal) v).abs() : (Object) Math.abs((Double) v);
                }
                case "ROUND": {
                    Object v = Values.number(arg(0, ctx));
                    if (v == null) {
                        return null;
                    }
                    int d = args.size() > 1 ? (int) Values.toLong(arg(1, ctx)) : 0;
                    if (v instanceof Long && d >= 0) {
                        return v;
                    }
                    java.math.BigDecimal r = Values.toDecimal(v).setScale(d, java.math.RoundingMode.HALF_UP);
                    if (v instanceof Double) {
                        return r.doubleValue();
                    }
                    return d <= 0 ? (Object) r.longValue() : (Object) r;
                }
                case "FLOOR": {
                    Object v = Values.number(arg(0, ctx));
                    return v == null ? null : (Object) (long) Math.floor(Values.toDouble(v));
                }
                case "CEIL":
                case "CEILING": {
                    Object v = Values.number(arg(0, ctx));
                    return v == null ? null : (Object) (long) Math.ceil(Values.toDouble(v));
                }
                case "CONCAT": {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < args.size(); i++) {
                        Object v = arg(i, ctx);
                        if (v == null) {
                            return null;
                        }
                        sb.append(Values.text(v));
                    }
                    return sb.toString();
                }
                case "COALESCE":
                case "IFNULL": {
                    for (int i = 0; i < args.size(); i++) {
                        Object v = arg(i, ctx);
                        if (v != null) {
                            return v;
                        }
                    }
                    return null;
                }
                case "IF": {
                    Boolean c = Values.truth(arg(0, ctx));
                    return c != null && c ? arg(1, ctx) : arg(2, ctx);
                }
                default:
                    throw new SqlError("FUNCTION " + name.toLowerCase() + " does not exist", 1305, "42000", SqlError.SYNTAX);
            }
        }
    }

    static final class Aggregate extends Expr {
        final String name;
        final Expr arg;               // null for COUNT(*)
        final boolean distinct;

        Aggregate(String name, Expr arg, boolean distinct) {
            this.name = name;
            this.arg = arg;
            this.distinct = distinct;
        }

        @Override
        void bind(Table table, String clause) {
            if (arg != null) {
                arg.bind(table, clause);
            }
        }

        @Override
        boolean hasAggregate() {
            return true;
        }

        @Override
        Object eval(Ctx ctx) {
            List<Object[]> rows = ctx.group;
            if (rows == null) {
                rows = new ArrayList<>();
                if (ctx.row != null) {
                    rows.add(ctx.row);
                }
            }
            Ctx inner = new Ctx();
            inner.params = ctx.params;
            List<Object> values = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (Object[] r : rows) {
                inner.row = r;
                Object v = arg == null ? 1L : arg.eval(inner);
                if (v == null) {
                    continue;
                }
                if (distinct && !seen.add(Values.key(v))) {
                    continue;
                }
                values.add(v);
            }
            switch (name) {
                case "COUNT":
                    return (long) values.size();
                case "SUM":
                case "AVG": {
                    if (values.isEmpty()) {
                        return null;
                    }
                    Object total = 0L;
                    boolean floating = false;
                    for (Object v : values) {
                        Object n = Values.number(v);
                        floating |= n instanceof Double;
                        total = Values.arithmetic("+", total, n);
                    }
                    if (name.equals("SUM")) {
                        return total;
                    }
                    if (floating) {
                        return Values.toDouble(total) / values.size();
                    }
                    java.math.BigDecimal sum = Values.toDecimal(total);
                    return sum.divide(java.math.BigDecimal.valueOf(values.size()), Math.max(0, sum.scale()) + Values.DIV_SCALE,
                            java.math.RoundingMode.HALF_UP);
                }
                case "MIN":
                case "MAX": {
                    Object best = null;
                    for (Object v : values) {
                        if (best == null) {
                            best = v;
                        } else {
                            int c = Values.compare(v, best);
                            if (name.equals("MIN") ? c < 0 : c > 0) {
                                best = v;
                            }
                        }
                    }
                    return best;
                }
                default:
                    throw SqlError.general("aggregate " + name);
            }
        }
    }
}
