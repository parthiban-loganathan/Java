package org.teavm.classlib.java.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import lab.web.BrowserStdin;

/**
 * java.util.Scanner for the TeaVM browser build.
 *
 * TeaVM's class library has no Scanner. TeaVM substitutes a class named
 * org.teavm.classlib.java.util.TScanner for java.util.Scanner, so course programs keep their
 * normal {@code import java.util.Scanner;}. Behaviour follows the JDK for the methods used in the
 * course: whitespace-delimited tokens, nextLine() returning the rest of the current line,
 * InputMismatchException without consuming the token, NoSuchElementException at the end.
 * Numbers are parsed in the ROOT/en-US format (no digit grouping), as typed in the lab.
 * {@code new Scanner(System.in)} reads the text given to the page's Input box.
 */
public final class TScanner implements Closeable {
    private final String src;
    private final boolean stdin;
    private int pos;
    private boolean closed;

    public TScanner(InputStream in) {
        if (in == System.in) {
            stdin = true;
            src = BrowserStdin.text();
            pos = BrowserStdin.position();
        } else {
            stdin = false;
            src = readAll(new InputStreamReader(in));
        }
    }

    public TScanner(File file) throws FileNotFoundException {
        this(new FileInputStream(file));
    }

    public TScanner(String text) {
        stdin = false;
        src = text;
    }

    private static String readAll(Reader r) {
        StringBuilder sb = new StringBuilder();
        char[] b = new char[4096];
        int n;
        try {
            while ((n = r.read(b)) > 0) {
                sb.append(b, 0, n);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sb.toString();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Scanner closed");
        }
    }

    private void moveTo(int p) {
        pos = p;
        if (stdin) {
            BrowserStdin.advance(p);
        }
    }

    private int tokenStart() {
        int p = pos;
        while (p < src.length() && Character.isWhitespace(src.charAt(p))) p++;
        return p;
    }

    private int tokenEnd(int s) {
        int p = s;
        while (p < src.length() && !Character.isWhitespace(src.charAt(p))) p++;
        return p;
    }

    private String peek() {
        int s = tokenStart();
        return s >= src.length() ? null : src.substring(s, tokenEnd(s));
    }

    public boolean hasNext() {
        ensureOpen();
        return peek() != null;
    }

    public String next() {
        ensureOpen();
        int s = tokenStart();
        if (s >= src.length()) throw new NoSuchElementException();
        int e = tokenEnd(s);
        moveTo(e);
        return src.substring(s, e);
    }

    public boolean hasNextLine() {
        ensureOpen();
        return pos < src.length();
    }

    public String nextLine() {
        ensureOpen();
        if (pos >= src.length()) throw new NoSuchElementException("No line found");
        int e = pos;
        while (e < src.length() && src.charAt(e) != '\n' && src.charAt(e) != '\r') e++;
        String line = src.substring(pos, e);
        if (e < src.length()) {
            e += (src.charAt(e) == '\r' && e + 1 < src.length() && src.charAt(e + 1) == '\n') ? 2 : 1;
        }
        moveTo(e);
        return line;
    }

    private static boolean isInteger(String t) {
        if (t == null || t.isEmpty()) return false;
        int i = (t.charAt(0) == '-' || t.charAt(0) == '+') ? 1 : 0;
        if (i == t.length()) return false;
        for (; i < t.length(); i++) {
            if (!Character.isDigit(t.charAt(i))) return false;
        }
        return true;
    }

    private static boolean isDecimal(String t) {
        if (t == null || t.isEmpty()) return false;
        String u = (t.charAt(0) == '-' || t.charAt(0) == '+') ? t.substring(1) : t;
        if (u.equals("NaN") || u.equals("Infinity")) return true;
        int i = 0, digits = 0;
        while (i < u.length() && Character.isDigit(u.charAt(i))) { i++; digits++; }
        if (i < u.length() && u.charAt(i) == '.') {
            i++;
            while (i < u.length() && Character.isDigit(u.charAt(i))) { i++; digits++; }
        }
        if (digits == 0) return false;
        if (i < u.length() && (u.charAt(i) == 'e' || u.charAt(i) == 'E')) {
            i++;
            if (i < u.length() && (u.charAt(i) == '+' || u.charAt(i) == '-')) i++;
            int exp = 0;
            while (i < u.length() && Character.isDigit(u.charAt(i))) { i++; exp++; }
            if (exp == 0) return false;
        }
        return i == u.length();
    }

    public boolean hasNextInt() {
        ensureOpen();
        String t = peek();
        if (!isInteger(t)) return false;
        try { Integer.parseInt(t); return true; } catch (NumberFormatException e) { return false; }
    }

    public boolean hasNextLong() {
        ensureOpen();
        String t = peek();
        if (!isInteger(t)) return false;
        try { Long.parseLong(t); return true; } catch (NumberFormatException e) { return false; }
    }

    public boolean hasNextDouble() {
        ensureOpen();
        return isDecimal(peek());
    }

    public boolean hasNextBoolean() {
        ensureOpen();
        String t = peek();
        return t != null && (t.equalsIgnoreCase("true") || t.equalsIgnoreCase("false"));
    }

    private String expect(boolean matches) {
        String t = peek();
        if (t == null) throw new NoSuchElementException();
        if (!matches) throw new InputMismatchException("For input string: \"" + t + "\"");
        return next();
    }

    public int nextInt() { ensureOpen(); return Integer.parseInt(stripPlus(expect(hasNextInt()))); }
    public long nextLong() { ensureOpen(); return Long.parseLong(stripPlus(expect(hasNextLong()))); }
    public double nextDouble() { ensureOpen(); return Double.parseDouble(expect(hasNextDouble())); }
    public float nextFloat() { ensureOpen(); return Float.parseFloat(expect(hasNextDouble())); }
    public boolean nextBoolean() { ensureOpen(); return Boolean.parseBoolean(expect(hasNextBoolean())); }

    private static String stripPlus(String t) {
        return t.startsWith("+") ? t.substring(1) : t;
    }

    @Override
    public void close() {
        closed = true;
    }
}
