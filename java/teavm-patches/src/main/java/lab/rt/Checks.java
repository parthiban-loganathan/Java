package lab.rt;

/**
 * Run-time checks called from students' code (inserted into the class files by
 * lab.teavm.ChecksTransformer): the exceptions and messages of the JDK. A null array is left to
 * the original instruction, which throws NullPointerException.
 */
public final class Checks {
    private Checks() {
    }

    private static int length(Object array) {
        if (array instanceof Object[]) {
            return ((Object[]) array).length;
        } else if (array instanceof int[]) {
            return ((int[]) array).length;
        } else if (array instanceof double[]) {
            return ((double[]) array).length;
        } else if (array instanceof char[]) {
            return ((char[]) array).length;
        } else if (array instanceof long[]) {
            return ((long[]) array).length;
        } else if (array instanceof boolean[]) {
            return ((boolean[]) array).length;
        } else if (array instanceof byte[]) {
            return ((byte[]) array).length;
        } else if (array instanceof float[]) {
            return ((float[]) array).length;
        } else if (array instanceof short[]) {
            return ((short[]) array).length;
        }
        return Integer.MAX_VALUE;
    }

    public static void index(Object array, int index) {
        if (array != null) {
            int length = length(array);
            if (index < 0 || index >= length) {
                throw new ArrayIndexOutOfBoundsException("Index " + index + " out of bounds for length " + length);
            }
        }
    }

    public static void store(int[] a, int i, int v) {
        index(a, i);
        a[i] = v;
    }

    public static void store(long[] a, int i, long v) {
        index(a, i);
        a[i] = v;
    }

    public static void store(float[] a, int i, float v) {
        index(a, i);
        a[i] = v;
    }

    public static void store(double[] a, int i, double v) {
        index(a, i);
        a[i] = v;
    }

    public static void store(char[] a, int i, int v) {
        index(a, i);
        a[i] = (char) v;
    }

    public static void store(short[] a, int i, int v) {
        index(a, i);
        a[i] = (short) v;
    }

    public static void store(Object[] a, int i, Object v) {
        index(a, i);
        a[i] = v;
    }

    public static void storeByteOrBoolean(Object a, int i, int v) {
        index(a, i);
        if (a instanceof boolean[]) {
            ((boolean[]) a)[i] = (v & 1) != 0;
        } else {
            ((byte[]) a)[i] = (byte) v;
        }
    }

    public static void size(int size) {
        if (size < 0) {
            throw new NegativeArraySizeException(String.valueOf(size));
        }
    }

    public static int div(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("/ by zero");
        }
        return b == -1 ? -a : a / b;
    }

    public static int rem(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("/ by zero");
        }
        return b == -1 ? 0 : a % b;
    }

    public static long div(long a, long b) {
        if (b == 0) {
            throw new ArithmeticException("/ by zero");
        }
        return b == -1 ? -a : a / b;
    }

    public static long rem(long a, long b) {
        if (b == 0) {
            throw new ArithmeticException("/ by zero");
        }
        return b == -1 ? 0 : a % b;
    }
}
