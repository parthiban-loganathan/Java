package org.teavm.classlib.java.io;

import java.io.DataOutput;
import java.io.IOException;

/** java.io.ObjectOutput (missing in TeaVM). */
public interface TObjectOutput extends DataOutput, AutoCloseable {
    void writeObject(Object obj) throws IOException;

    @Override
    void write(int b) throws IOException;

    @Override
    void write(byte[] b) throws IOException;

    @Override
    void write(byte[] b, int off, int len) throws IOException;

    void flush() throws IOException;

    @Override
    void close() throws IOException;
}
