package lab.web;

import org.teavm.jso.JSBody;

/** The database set-up script given by the page (globalThis.labSql), run before the first connection. */
public final class BrowserSql {
    private BrowserSql() {
    }

    @JSBody(script = "return (typeof globalThis.labSql === 'string') ? globalThis.labSql : '';")
    public static native String setupScript();
}
