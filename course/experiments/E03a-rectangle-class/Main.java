import java.util.Scanner;

// Blueprint (Class) for a Rectangle
class Rectangle {
    // 1. Properties (Variables belonging to each Rectangle object)
    int length;
    int breadth;

    // 2. Constructor: Initializes length and breadth when object is created
    Rectangle(int length, int breadth) {
        this.length = length;
        this.breadth = breadth;
    }

    // 3. Behaviours (Methods that calculate and return Area and Perimeter)
    int getArea() {
        return length * breadth;
    }

    int getPerimeter() {
        return 2 * (length + breadth);
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        int length = sc.nextInt();
        int breadth = sc.nextInt();

        // Build a Rectangle object named 'rect' using the 'new' keyword
        Rectangle rect = new Rectangle(length, breadth);

        // Call methods on 'rect' using the dot (.) operator
        System.out.println("Area: " + rect.getArea());
        System.out.println("Perimeter: " + rect.getPerimeter());

        sc.close();
    }
}
