package civiltech.io;

/**
 * Minimal JSON writer (no external library) used to hand results from Java to JavaScript.
 * Numbers are written with a fixed number of significant decimals to keep the text small.
 */
public final class JsonWriter {

    private final StringBuilder sb = new StringBuilder();
    private boolean needComma;

    public JsonWriter beginObject() { comma(); sb.append('{'); needComma = false; return this; }
    public JsonWriter endObject() { sb.append('}'); needComma = true; return this; }
    public JsonWriter beginArray() { comma(); sb.append('['); needComma = false; return this; }
    public JsonWriter endArray() { sb.append(']'); needComma = true; return this; }

    public JsonWriter name(String name) {
        comma();
        string(name);
        sb.append(':');
        needComma = false;
        return this;
    }

    public JsonWriter value(String v) { comma(); string(v); needComma = true; return this; }
    public JsonWriter value(boolean v) { comma(); sb.append(v); needComma = true; return this; }
    public JsonWriter value(long v) { comma(); sb.append(v); needComma = true; return this; }

    public JsonWriter value(double v) {
        comma();
        number(v, 6);
        needComma = true;
        return this;
    }

    public JsonWriter field(String n, String v) { return name(n).value(v); }
    public JsonWriter field(String n, double v) { return name(n).value(v); }
    public JsonWriter field(String n, long v) { return name(n).value(v); }
    public JsonWriter field(String n, boolean v) { return name(n).value(v); }

    public JsonWriter array(String n, double[] values, int decimals) {
        name(n);
        comma();
        sb.append('[');
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            number(values[i], decimals);
        }
        sb.append(']');
        needComma = true;
        return this;
    }

    public JsonWriter array(String n, int[] values) {
        name(n);
        comma();
        sb.append('[');
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(values[i]);
        }
        sb.append(']');
        needComma = true;
        return this;
    }

    private void comma() {
        if (needComma) {
            sb.append(',');
        }
    }

    /** Writes a finite number rounded to the given decimals; NaN and infinity become null. */
    private void number(double v, int decimals) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            sb.append("null");
            return;
        }
        double scale = Math.pow(10, decimals);
        double r = Math.round(v * scale) / scale;
        if (r == Math.rint(r) && Math.abs(r) < 1e15) {
            sb.append((long) r);
        } else {
            sb.append(r);
        }
    }

    private void string(String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        String hex = Integer.toHexString(c);
                        sb.append("\\u");
                        for (int p = hex.length(); p < 4; p++) sb.append('0');
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    @Override
    public String toString() { return sb.toString(); }
}
