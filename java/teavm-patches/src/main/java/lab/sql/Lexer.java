package lab.sql;

import java.util.ArrayList;
import java.util.List;

/** Splits SQL text into tokens. */
final class Lexer {
    static final int WORD = 0;      // identifier or keyword (text as written; quoted identifiers unquoted)
    static final int QUOTED = 1;    // `quoted identifier`
    static final int NUMBER = 2;
    static final int STRING = 3;
    static final int PARAM = 4;     // ?
    static final int SYMBOL = 5;    // operators and punctuation
    static final int END = 6;

    static final class Token {
        final int type;
        final String text;
        final int start;
        final int end;

        Token(int type, String text, int start, int end) {
            this.type = type;
            this.text = text;
            this.start = start;
            this.end = end;
        }

        boolean is(String word) {
            return (type == WORD || type == SYMBOL) && text.equalsIgnoreCase(word);
        }

        @Override
        public String toString() {
            return text;
        }
    }

    private Lexer() {
    }

    static List<Token> tokens(String sql) {
        List<Token> out = new ArrayList<>();
        int i = 0;
        int n = sql.length();
        while (i < n) {
            char c = sql.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '-' && i + 1 < n && sql.charAt(i + 1) == '-'
                    && (i + 2 >= n || Character.isWhitespace(sql.charAt(i + 2)))) {
                while (i < n && sql.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '#') {
                while (i < n && sql.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && i + 1 < n && sql.charAt(i + 1) == '*') {
                int e = sql.indexOf("*/", i + 2);
                i = e < 0 ? n : e + 2;
            } else if (c == '\'' || c == '"') {
                StringBuilder sb = new StringBuilder();
                int start = i++;
                boolean closed = false;
                while (i < n) {
                    char d = sql.charAt(i);
                    if (d == '\\' && i + 1 < n) {
                        char e = sql.charAt(i + 1);
                        sb.append(e == 'n' ? '\n' : e == 't' ? '\t' : e == '0' ? '\0' : e);
                        i += 2;
                    } else if (d == c) {
                        if (i + 1 < n && sql.charAt(i + 1) == c) {
                            sb.append(c);
                            i += 2;
                        } else {
                            i++;
                            closed = true;
                            break;
                        }
                    } else {
                        sb.append(d);
                        i++;
                    }
                }
                if (!closed) {
                    throw SqlError.syntax(sql.substring(start));
                }
                out.add(new Token(STRING, sb.toString(), start, i));
            } else if (c == '`') {
                int e = sql.indexOf('`', i + 1);
                if (e < 0) {
                    throw SqlError.syntax(sql.substring(i));
                }
                out.add(new Token(QUOTED, sql.substring(i + 1, e), i, e + 1));
                i = e + 1;
            } else if (Character.isDigit(c) || (c == '.' && i + 1 < n && Character.isDigit(sql.charAt(i + 1)))) {
                int start = i;
                while (i < n && (Character.isDigit(sql.charAt(i)) || sql.charAt(i) == '.')) {
                    i++;
                }
                if (i < n && (sql.charAt(i) == 'e' || sql.charAt(i) == 'E')) {
                    int j = i + 1;
                    if (j < n && (sql.charAt(j) == '+' || sql.charAt(j) == '-')) {
                        j++;
                    }
                    if (j < n && Character.isDigit(sql.charAt(j))) {
                        i = j;
                        while (i < n && Character.isDigit(sql.charAt(i))) {
                            i++;
                        }
                    }
                }
                out.add(new Token(NUMBER, sql.substring(start, i), start, i));
            } else if (Character.isLetter(c) || c == '_' || c == '$') {
                int start = i;
                while (i < n && (Character.isLetterOrDigit(sql.charAt(i)) || sql.charAt(i) == '_' || sql.charAt(i) == '$')) {
                    i++;
                }
                out.add(new Token(WORD, sql.substring(start, i), start, i));
            } else if (c == '?') {
                out.add(new Token(PARAM, "?", i, i + 1));
                i++;
            } else {
                String two = i + 1 < n ? sql.substring(i, i + 2) : "";
                if (two.equals("<=") || two.equals(">=") || two.equals("<>") || two.equals("!=") || two.equals("||")
                        || two.equals("&&")) {
                    out.add(new Token(SYMBOL, two, i, i + 2));
                    i += 2;
                } else if ("=<>+-*/%(),;.!".indexOf(c) >= 0) {
                    out.add(new Token(SYMBOL, String.valueOf(c), i, i + 1));
                    i++;
                } else {
                    throw SqlError.syntax(sql.substring(i));
                }
            }
        }
        out.add(new Token(END, "", n, n));
        return out;
    }
}
