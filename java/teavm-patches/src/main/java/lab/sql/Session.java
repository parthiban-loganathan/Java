package lab.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A connection to the embedded database: parses and executes the MySQL dialect used in the
 * course (CREATE/DROP DATABASE and TABLE, USE, INSERT, SELECT with WHERE, GROUP BY, HAVING,
 * ORDER BY, LIMIT and aggregates, UPDATE, DELETE, TRUNCATE) with MySQL's messages.
 * All databases live in memory for one program run.
 */
public final class Session {
    /** All databases: schema name (lower case) to tables (lower-case name to table). */
    private static final Map<String, Map<String, Table>> SCHEMAS = new LinkedHashMap<>();
    private static boolean initialised;

    private String schema;
    private boolean autoCommit = true;
    private Map<String, Map<String, Table>> snapshot;

    /** Forgets every database (a new program run in the same page). */
    public static void reset() {
        SCHEMAS.clear();
        initialised = false;
    }

    /** Runs the setup script (CREATE DATABASE, CREATE TABLE ...) the first time the database is used. */
    public static void initialise(String setupScript) {
        if (initialised) {
            return;
        }
        initialised = true;
        if (setupScript != null && !setupScript.trim().isEmpty()) {
            new Session(null).executeScript(setupScript);
        }
    }

    public Session(String schema) {
        if (schema != null && !schema.isEmpty()) {
            SCHEMAS.computeIfAbsent(schema.toLowerCase(), k -> new LinkedHashMap<>());
        }
        this.schema = schema == null || schema.isEmpty() ? null : schema;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String name) {
        if (!SCHEMAS.containsKey(name.toLowerCase())) {
            throw new SqlError("Unknown database '" + name + "'", 1049, "42000", SqlError.SYNTAX);
        }
        schema = name;
    }

    // ------------------------------------------------------------------ transactions
    public boolean getAutoCommit() {
        return autoCommit;
    }

    public void setAutoCommit(boolean value) {
        if (value && !autoCommit) {
            commit();
        }
        autoCommit = value;
    }

    public void commit() {
        snapshot = null;
    }

    public void rollback() {
        if (snapshot != null) {
            SCHEMAS.clear();
            SCHEMAS.putAll(snapshot);
            snapshot = null;
        }
    }

