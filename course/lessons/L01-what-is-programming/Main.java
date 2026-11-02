// L01: the smallest useful Java program.
// It needs no input; it prints text and the result of one calculation.
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, Civil Engineer");          // text inside " " is printed as it is
        System.out.println("Bending moment of a simply supported beam:");
        // Java evaluates the arithmetic first, then joins the number to the text with +
        System.out.println("M = w L^2 / 8 = " + (15.0 * 6.0 * 6.0 / 8.0) + " kN m");
        System.out.print("print keeps the cursor on the same line, ");
        System.out.println("println ends the line.");
    }
}
