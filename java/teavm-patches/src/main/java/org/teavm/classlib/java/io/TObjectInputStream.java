package org.teavm.classlib.java.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.StreamCorruptedException;
import lab.io.ObjectStore;

/** java.io.ObjectInputStream for programs run in the browser; see TObjectOutputStream. */
public class TObjectInputStream extends InputStream implements TObjectInput {
    private final DataInputStream in;

    public TObjectInputStream(InputStream in) throws IOException {
        if (in == null) {
            throw new NullPointerException();
        }
        this.in = new DataInputStream(in);
        int magic = this.in.readShort() & 0xFFFF;
        int version = this.in.readShort() & 0xFFFF;
        if (magic != 0xACED || version != 5) {
            throw new StreamCorruptedException(String.format("invalid stream header: %04X%04X", magic, version));
        }
    }

    @Override
    public final Object readObject() throws IOException, ClassNotFoundException {
        int tag = in.readByte() & 0xFF;
        if (tag == 0x70) {
            return null;
        }
        if (tag != 0x73) {
            throw new StreamCorruptedException(String.format("invalid type code: %02X", tag));
        }
        int id = in.readInt();
        Object obj = ObjectStore.get(id);
        if (obj == null) {
            throw new InvalidClassException("object " + id + " was not written by this program run");
        }
        return obj;
    }

    public Object readUnshared() throws IOException, ClassNotFoundException {
        return readObject();
    }

    public void defaultReadObject() throws IOException, ClassNotFoundException {
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return in.read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        in.readFully(b);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        in.readFully(b, off, len);
    }

    @Override
    public int skipBytes(int n) throws IOException {
        return in.skipBytes(n);
    }

    @Override
    public boolean readBoolean() throws IOException {
        return in.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return in.readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return in.readUnsignedByte();
    }

    @Override
    public short readShort() throws IOException {
        return in.readShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return in.readUnsignedShort();
    }

    @Override
    public char readChar() throws IOException {
        return in.readChar();
    }

    @Override
    public int readInt() throws IOException {
        return in.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return in.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return in.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return in.readDouble();
    }

    @Override
    @SuppressWarnings("deprecation")
    public String readLine() throws IOException {
        return in.readLine();
    }

    @Override
    public String readUTF() throws IOException {
        return in.readUTF();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
