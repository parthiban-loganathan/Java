package civiltech.io;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small, strict JSON parser (RFC 8259) producing Map, List, String, Double, Boolean or null.
 * Written without libraries so it runs unchanged on the JVM, on Android and in the browser.
 */
public final class MiniJson {

    private final String s;
    private int p;

    private MiniJson(String s) { this.s = s; }

    public static Object parse(String text) {
        MiniJson j = new MiniJson(text);
        j.ws();
        Object v = j.value();
        j.ws();
        if (j.p != text.length()) {
            throw j.error("unexpected trailing text");
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> object(Object o) { return (Map<String, Object>) o; }

    @SuppressWarnings("unchecked")
    public static List<Object> list(Object o) { return (List<Object>) o; }

    public static double[] doubles(Object o) {
        List<Object> l = list(o);
        double[] a = new double[l.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = ((Number) l.get(i)).doubleValue();
        }
        return a;
    }

    public static int[] ints(Object o) {
        List<Object> l = list(o);
        int[] a = new int[l.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = (int) Math.round(((Number) l.get(i)).doubleValue());
        }
        return a;
    }

    private Object value() {
        if (p >= s.length()) throw error("unexpected end");
        char c = s.charAt(p);
        switch (c) {
            case '{': return obj();
            case '[': return arr();
            case '"': return str();
            case 't': lit("true"); return Boolean.TRUE;
            case 'f': lit("false"); return Boolean.FALSE;
            case 'n': lit("null"); return null;
            default: return num();
        }
    }

    private Map<String, Object> obj() {
        Map<String, Object> m = new LinkedHashMap<>();
        p++;
        ws();
        if (peek('}')) { p++; return m; }
        while (true) {
            ws();
            if (!peek('"')) throw error("expected string key");
            String k = str();
            ws();
            expect(':');
            ws();
            m.put(k, value());
            ws();
            if (peek(',')) { p++; continue; }
            expect('}');
            return m;
        }
    }

    private List<Object> arr() {
        List<Object> l = new ArrayList<>();
        p++;
        ws();
        if (peek(']')) { p++; return l; }
        while (true) {
            ws();
            l.add(value());
            ws();
            if (peek(',')) { p++; continue; }
            expect(']');
            return l;
        }
    }

    private String str() {
        expect('"');
        StringBuilder b = new StringBuilder();
        while (true) {
            if (p >= s.length()) throw error("unterminated string");
            char c = s.charAt(p++);
            if (c == '"') return b.toString();
            if (c == '\\') {
                char e = s.charAt(p++);
                switch (e) {
                    case '"': b.append('"'); break;
                    case '\\': b.append('\\'); break;
                    case '/': b.append('/'); break;
                    case 'b': b.append('\b'); break;
                    case 'f': b.append('\f'); break;
                    case 'n': b.append('\n'); break;
                    case 'r': b.append('\r'); break;
                    case 't': b.append('\t'); break;
                    case 'u': b.append((char) Integer.parseInt(s.substring(p, p + 4), 16)); p += 4; break;
                    default: throw error("bad escape");
                }
            } else {
                b.append(c);
            }
        }
    }

    private Double num() {
        int start = p;
        if (peek('-')) p++;
        while (p < s.length() && "0123456789+-.eE".indexOf(s.charAt(p)) >= 0) p++;
        if (start == p) throw error("unexpected character '" + s.charAt(p) + "'");
        try {
            return Double.valueOf(s.substring(start, p));
        } catch (NumberFormatException e) {
            throw error("bad number");
        }
    }

    private void lit(String w) {
        if (!s.startsWith(w, p)) throw error("expected " + w);
        p += w.length();
    }

    private boolean peek(char c) { return p < s.length() && s.charAt(p) == c; }

    private void expect(char c) {
        if (!peek(c)) throw error("expected '" + c + "'");
        p++;
    }

    private void ws() {
        while (p < s.length() && Character.isWhitespace(s.charAt(p))) p++;
    }

    private IllegalArgumentException error(String msg) {
        return new IllegalArgumentException("JSON error at position " + p + ": " + msg);
    }
}
