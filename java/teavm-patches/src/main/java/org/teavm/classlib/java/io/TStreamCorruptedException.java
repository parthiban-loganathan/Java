package org.teavm.classlib.java.io;

import java.io.ObjectStreamException;

/** java.io.StreamCorruptedException (missing in TeaVM). */
public class TStreamCorruptedException extends ObjectStreamException {
    private static final long serialVersionUID = 1L;

    public TStreamCorruptedException() {
        super();
    }

    public TStreamCorruptedException(String reason) {
        super(reason);
    }
}
