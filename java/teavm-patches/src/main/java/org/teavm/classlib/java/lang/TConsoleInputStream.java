package org.teavm.classlib.java.lang;

import java.io.IOException;
import java.io.InputStream;
import lab.web.BrowserStdin;

/**
 * System.in for programs run in the browser (replaces TeaVM's class of the same name): the
 * keyboard input typed on the page, as UTF-8 bytes. It shares the read position with Scanner and
 * InputStreamReader, so programs may mix them.
 */
class TConsoleInputStream extends InputStream {
    private final byte[] pending = new byte[4];
    private int pendingStart;
    private int pendingEnd;

    TConsoleInputStream() {
    }

    @Override
    public int read() throws IOException {
        if (pendingStart == pendingEnd && !fill()) {
            return -1;
        }
        return pending[pendingStart++] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        int n = 0;
        while (n < len) {
            if (pendingStart == pendingEnd && !fill()) {
                break;
            }
            b[off + n++] = pending[pendingStart++];
        }
        return n == 0 ? -1 : n;
    }

    @Override
    public int available() {
        return (pendingEnd - pendingStart) + (BrowserStdin.text().length() - BrowserStdin.position());
    }

    private boolean fill() {
        String text = BrowserStdin.text();
        int pos = BrowserStdin.position();
        if (pos >= text.length()) {
            return false;
        }
        int cp = text.codePointAt(pos);
        BrowserStdin.advance(pos + Character.charCount(cp));
        pendingStart = 0;
        if (cp < 0x80) {
            pending[0] = (byte) cp;
            pendingEnd = 1;
        } else if (cp < 0x800) {
            pending[0] = (byte) (0xC0 | (cp >> 6));
            pending[1] = (byte) (0x80 | (cp & 0x3F));
            pendingEnd = 2;
        } else if (cp < 0x10000) {
            pending[0] = (byte) (0xE0 | (cp >> 12));
            pending[1] = (byte) (0x80 | ((cp >> 6) & 0x3F));
            pending[2] = (byte) (0x80 | (cp & 0x3F));
            pendingEnd = 3;
        } else {
            pending[0] = (byte) (0xF0 | (cp >> 18));
            pending[1] = (byte) (0x80 | ((cp >> 12) & 0x3F));
            pending[2] = (byte) (0x80 | ((cp >> 6) & 0x3F));
            pending[3] = (byte) (0x80 | (cp & 0x3F));
            pendingEnd = 4;
        }
        return true;
    }
}
