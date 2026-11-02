// L04: char as a number, String methods, parsing and formatted output.
public class Main {
    public static void main(String[] args) {
        // ---- char
        char grade = 'B';
        int code = grade;                 // char widens to its Unicode number
        grade++;                          // next letter
        System.out.println("'B' has code " + code + "; after ++ the char is " + grade);
        System.out.println("'7' - '0' = " + ('7' - '0'));
        System.out.println("isDigit('7') = " + Character.isDigit('7') + ", toUpperCase('m') = " + Character.toUpperCase('m'));

        // ---- String methods
        String s = "Concrete M25";
        System.out.println("length         = " + s.length());
        System.out.println("charAt(0)      = " + s.charAt(0));
        System.out.println("substring(9,12)= " + s.substring(9, 12));
        System.out.println("indexOf(\"M\")   = " + s.indexOf("M"));
        System.out.println("toUpperCase    = " + s.toUpperCase());
        System.out.println("replace        = " + s.replace("M25", "M30"));
        System.out.println("original s     = " + s + "   (strings never change)");

        // ---- equals versus ==
        String a = "M25";
        String b = new String("M25");     // same text, different object
        System.out.println("a.equals(b) = " + a.equals(b) + ", a == b = " + (a == b));

        // ---- split and parse a CSV record: borehole id, depth (m), SPT N value
        String record = "BH-01,12.5,23";
        String[] parts = record.split(",");
        String id = parts[0];
        double depth = Double.parseDouble(parts[1]);
        int sptN = Integer.parseInt(parts[2]);
        System.out.println("Borehole " + id + ": depth " + depth + " m, N = " + sptN);

        // ---- escape sequences and printf
        System.out.println("Tab\tseparated, a \"quoted\" word, a backslash \\");
        System.out.printf("[%f] [%.2f] [%8.2f] [%-8s] [%d] [%c]%n", Math.PI, Math.PI, Math.PI, "M25", 42, 'X');
        String line = String.format("%s at %.1f m", id, depth);
        System.out.println(line);
    }
}
