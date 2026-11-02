package org.teavm.classlib.java.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.OutputStream;
import java.io.Serializable;
import lab.io.ObjectStore;

/**
 * java.io.ObjectOutputStream for programs run in the browser (TeaVM has none). The stream starts
 * with the JDK's header (0xACED 0x0005); writeObject checks, as the JDK does, that the object's
 * class implements Serializable (NotSerializableException otherwise) and records the object; the
 * matching ObjectInputStream.readObject returns it. Primitive values are written as by
 * DataOutputStream. Differences from the JDK: the object read back is the same object, so later
 * changes to it are visible and transient fields keep their values.
 */
public class TObjectOutputStream extends OutputStream implements TObjectOutput {
    private final DataOutputStream out;

    public TObjectOutputStream(OutputStream out) throws IOException {
        if (out == null) {
            throw new NullPointerException();
        }
        this.out = new DataOutputStream(out);
        this.out.writeShort(0xACED);
        this.out.writeShort(5);
    }

    @Override
    public final void writeObject(Object obj) throws IOException {
        if (obj == null) {
            out.writeByte(0x70);
            return;
        }
        if (!(obj instanceof Serializable) && !ObjectStore.isLibraryClass(obj.getClass().getName())) {
            throw new NotSerializableException(obj.getClass().getName());
        }
        out.writeByte(0x73);
        out.writeInt(ObjectStore.put(obj));
    }

    public void writeUnshared(Object obj) throws IOException {
        writeObject(obj);
    }

    public void defaultWriteObject() throws IOException {
    }

    public void reset() throws IOException {
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        out.write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        out.writeBoolean(v);
    }

    @Override
    public void writeByte(int v) throws IOException {
        out.writeByte(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        out.writeShort(v);
    }

    @Override
    public void writeChar(int v) throws IOException {
        out.writeChar(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        out.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        out.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        out.writeFloat(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        out.writeDouble(v);
    }

    @Override
    public void writeBytes(String s) throws IOException {
        out.writeBytes(s);
    }

    @Override
    public void writeChars(String s) throws IOException {
        out.writeChars(s);
    }

    @Override
    public void writeUTF(String s) throws IOException {
        out.writeUTF(s);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
