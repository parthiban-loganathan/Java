package lab.web;

import org.teavm.jso.JSBody;

/**
 * Text that a web page supplies as "keyboard input" for a course program. The browser has no
 * console, so {@code new Scanner(System.in)} reads from here instead (see TScanner).
 *
 * Two ways to provide the text:
 *  - the course page's precompiled programs call {@link #reset(String)} before main;
 *  - programs compiled in the browser (lab page) read the global variable {@code labStdin},
 *    which the page sets before it starts the program.
 */
public final class BrowserStdin {
    static String text = null;
    static int position = 0;

    private BrowserStdin() { }

    @JSBody(script = "return (typeof globalThis.labStdin === 'string') ? globalThis.labStdin : '';")
    private static native String fromPage();

    public static String text() {
        if (text == null) {
            text = fromPage();
        }
        return text;
    }

    public static int position() { return position; }
    public static void advance(int newPosition) { position = newPosition; }

    public static void reset(String input) {
        text = input == null ? "" : input;
        position = 0;
    }
}
