package lab.web;

/**
 * Text that a web page supplies as "keyboard input" for a course program. The browser has no
 * console, so {@code new Scanner(System.in)} reads from here instead (see TScanner).
 */
public final class BrowserStdin {
    static String text = "";
    static int position = 0;

    private BrowserStdin() { }

    public static String text() { return text; }
    public static int position() { return position; }
    public static void advance(int newPosition) { position = newPosition; }

    public static void reset(String input) {
        text = input == null ? "" : input;
        position = 0;
    }
}
