package org.teavm.classlib.java.io;

import java.io.ObjectStreamException;

/** java.io.NotSerializableException (missing in TeaVM). */
public class TNotSerializableException extends ObjectStreamException {
    private static final long serialVersionUID = 1L;

    public TNotSerializableException() {
        super();
    }

    public TNotSerializableException(String className) {
        super(className);
    }
}
