package org.teavm.classlib.java.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

/**
 * java.io.OutputStreamWriter for programs run in the browser (replaces TeaVM's, whose charset
 * encoder needs JavaScript-only buffers in the WebAssembly runtime; FileWriter and PrintWriter
 * build on it). Encodes UTF-8 (default), ISO-8859-1 and US-ASCII by hand.
 */
public class TOutputStreamWriter extends Writer {
    private OutputStream out;
    private final int charset;            // 0 UTF-8, 1 ISO-8859-1, 2 US-ASCII
    private final byte[] buffer = new byte[8192];
    private int count;
    private int highSurrogate = -1;

    public TOutputStreamWriter(OutputStream out) {
        this(out, 0);
    }

    public TOutputStreamWriter(OutputStream out, String charsetName) throws UnsupportedEncodingException {
        this(out, code(charsetName));
    }

    public TOutputStreamWriter(OutputStream out, Charset charset) {
        this(out, codeOrUtf8(charset.name()));
    }

    public TOutputStreamWriter(OutputStream out, CharsetEncoder encoder) {
        this(out, codeOrUtf8(encoder.charset().name()));
    }

    private TOutputStreamWriter(OutputStream out, int charset) {
        super(out);
        if (out == null) {
            throw new NullPointerException();
        }
        this.out = out;
        this.charset = charset;
    }

    private static int code(String name) throws UnsupportedEncodingException {
        if (name == null) {
            throw new NullPointerException("charsetName");
        }
        String n = name.toUpperCase().replace("_", "-");
        if (n.equals("UTF-8") || n.equals("UTF8")) {
            return 0;
        }
        if (n.equals("ISO-8859-1") || n.equals("ISO8859-1") || n.equals("LATIN1")) {
            return 1;
        }
        if (n.equals("US-ASCII") || n.equals("ASCII")) {
            return 2;
        }
        throw new UnsupportedEncodingException(name);
    }

    private static int codeOrUtf8(String name) {
        try {
            return code(name);
        } catch (UnsupportedEncodingException e) {
            return 0;
        }
    }

    public String getEncoding() {
        if (out == null) {
            return null;
        }
        return charset == 0 ? "UTF8" : charset == 1 ? "ISO8859_1" : "ASCII";
    }

    private void ensureOpen() throws IOException {
        if (out == null) {
            throw new IOException("Stream closed");
        }
    }

    private void put(int b) throws IOException {
        if (count == buffer.length) {
            out.write(buffer, 0, count);
            count = 0;
        }
        buffer[count++] = (byte) b;
    }

    private void encode(int c) throws IOException {
        if (charset != 0) {
            put(c < (charset == 1 ? 0x100 : 0x80) ? c : '?');
            return;
        }
        if (highSurrogate >= 0) {
            int high = highSurrogate;
            highSurrogate = -1;
            if (Character.isLowSurrogate((char) c)) {
                int cp = Character.toCodePoint((char) high, (char) c);
                put(0xF0 | (cp >> 18));
                put(0x80 | ((cp >> 12) & 0x3F));
                put(0x80 | ((cp >> 6) & 0x3F));
                put(0x80 | (cp & 0x3F));
                return;
            }
            put('?');
        }
        if (Character.isHighSurrogate((char) c)) {
            highSurrogate = c;
        } else if (c < 0x80) {
            put(c);
        } else if (c < 0x800) {
            put(0xC0 | (c >> 6));
            put(0x80 | (c & 0x3F));
        } else if (Character.isLowSurrogate((char) c)) {
            put('?');
        } else {
            put(0xE0 | (c >> 12));
            put(0x80 | ((c >> 6) & 0x3F));
            put(0x80 | (c & 0x3F));
        }
    }

    @Override
    public void write(int c) throws IOException {
        ensureOpen();
        encode(c & 0xFFFF);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        ensureOpen();
        if (off < 0 || len < 0 || off + len > cbuf.length) {
            throw new IndexOutOfBoundsException();
        }
        for (int i = 0; i < len; i++) {
            encode(cbuf[off + i]);
        }
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        ensureOpen();
        if (off < 0 || len < 0 || off + len > str.length()) {
            throw new IndexOutOfBoundsException();
        }
        for (int i = 0; i < len; i++) {
            encode(str.charAt(off + i));
        }
    }

    @Override
    public void flush() throws IOException {
        ensureOpen();
        if (count > 0) {
            out.write(buffer, 0, count);
            count = 0;
        }
        out.flush();
    }

    @Override
    public void close() throws IOException {
        if (out != null) {
            if (highSurrogate >= 0) {
                highSurrogate = -1;
                put('?');
            }
            flush();
            out.close();
            out = null;
        }
    }
}