    private void beforeChange() {
        if (!autoCommit && snapshot == null) {
            snapshot = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Table>> s : SCHEMAS.entrySet()) {
                Map<String, Table> copy = new LinkedHashMap<>();
                for (Map.Entry<String, Table> t : s.getValue().entrySet()) {
                    copy.put(t.getKey(), t.getValue().copy());
                }
                snapshot.put(s.getKey(), copy);
            }
        }
    }

    // ------------------------------------------------------------------ entry points
    /** Number of ? placeholders in a statement. */
    public static int parameterCount(String sql) {
        int n = 0;
        for (Lexer.Token t : Lexer.tokens(sql)) {
            if (t.type == Lexer.PARAM) {
                n++;
            }
        }
        return n;
    }

    /** Executes several statements separated by ';'. */
    public void executeScript(String script) {
        List<Lexer.Token> tokens = Lexer.tokens(script);
        int start = 0;
        for (int i = 0; i < tokens.size(); i++) {
            Lexer.Token t = tokens.get(i);
            if (t.type == Lexer.END || (t.type == Lexer.SYMBOL && t.text.equals(";"))) {
                if (i > start) {
                    String sql = script.substring(tokens.get(start).start, tokens.get(i - 1).end);
                    execute(sql, new Object[0]);
                }
                start = i + 1;
            }
        }
    }

    /** Executes one statement. */
    public Result execute(String sql, Object[] params) {
        Parser p = new Parser(sql, params);
        Result r = p.statement();
        p.expectEnd();
        return r;
    }

    // ------------------------------------------------------------------ parser + executor
    private Map<String, Table> tables(String schemaName) {
        String s = schemaName != null ? schemaName : schema;
        if (s == null) {
            throw new SqlError("No database selected", 1046, "3D000", SqlError.SYNTAX);
        }
        Map<String, Table> t = SCHEMAS.get(s.toLowerCase());
        if (t == null) {
            throw new SqlError("Unknown database '" + s + "'", 1049, "42000", SqlError.SYNTAX);
        }
        return t;
    }

    private final class Parser {
        final String sql;
        final List<Lexer.Token> tokens;
        final Object[] params;
        int pos;
        int paramIndex;

        Parser(String sql, Object[] params) {
            this.sql = sql;
            this.tokens = Lexer.tokens(sql);
            this.params = params;
        }

        Lexer.Token peek() {
            return tokens.get(pos);
        }

        Lexer.Token next() {
            return tokens.get(pos++);
        }

        boolean accept(String word) {
            if (peek().is(word)) {
                pos++;
                return true;
            }
            return false;
        }

        SqlError error() {
            Lexer.Token t = peek();
            return SqlError.syntax(t.type == Lexer.END ? "" : sql.substring(t.start));
        }

        void expect(String word) {
            if (!accept(word)) {
                throw error();
            }
        }

        void expectEnd() {
            accept(";");
            if (peek().type != Lexer.END) {
                throw error();
            }
        }

        String name() {
            Lexer.Token t = next();
            if (t.type != Lexer.WORD && t.type != Lexer.QUOTED) {
                pos--;
                throw error();
            }
            return t.text;
        }

        /** [schema.]table */
        String[] qualifiedName() {
            String a = name();
            if (accept(".")) {
                return new String[] {a, name()};
            }
            return new String[] {null, a};
        }

        Table table(String[] qn) {
            Table t = tables(qn[0]).get(qn[1].toLowerCase());
            if (t == null) {
                throw SqlError.noTable(qn[0] != null ? qn[0] : schema, qn[1]);
            }
            return t;
        }

        Result statement() {
            Lexer.Token t = peek();
            if (t.is("SELECT")) {
                return select();
            } else if (t.is("INSERT") || t.is("REPLACE")) {
                return insert();
            } else if (t.is("UPDATE")) {
                return update();
            } else if (t.is("DELETE")) {
                return delete();
            } else if (t.is("CREATE")) {
                return create();
            } else if (t.is("DROP")) {
                return drop();
            } else if (t.is("USE")) {
                next();
                setSchema(name());
                return count(0);
            } else if (t.is("TRUNCATE")) {
                next();
                accept("TABLE");
                Table table = table(qualifiedName());
                beforeChange();
                table.rows = new ArrayList<>();
                table.autoIncrement = 1;
                return count(0);
            } else if (t.is("SHOW")) {
                return show();
            } else if (t.is("DESCRIBE") || t.is("DESC")) {
                next();
                return describe(table(qualifiedName()));
            } else if (t.is("START") || t.is("BEGIN") || t.is("COMMIT") || t.is("ROLLBACK")) {
                if (accept("COMMIT")) {
                    commit();
                } else if (accept("ROLLBACK")) {
                    rollback();
                } else {
                    next();
                    accept("TRANSACTION");
                }
                return count(0);
            }
            throw error();
        }

        Result count(long n) {
            Result r = new Result();
            r.updateCount = n;
            return r;
        }

        // ---------------------------------------------------------- DDL
        Result create() {
            expect("CREATE");
            if (accept("DATABASE") || accept("SCHEMA")) {
                boolean ifNotExists = ifNotExists();
                String n = name();
                skipRest();
                if (SCHEMAS.containsKey(n.toLowerCase())) {
                    if (!ifNotExists) {
                        throw new SqlError("Can't create database '" + n + "'; database exists", 1007, "HY000", SqlError.GENERAL);
                    }
                    return count(0);
                }
                SCHEMAS.put(n.toLowerCase(), new LinkedHashMap<>());
                return count(1);
            }
            accept("TEMPORARY");
            expect("TABLE");
            boolean ifNotExists = ifNotExists();
            String[] qn = qualifiedName();
            Map<String, Table> schemaTables = tables(qn[0]);
            Table table = new Table(qn[1]);
            expect("(");
            do {
                if (peek().is("PRIMARY")) {
                    next();
                    expect("KEY");
                    for (String c : nameList()) {
                        column(table, c).primaryKey = true;
                        column(table, c).notNull = true;
                    }
                } else if (peek().is("UNIQUE")) {
                    next();
                    if (!accept("KEY")) {
                        accept("INDEX");
                    }
                    if (peek().type == Lexer.WORD && !peek().is("(")) {
                        name();
                    }
                    for (String c : nameList()) {
                        column(table, c).unique = true;
                    }
                } else if (peek().is("CONSTRAINT") || peek().is("FOREIGN") || peek().is("KEY") || peek().is("INDEX")
                        || peek().is("CHECK")) {
                    skipDefinition();
                } else {
                    table.columns.add(columnDefinition());
                }
            } while (accept(","));
            expect(")");
            skipRest();
            if (schemaTables.containsKey(table.name.toLowerCase())) {
                if (ifNotExists) {
                    return count(0);
                }
                throw new SqlError("Table '" + table.name + "' already exists", 1050, "42S01", SqlError.SYNTAX);
            }
            beforeChange();
            schemaTables.put(table.name.toLowerCase(), table);
            return count(0);
        }

        boolean ifNotExists() {
            if (accept("IF")) {
                expect("NOT");
                expect("EXISTS");
                return true;
            }
            return false;
        }

        Table.Column column(Table table, String name) {
            int i = table.index(name);
            if (i < 0) {
                throw new SqlError("Key column '" + name + "' doesn't exist in table", 1072, "42000", SqlError.SYNTAX);
            }
            return table.columns.get(i);
        }

        List<String> nameList() {
            List<String> names = new ArrayList<>();
            expect("(");
            do {
                names.add(name());
                if (accept("(")) {          // prefix length, e.g. name(10)
                    next();
                    expect(")");
                }
                if (!accept("ASC")) {
                    accept("DESC");
                }
            } while (accept(","));
            expect(")");
            return names;
        }

        void skipDefinition() {
            int depth = 0;
            while (peek().type != Lexer.END) {
                if (peek().is("(")) {
                    depth++;
                } else if (peek().is(")")) {
                    if (depth == 0) {
                        return;
                    }
                    depth--;
                } else if (peek().is(",") && depth == 0) {
                    return;
                }
                next();
            }
        }

        void skipRest() {
            while (peek().type != Lexer.END && !peek().is(";")) {
                next();
            }
        }

        Table.Column columnDefinition() {
            Table.Column c = new Table.Column();
            c.name = name();
            String type = name().toUpperCase();
            if (type.equals("DOUBLE") && peek().is("PRECISION")) {
                next();
            }
            if (type.equals("CHARACTER") && accept("VARYING")) {
                type = "VARCHAR";
            }
            c.type = type;
            if (accept("(")) {
                c.length = (int) Values.toLong(literalNumber());
                if (accept(",")) {
                    c.scale = (int) Values.toLong(literalNumber());
                } else if (c.isDecimal()) {
                    c.scale = 0;
                }
                expect(")");
            } else if (c.isDecimal()) {
                c.scale = 0;
            }
            if (type.equals("SERIAL")) {
                c.autoIncrement = true;
                c.notNull = true;
                c.unique = true;
            }
            while (true) {
                if (accept("UNSIGNED") || accept("SIGNED") || accept("ZEROFILL")) {
                    continue;
                }
                if (accept("NOT")) {
                    expect("NULL");
                    c.notNull = true;
                } else if (accept("NULL")) {
                    c.notNull = false;
                } else if (accept("AUTO_INCREMENT") || accept("AUTOINCREMENT")) {
                    c.autoIncrement = true;
                } else if (accept("PRIMARY")) {
                    expect("KEY");
                    c.primaryKey = true;
                    c.notNull = true;
                } else if (accept("UNIQUE")) {
                    accept("KEY");
                    c.unique = true;
                } else if (accept("DEFAULT")) {
                    Expr e = primary();
                    c.defaultValue = e.eval(new Expr.Ctx());
                } else if (accept("CHARACTER")) {
                    expect("SET");
                    name();
                } else if (accept("CHARSET") || accept("COLLATE")) {
                    name();
                } else if (accept("COMMENT")) {
                    next();
                } else if (peek().is("CHECK") || peek().is("REFERENCES")) {
                    next();
                    if (peek().type == Lexer.WORD) {
                        qualifiedName();
                    }
                    if (peek().is("(")) {
                        next();
                        int depth = 1;
                        while (depth > 0 && peek().type != Lexer.END) {
                            if (peek().is("(")) {
                                depth++;
                            } else if (peek().is(")")) {
                                depth--;
                            }
                            next();
                        }
                    }
                    while (accept("ON")) {
                        next();
                        if (!accept("CASCADE") && !accept("RESTRICT")) {
                            if (accept("SET")) {
                                next();
                            } else {
                                accept("NO");
                                accept("ACTION");
                            }
                        }
                    }
                } else {
                    break;
                }
            }
            return c;
        }

        Object literalNumber() {
            Lexer.Token t = next();
            if (t.type != Lexer.NUMBER) {
                pos--;
                throw error();
            }
            return Values.number(t.text);
        }

        Result drop() {
            expect("DROP");
            if (accept("DATABASE") || accept("SCHEMA")) {
                boolean ifExists = accept("IF") && accept("EXISTS");
                String n = name();
                if (SCHEMAS.remove(n.toLowerCase()) == null && !ifExists) {
                    throw new SqlError("Can't drop database '" + n + "'; database doesn't exist", 1008, "HY000", SqlError.GENERAL);
                }
                if (schema != null && schema.equalsIgnoreCase(n)) {
                    schema = null;
                }
                return count(0);
            }
            accept("TEMPORARY");
            expect("TABLE");
            boolean ifExists = false;
            if (accept("IF")) {
                expect("EXISTS");
                ifExists = true;
            }
            do {
                String[] qn = qualifiedName();
                Map<String, Table> schemaTables = tables(qn[0]);
                if (!schemaTables.containsKey(qn[1].toLowerCase())) {
                    if (!ifExists) {
                        throw new SqlError("Unknown table '" + (qn[0] != null ? qn[0] : schema) + "." + qn[1] + "'",
                                1051, "42S02", SqlError.SYNTAX);
                    }
                } else {
                    beforeChange();
                    schemaTables.remove(qn[1].toLowerCase());
                }
            } while (accept(","));
            skipRest();
            return count(0);
        }

        Result show() {
            expect("SHOW");
            Result r = new Result();
            r.query = true;
            if (accept("TABLES")) {
                String label = "Tables_in_" + (schema == null ? "" : schema);
                header(r, new String[] {label}, new int[] {12});
                for (Table t : tables(null).values()) {
                    r.rows.add(new Object[] {t.name});
                }
            } else if (accept("DATABASES") || accept("SCHEMAS")) {
                header(r, new String[] {"Database"}, new int[] {12});
                for (String s : SCHEMAS.keySet()) {
                    r.rows.add(new Object[] {s});
                }
            } else {
                throw error();
            }
            return r;
        }

        Result describe(Table t) {
            Result r = new Result();
            r.query = true;
            header(r, new String[] {"Field", "Type", "Null", "Key", "Default", "Extra"}, new int[] {12, 12, 12, 12, 12, 12});
            for (Table.Column c : t.columns) {
                String type = c.type.toLowerCase() + (c.length >= 0 ? "(" + c.length + (c.scale >= 0 && c.isDecimal() ? "," + c.scale : "") + ")" : "");
                r.rows.add(new Object[] {c.name, type, c.notNull ? "NO" : "YES", c.primaryKey ? "PRI" : c.unique ? "UNI" : "",
                    Values.text(c.defaultValue), c.autoIncrement ? "auto_increment" : ""});
            }
            return r;
        }

        void header(Result r, String[] labels, int[] types) {
            r.labels = labels;
            r.names = labels;
            r.sqlTypes = types;
            r.typeNames = new String[labels.length];
            for (int i = 0; i < labels.length; i++) {
                r.typeNames[i] = "VARCHAR";
            }
        }

        // ---------------------------------------------------------- DML
        Result insert() {
            boolean replace = next().is("REPLACE");
            boolean ignore = accept("IGNORE");
            accept("INTO");
            Table table = table(qualifiedName());
            List<Integer> cols = new ArrayList<>();
            if (peek().is("(")) {
                for (String c : nameList()) {
                    int i = table.index(c);
                    if (i < 0) {
                        throw SqlError.unknownColumn(c, "field list");
                    }
                    cols.add(i);
                }
            } else {
                for (int i = 0; i < table.columns.size(); i++) {
                    cols.add(i);
                }
            }
            List<Object[]> newRows = new ArrayList<>();
            if (accept("SET")) {
                cols.clear();
                List<Object> values = new ArrayList<>();
                do {
                    String c = name();
                    int i = table.index(c);
                    if (i < 0) {
                        throw SqlError.unknownColumn(c, "field list");
                    }
                    expect("=");
                    cols.add(i);
                    values.add(expression().eval(ctx(null)));
                } while (accept(","));
                newRows.add(values.toArray());
            } else {
                if (!accept("VALUES")) {
                    expect("VALUE");
                }
                do {
                    expect("(");
                    List<Object> values = new ArrayList<>();
                    if (!peek().is(")")) {
                        do {
                            if (accept("DEFAULT")) {
                                values.add(DEFAULT);
                            } else {
                                values.add(expression().eval(ctx(null)));
                            }
                        } while (accept(","));
                    }
                    expect(")");
                    if (values.size() != cols.size()) {
                        throw new SqlError("Column count doesn't match value count at row " + (newRows.size() + 1),
                                1136, "21S01", SqlError.GENERAL);
                    }
                    newRows.add(values.toArray());
                } while (accept(","));
            }
            beforeChange();
            Result r = count(0);
            for (Object[] values : newRows) {
                Object[] row = new Object[table.columns.size()];
                boolean[] given = new boolean[row.length];
                for (int k = 0; k < cols.size(); k++) {
                    if (values[k] != DEFAULT) {
                        row[cols.get(k)] = values[k];
                        given[cols.get(k)] = true;
                    }
                }
                Long key = null;
                for (int i = 0; i < row.length; i++) {
                    Table.Column c = table.columns.get(i);
                    if (!given[i]) {
                        row[i] = c.defaultValue;
                    }
                    if (c.autoIncrement) {
                        if (row[i] == null || Values.toLong(row[i]) == 0) {
                            row[i] = table.autoIncrement;
                        }
                        long v = Values.toLong(row[i]);
                        table.autoIncrement = Math.max(table.autoIncrement, v + 1);
                        key = v;
                    }
                    row[i] = c.store(row[i], table.name);
                    if (row[i] == null && c.notNull) {
                        if (given[i]) {
                            throw new SqlError("Column '" + c.name + "' cannot be null", 1048, "23000", SqlError.INTEGRITY);
                        }
                        throw new SqlError("Field '" + c.name + "' doesn't have a default value", 1364, "HY000", SqlError.GENERAL);
                    }
                }
                int clash = duplicate(table, row, -1);
                if (clash >= 0) {
                    if (replace) {
                        table.rows.remove(clash);
                        r.updateCount++;
                    } else if (ignore) {
                        continue;
                    } else {
                        throw duplicateError(table, row);
                    }
                }
                table.rows.add(row);
                r.updateCount++;
                if (key != null) {
                    r.generatedKeys.add(key);
                }
            }
            return r;
        }

        /** Index of a row that has the same primary key or unique value, or -1. */
        int duplicate(Table table, Object[] row, int skip) {
            for (int r = 0; r < table.rows.size(); r++) {
                if (r == skip) {
                    continue;
                }
                Object[] other = table.rows.get(r);
                boolean pkAll = false;
                boolean pkSame = true;
                for (int i = 0; i < row.length; i++) {
                    Table.Column c = table.columns.get(i);
                    if (c.primaryKey) {
                        pkAll = true;
                        if (row[i] == null || Values.compare(row[i], other[i]) != 0) {
                            pkSame = false;
                        }
                    }
                    if ((c.unique || (c.autoIncrement && !c.primaryKey)) && row[i] != null
                            && Values.compare(row[i], other[i]) != null && Values.compare(row[i], other[i]) == 0) {
                        lastKey = c.name;
                        lastValue = row[i];
                        return r;
                    }
                }
                if (pkAll && pkSame) {
                    StringBuilder v = new StringBuilder();
                    for (int i = 0; i < row.length; i++) {
                        if (table.columns.get(i).primaryKey) {
                            if (v.length() > 0) {
                                v.append('-');
                            }
                            v.append(Values.text(row[i]));
                        }
                    }
                    lastKey = "PRIMARY";
                    lastValue = v.toString();
                    return r;
                }
            }
            return -1;
        }

        String lastKey;
        Object lastValue;

        SqlError duplicateError(Table table, Object[] row) {
            return new SqlError("Duplicate entry '" + Values.text(lastValue) + "' for key '" + table.name.toLowerCase() + "."
                    + lastKey + "'", 1062, "23000", SqlError.INTEGRITY);
        }

        Result update() {
            expect("UPDATE");
            Table table = table(qualifiedName());
            if (peek().type == Lexer.WORD && !peek().is("SET")) {
                accept("AS");
                name();
            }
            expect("SET");
            List<Integer> cols = new ArrayList<>();
            List<Expr> values = new ArrayList<>();
            do {
                String c = name();
                if (accept(".")) {
                    c = name();
                }
                int i = table.index(c);
                if (i < 0) {
                    throw SqlError.unknownColumn(c, "field list");
                }
                expect("=");
                Expr e = expression();
                e.bind(table, "field list");
                cols.add(i);
                values.add(e);
            } while (accept(","));
            Expr where = where(table);
            long limit = limitOnly();
            beforeChange();
            long matched = 0;
            for (int r = 0; r < table.rows.size() && matched < limit; r++) {
                Object[] row = table.rows.get(r);
                Expr.Ctx ctx = ctx(row);
                if (where != null && !Boolean.TRUE.equals(Values.truth(where.eval(ctx)))) {
                    continue;
                }
                matched++;
                Object[] updated = row.clone();
                for (int k = 0; k < cols.size(); k++) {
                    Table.Column c = table.columns.get(cols.get(k));
                    Object v = c.store(values.get(k).eval(ctx), table.name);
                    if (v == null && c.notNull) {
                        throw new SqlError("Column '" + c.name + "' cannot be null", 1048, "23000", SqlError.INTEGRITY);
                    }
                    updated[cols.get(k)] = v;
                }
                if (duplicate(table, updated, r) >= 0) {
                    throw duplicateError(table, updated);
                }
                table.rows.set(r, updated);
            }
            return count(matched);     // MySQL Connector/J reports the rows found, changed or not
        }

        Result delete() {
            expect("DELETE");
            expect("FROM");
            Table table = table(qualifiedName());
            Expr where = where(table);
            long limit = limitOnly();
            beforeChange();
            List<Object[]> keep = new ArrayList<>();
            long deleted = 0;
            for (Object[] row : table.rows) {
                if (deleted < limit && (where == null || Boolean.TRUE.equals(Values.truth(where.eval(ctx(row)))))) {
                    deleted++;
                } else {
                    keep.add(row);
                }
            }
            table.rows = keep;
            return count(deleted);
        }

        Expr where(Table table) {
            if (accept("WHERE")) {
                Expr e = expression();
                e.bind(table, "where clause");
                if (e.hasAggregate()) {
                    throw new SqlError("Invalid use of group function", 1111, "HY000", SqlError.GENERAL);
                }
                return e;
            }
            return null;
        }

        long limitOnly() {
            if (accept("ORDER")) {
                expect("BY");
                do {
                    expression();
                    if (!accept("ASC")) {
                        accept("DESC");
                    }
                } while (accept(","));
            }
            if (accept("LIMIT")) {
                return Values.toLong(expression().eval(ctx(null)));
            }
            return Long.MAX_VALUE;
        }

        Expr.Ctx ctx(Object[] row) {
            Expr.Ctx c = new Expr.Ctx();
            c.row = row;
            c.params = params;
            return c;
        }

        // ---------------------------------------------------------- SELECT
        Result select() {
            expect("SELECT");
            boolean distinct = accept("DISTINCT");
            accept("ALL");
            List<Expr> items = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            boolean star = false;
            List<Integer> starAt = new ArrayList<>();
            do {
                if (accept("*")) {
                    star = true;
                    starAt.add(items.size());
                    items.add(null);
                    labels.add(null);
                    continue;
                }
                if (peek().type == Lexer.WORD && tokens.get(pos + 1).is(".") && tokens.get(pos + 2).is("*")) {
                    pos += 3;
                    star = true;
                    starAt.add(items.size());
                    items.add(null);
                    labels.add(null);
                    continue;
                }
                int startTok = pos;
                Expr e = expression();
                String label = sql.substring(tokens.get(startTok).start, tokens.get(pos - 1).end);
                if (e instanceof Expr.Column) {
                    label = ((Expr.Column) e).name;
                }
                if (accept("AS")) {
                    label = aliasName();
                } else if (peek().type == Lexer.WORD && !isClauseWord(peek()) || peek().type == Lexer.QUOTED
                        || peek().type == Lexer.STRING) {
                    label = aliasName();
                }
                items.add(e);
                labels.add(label);
            } while (accept(","));

            Table table = null;
            if (accept("FROM")) {
                if (peek().is("DUAL")) {
                    next();
                } else {
                    table = table(qualifiedName());
                    if (accept("AS")) {
                        name();
                    } else if (peek().type == Lexer.WORD && !isClauseWord(peek())) {
                        name();
                    }
                    if (peek().is("JOIN") || peek().is("INNER") || peek().is("LEFT") || peek().is("RIGHT")
                            || peek().is("CROSS") || peek().is(",")) {
                        throw new SqlError("This browser database runs queries on one table at a time (no JOIN). "
                                + "Run the query on the real JDK and MySQL for joins.", 1235, "42000", SqlError.GENERAL);
                    }
                }
            }
            // expand *
            if (star) {
                if (table == null) {
                    throw new SqlError("No tables used", 1096, "HY000", SqlError.GENERAL);
                }
                List<Expr> expanded = new ArrayList<>();
                List<String> expandedLabels = new ArrayList<>();
                for (int k = 0; k < items.size(); k++) {
                    if (items.get(k) == null) {
                        for (Table.Column c : table.columns) {
                            Expr.Column ref = new Expr.Column(null, c.name);
                            expanded.add(ref);
                            expandedLabels.add(c.name);
                        }
                    } else {
                        expanded.add(items.get(k));
                        expandedLabels.add(labels.get(k));
                    }
                }
                items = expanded;
                labels = expandedLabels;
            }
            for (Expr e : items) {
                if (table != null) {
                    e.bind(table, "field list");
                } else {
                    e.bind(null, "field list");
                }
            }
            Expr where = table == null ? null : where(table);
            List<Expr> groupBy = new ArrayList<>();
            if (accept("GROUP")) {
                expect("BY");
                do {
                    Expr g = expression();
                    groupBy.add(resolveAlias(g, items, labels, table, "group statement"));
                } while (accept(","));
            }
            Expr having = null;
            if (accept("HAVING")) {
                having = resolveAlias(expression(), items, labels, table, "having clause");
            }
            List<Expr> orderBy = new ArrayList<>();
            List<Boolean> descending = new ArrayList<>();
            if (accept("ORDER")) {
                expect("BY");
                do {
                    Expr o = expression();
                    if (o instanceof Expr.Literal && ((Expr.Literal) o).value instanceof Long) {
                        long position = (Long) ((Expr.Literal) o).value;
                        if (position < 1 || position > items.size()) {
                            throw new SqlError("Unknown column '" + position + "' in 'order clause'", 1054, "42S22", SqlError.SYNTAX);
                        }
                        o = items.get((int) position - 1);
                    } else {
                        o = resolveAlias(o, items, labels, table, "order clause");
                    }
                    orderBy.add(o);
                    descending.add(accept("DESC") ? Boolean.TRUE : (accept("ASC") ? Boolean.FALSE : Boolean.FALSE));
                } while (accept(","));
            }
            long offset = 0;
            long limit = Long.MAX_VALUE;
            if (accept("LIMIT")) {
                long a = Values.toLong(expression().eval(ctx(null)));
                if (accept(",")) {
                    offset = a;
                    limit = Values.toLong(expression().eval(ctx(null)));
                } else {
                    limit = a;
                    if (accept("OFFSET")) {
                        offset = Values.toLong(expression().eval(ctx(null)));
                    }
                }
            }

            // evaluate
            List<Expr.Ctx> contexts = new ArrayList<>();
            boolean aggregate = !groupBy.isEmpty();
            for (Expr e : items) {
                aggregate |= e.hasAggregate();
            }
            if (having != null) {
                aggregate = true;
            }
            List<Object[]> source = new ArrayList<>();
            if (table == null) {
                source.add(new Object[0]);
            } else {
                for (Object[] row : table.rows) {
                    if (where == null || Boolean.TRUE.equals(Values.truth(where.eval(ctx(row))))) {
                        source.add(row);
                    }
                }
            }
            if (aggregate) {
                Map<String, List<Object[]>> groups = new LinkedHashMap<>();
                for (Object[] row : source) {
                    StringBuilder key = new StringBuilder();
                    for (Expr g : groupBy) {
                        key.append(Values.key(g.eval(ctx(row)))).append('\u0001');
                    }
                    groups.computeIfAbsent(key.toString(), k -> new ArrayList<>()).add(row);
                }
                if (groups.isEmpty() && groupBy.isEmpty()) {
                    groups.put("", new ArrayList<>());
                }
                for (List<Object[]> g : groups.values()) {
                    Expr.Ctx c = ctx(null);
                    c.group = g;
                    if (having == null || Boolean.TRUE.equals(Values.truth(having.eval(c)))) {
                        contexts.add(c);
                    }
                }
            } else {
                for (Object[] row : source) {
                    contexts.add(ctx(row));
                }
            }
            if (!orderBy.isEmpty()) {
                List<Object[]> keys = new ArrayList<>();
                Map<Expr.Ctx, Object[]> keyOf = new HashMap<>();
                for (Expr.Ctx c : contexts) {
                    Object[] k = new Object[orderBy.size()];
                    for (int i = 0; i < k.length; i++) {
                        k[i] = orderBy.get(i).eval(c);
                    }
                    keyOf.put(c, k);
                    keys.add(k);
                }
                final List<Boolean> desc = descending;
                Collections.sort(contexts, (x, y) -> {
                    Object[] a = keyOf.get(x);
                    Object[] b = keyOf.get(y);
                    for (int i = 0; i < a.length; i++) {
                        int c = Values.order(a[i], b[i]);
                        if (c != 0) {
                            return desc.get(i) ? -c : c;
                        }
                    }
                    return 0;
                });
            }
            Result r = new Result();
            r.query = true;
            r.table = table == null ? "" : table.name;
            r.labels = labels.toArray(new String[0]);
            r.names = new String[items.size()];
            r.sqlTypes = new int[items.size()];
            r.typeNames = new String[items.size()];
            r.scales = new int[items.size()];
            for (int i = 0; i < items.size(); i++) {
                Expr e = items.get(i);
                r.scales[i] = -1;
                if (e instanceof Expr.Column && table != null) {
                    Table.Column c = table.columns.get(((Expr.Column) e).index);
                    r.names[i] = c.name;
                    r.sqlTypes[i] = c.sqlType();
                    r.typeNames[i] = c.type;
                    r.scales[i] = c.isDecimal() ? Math.max(0, c.scale) : -1;
                } else {
                    r.names[i] = r.labels[i];
                    r.sqlTypes[i] = 0;          // decided from the values below
                    r.typeNames[i] = "";
                }
            }
            Set<String> seen = new HashSet<>();
            long skipped = 0;
            for (Expr.Ctx c : contexts) {
                Object[] out = new Object[items.size()];
                for (int i = 0; i < out.length; i++) {
                    out[i] = items.get(i).eval(c);
                }
                if (distinct) {
                    StringBuilder k = new StringBuilder();
                    for (Object v : out) {
                        k.append(Values.key(v)).append('\u0001');
                    }
                    if (!seen.add(k.toString())) {
                        continue;
                    }
                }
                if (skipped < offset) {
                    skipped++;
                    continue;
                }
                if (r.rows.size() >= limit) {
                    break;
                }
                r.rows.add(out);
            }
            for (int i = 0; i < items.size(); i++) {
                if (r.sqlTypes[i] == 0) {
                    int type = 12;
                    String name = "VARCHAR";
                    for (Object[] row : r.rows) {
                        if (row[i] instanceof Long) {
                            type = -5;
                            name = "BIGINT";
                            break;
                        } else if (row[i] instanceof Double) {
                            type = 8;
                            name = "DOUBLE";
                            break;
                        } else if (row[i] != null) {
                            break;
                        }
                    }
                    r.sqlTypes[i] = type;
                    r.typeNames[i] = name;
                }
            }
            return r;
        }

        String aliasName() {
            Lexer.Token t = next();
            if (t.type != Lexer.WORD && t.type != Lexer.QUOTED && t.type != Lexer.STRING) {
                pos--;
                throw error();
            }
            return t.text;
        }

        boolean isClauseWord(Lexer.Token t) {
            for (String w : new String[] {"FROM", "WHERE", "GROUP", "HAVING", "ORDER", "LIMIT", "JOIN", "INNER", "LEFT",
                "RIGHT", "CROSS", "ON", "UNION", "OFFSET", "INTO", "FOR", "SET"}) {
                if (t.is(w)) {
                    return true;
                }
            }
            return false;
        }

        /** Column alias of the select list, or the expression bound to the table. */
        Expr resolveAlias(Expr e, List<Expr> items, List<String> labels, Table table, String clause) {
            if (e instanceof Expr.Column && ((Expr.Column) e).qualifier == null) {
                String n = ((Expr.Column) e).name;
                if (table == null || table.index(n) < 0) {
                    for (int i = 0; i < labels.size(); i++) {
                        if (n.equalsIgnoreCase(labels.get(i))) {
                            return items.get(i);
                        }
                    }
                }
            }
            e.bind(table, clause);
            return e;
        }

        // ---------------------------------------------------------- expressions
        Expr expression() {
            return or();
        }

        Expr or() {
            Expr e = and();
            while (accept("OR") || accept("||")) {
                e = new Expr.Binary("OR", e, and());
            }
            return e;
        }

        Expr and() {
            Expr e = not();
            while (accept("AND") || accept("&&")) {
                e = new Expr.Binary("AND", e, not());
            }
            return e;
        }

        Expr not() {
            if (accept("NOT") || accept("!")) {
                return new Expr.Unary("NOT", not());
            }
            return comparison();
        }

        Expr comparison() {
            Expr e = additive();
            while (true) {
                Lexer.Token t = peek();
                if (t.is("=") || t.is("<>") || t.is("!=") || t.is("<") || t.is("<=") || t.is(">") || t.is(">=")) {
                    next();
                    String op = t.text.equals("!=") ? "<>" : t.text;
                    if (op.equals("<=") && peek().is(">")) {
                        next();
                        op = "<=>";
                    }
                    e = new Expr.Binary(op, e, additive());
                } else if (t.is("IS")) {
                    next();
                    boolean not = accept("NOT");
                    if (accept("NULL")) {
                        e = new Expr.IsNull(e, not);
                    } else if (accept("TRUE")) {
                        Expr test = new Expr.Binary("<>", e, new Expr.Literal(0L));
                        e = not ? new Expr.Unary("NOT", test) : test;
                    } else if (accept("FALSE")) {
                        Expr test = new Expr.Binary("=", e, new Expr.Literal(0L));
                        e = not ? new Expr.Unary("NOT", test) : test;
                    } else {
                        throw error();
                    }
                } else if (t.is("NOT") && (tokens.get(pos + 1).is("LIKE") || tokens.get(pos + 1).is("IN")
                        || tokens.get(pos + 1).is("BETWEEN"))) {
                    next();
                    e = suffix(e, true);
                } else if (t.is("LIKE") || t.is("IN") || t.is("BETWEEN")) {
                    e = suffix(e, false);
                } else {
                    return e;
                }
            }
        }

        Expr suffix(Expr e, boolean not) {
            if (accept("LIKE")) {
                return new Expr.Like(e, additive(), not);
            }
            if (accept("IN")) {
                expect("(");
                List<Expr> list = new ArrayList<>();
                do {
                    list.add(expression());
                } while (accept(","));
                expect(")");
                return new Expr.In(e, list, not);
            }
            expect("BETWEEN");
            Expr low = additive();
            expect("AND");
            return new Expr.Between(e, low, additive(), not);
        }

        Expr additive() {
            Expr e = multiplicative();
            while (peek().is("+") || peek().is("-")) {
                String op = next().text;
                e = new Expr.Binary(op, e, multiplicative());
            }
            return e;
        }

        Expr multiplicative() {
            Expr e = unary();
            while (peek().is("*") || peek().is("/") || peek().is("%") || peek().is("DIV") || peek().is("MOD")) {
                String op = next().text.toUpperCase();
                e = new Expr.Binary(op.equals("MOD") ? "%" : op, e, unary());
            }
            return e;
        }

        Expr unary() {
            if (accept("-")) {
                return new Expr.Unary("-", unary());
            }
            if (accept("+")) {
                return unary();
            }
            return primary();
        }

        Expr primary() {
            Lexer.Token t = next();
            switch (t.type) {
                case Lexer.NUMBER:
                    return new Expr.Literal(Values.number(t.text));
                case Lexer.STRING:
                    return new Expr.Literal(t.text);
                case Lexer.PARAM: {
                    int index = paramIndex++;
                    if (index >= params.length) {
                        throw SqlError.general("No value specified for parameter " + (index + 1));
                    }
                    if (params[index] == UNSET) {
                        throw new SqlError("No value specified for parameter " + (index + 1), 0, "07001", SqlError.GENERAL);
                    }
                    return new Expr.Param(index);
                }
                case Lexer.SYMBOL:
                    if (t.text.equals("(")) {
                        Expr e = expression();
                        expect(")");
                        return e;
                    }
                    pos--;
                    throw error();
                case Lexer.QUOTED:
                    return columnRef(t.text);
                case Lexer.WORD: {
                    String w = t.text.toUpperCase();
                    if (w.equals("NULL")) {
                        return new Expr.Literal(null);
                    }
                    if (w.equals("TRUE")) {
                        return new Expr.Literal(1L);
                    }
                    if (w.equals("FALSE")) {
                        return new Expr.Literal(0L);
                    }
                    if (peek().is("(")) {
                        next();
                        if (w.equals("COUNT") || w.equals("SUM") || w.equals("AVG") || w.equals("MIN") || w.equals("MAX")) {
                            boolean distinct = accept("DISTINCT");
                            Expr arg = null;
                            if (w.equals("COUNT") && accept("*")) {
                                arg = null;
                            } else {
                                arg = expression();
                            }
                            expect(")");
                            return new Expr.Aggregate(w, arg, distinct);
                        }
                        List<Expr> args = new ArrayList<>();
                        if (!peek().is(")")) {
                            do {
                                args.add(expression());
                            } while (accept(","));
                        }
                        expect(")");
                        return new Expr.Function(w, args);
                    }
                    return columnRef(t.text);
                }
                default:
                    pos--;
                    throw error();
            }
        }

        Expr columnRef(String first) {
            if (accept(".")) {
                return new Expr.Column(first, name());
            }
            return new Expr.Column(null, first);
        }
    }

    /** Marker for a parameter that has not been set, and for DEFAULT in VALUES. */
    public static final Object UNSET = new Object();
    static final Object DEFAULT = new Object();
}
