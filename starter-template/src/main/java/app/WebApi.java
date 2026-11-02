package app;

import org.teavm.jso.JSExport;

/**
 * The bridge between the web page and the Java calculation. TeaVM turns every method marked
 * {@code @JSExport} into a JavaScript function in target/js/app.js; web/app.js imports it with
 * {@code import { calculate } from './java/app.js';}.
 *
 * Keep this class thin: receive strings or numbers, call the calculation module, return text.
 */
public class WebApi {

    public static void main(String[] args) {
        // Nothing to start; the page calls calculate().
    }

    /** Returns the results as JSON, or {"error": "..."} when the input is invalid. */
    @JSExport
    public static String calculate(String subCatchmentCsv, String rainfallCsv) {
        try {
            return RunoffCalculator.calculateJson(subCatchmentCsv, rainfallCsv);
        } catch (RuntimeException e) {
            String msg = String.valueOf(e.getMessage()).replace("\\", "/").replace("\"", "'");
            return "{\"error\":\"" + msg + "\"}";
        }
    }
}
