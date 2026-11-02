package lab.io;

import java.util.ArrayList;
import java.util.List;

/**
 * Objects written by ObjectOutputStream in the browser. The stream (and so the file) holds the
 * JDK's stream header and a reference number; ObjectInputStream returns the object with that
 * number. A program run in the browser is one process, and its files exist only while it runs,
 * so every file a program reads back was written by the same run.
 */
public final class ObjectStore {
    private static final List<Object> OBJECTS = new ArrayList<>();

    private ObjectStore() {
    }

    public static int put(Object object) {
        OBJECTS.add(object);
        return OBJECTS.size() - 1;
    }

    public static Object get(int id) {
        return id >= 0 && id < OBJECTS.size() ? OBJECTS.get(id) : null;
    }

    /** True for classes of the Java library, which this runtime treats as serializable. */
    public static boolean isLibraryClass(String name) {
        return name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("[");
    }
}
