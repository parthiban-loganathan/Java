// L09: ArrayList, wrapper classes, autoboxing, and the legacy Vector.
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        List<Integer> spt = new ArrayList<>();      // grows as readings arrive
        while (sc.hasNextInt()) {                   // read until the input ends
            spt.add(sc.nextInt());                  // autoboxing int -> Integer
        }
        System.out.println("SPT N values: " + spt);
        System.out.println("readings = " + spt.size());

        int sum = 0;
        for (int n : spt) {                         // unboxing Integer -> int
            sum += n;
        }
        System.out.printf("mean N = %.2f, max N = %d, min N = %d%n",
                (double) sum / spt.size(), Collections.max(spt), Collections.min(spt));
        System.out.println("first N >= 30 at reading index " + firstAtLeast(spt, 30));

        spt.add(0, 4);                              // insert at the front
        spt.set(1, 6);                              // replace index 1
        System.out.println("after add(0, 4) and set(1, 6): " + spt);
        spt.remove(2);                              // removes INDEX 2
        System.out.println("after remove(2) (an index):   " + spt);
        spt.remove(Integer.valueOf(50));            // removes the VALUE 50
        System.out.println("after remove(value 50):       " + spt);
        System.out.println("contains 18? " + spt.contains(18) + ", indexOf(18) = " + spt.indexOf(18));

        List<Integer> sorted = new ArrayList<>(spt);
        Collections.sort(sorted);
        System.out.println("sorted copy: " + sorted);

        Vector<Double> loads = new Vector<>();     // legacy synchronized list
        loads.add(12.5);
        loads.add(7.5);
        loads.addElement(3.0);                     // Vector's original method name
        System.out.println("Vector " + loads + " size " + loads.size() + ", element 1 = " + loads.get(1));
        sc.close();
    }

    static int firstAtLeast(List<Integer> values, int limit) {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) >= limit) {
                return i;
            }
        }
        return -1;
    }
}
