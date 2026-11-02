package org.teavm.classlib.java.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import lab.web.BrowserStdin;

/**
 * java.io.InputStreamReader for programs run in the browser (replaces TeaVM's, whose charset
 * decoder needs JavaScript-only buffers in the WebAssembly runtime). Decodes UTF-8 (default),
 * ISO-8859-1 and US-ASCII by hand. On System.in it reads the page's keyboard input directly and
 * shares the read position with Scanner.
 */
public class TInputStreamReader extends Reader {
    private InputStream in;
    private final boolean stdin;
    private final int charset;            // 0 UTF-8, 1 ISO-8859-1, 2 US-ASCII
    private int pendingLow = -1;          // second half of a supplementary character

    public TInputStreamReader(InputStream in) {
        this(in, 0);
    }

    public TInputStreamReader(InputStream in, String charsetName) throws UnsupportedEncodingException {
        this(in, code(charsetName));
    }

    public TInputStreamReader(InputStream in, Charset charset) {
        this(in, codeOrUtf8(charset.name()));
    }

    private TInputStreamReader(InputStream in, int charset) {
        super(in);
        if (in == null) {
            throw new NullPointerException();
        }
        this.in = in;
        this.stdin = in == System.in;
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
        if (in == null) {
            return null;
        }
        return charset == 0 ? "UTF8" : charset == 1 ? "ISO8859_1" : "ASCII";
    }

    @Override
    public int read() throws IOException {
        ensureOpen();
        if (pendingLow >= 0) {
            int c = pendingLow;
            pendingLow = -1;
            return c;
        }
        if (stdin) {
            String text = BrowserStdin.text();
            int pos = BrowserStdin.position();
            if (pos >= text.length()) {
                return -1;
            }
            BrowserStdin.advance(pos + 1);
            return text.charAt(pos);
        }
        int b = in.read();
        if (b < 0) {
            return -1;
        }
        if (charset == 1) {
            return b;
        }
        if (charset == 2) {
            return b < 0x80 ? b : 0xFFFD;
        }
        if (b < 0x80) {
            return b;
        }
        int extra;
        int cp;
        if (b >= 0xF0 && b < 0xF8) {
            extra = 3;
            cp = b & 0x07;
        } else if (b >= 0xE0) {
            extra = 2;
            cp = b & 0x0F;
        } else if (b >= 0xC0) {
            extra = 1;
            cp = b & 0x1F;
        } else {
            return 0xFFFD;
        }
        for (int k = 0; k < extra; k++) {
            int c = in.read();
            if (c < 0 || (c & 0xC0) != 0x80) {
                return 0xFFFD;
            }
            cp = (cp << 6) | (c & 0x3F);
        }
        if (cp >= 0x10000) {
            pendingLow = Character.lowSurrogate(cp);
            return Character.highSurrogate(cp);
        }
        return cp;
    }

    @Override
    public int read(char[] buf, int off, int len) throws IOException {
        ensureOpen();
        if (len == 0) {
            return 0;
        }
        int n = 0;
        while (n < len) {
            int c = read();
            if (c < 0) {
                break;
            }
            buf[off + n++] = (char) c;
            if (c == '\n' && !ready()) {
                break;                       // hand complete lines to BufferedReader promptly
            }
        }
        return n == 0 ? -1 : n;
    }

    @Override
    public boolean ready() throws IOException {
        ensureOpen();
        if (pendingLow >= 0) {
            return true;
        }
        if (stdin) {
            return BrowserStdin.position() < BrowserStdin.text().length();
        }
        return in.available() > 0;
    }

    @Override
    public void close() throws IOException {
        if (in != null) {
            if (!stdin) {
                in.close();
            }
            in = null;
        }
    }

    private void ensureOpen() throws IOException {
        if (in == null) {
            throw new IOException("Stream closed");
        }
    }
}
