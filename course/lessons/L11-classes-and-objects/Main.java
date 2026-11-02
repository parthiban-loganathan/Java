// L11: a class with private fields, a validating constructor, methods, static members and toString.
class Beam {
    private static int created = 0;          // shared by all Beam objects

    private final String mark;               // e.g. "B1" on the drawing
    private final double span;               // m
    private double load;                     // kN/m, can change (setter)

    Beam(String mark, double span, double load) {
        if (span <= 0) {
            throw new IllegalArgumentException("span must be positive: " + span);
        }
        this.mark = mark;
        this.span = span;
        this.load = load;
        created++;
    }

    double getSpan() { return span; }
    double getLoad() { return load; }

    void setLoad(double load) {
        if (load < 0) {
            throw new IllegalArgumentException("load cannot be negative");
        }
        this.load = load;
    }

    double maxMoment() { return load * span * span / 8.0; }   // simply supported, UDL
    double maxShear() { return load * span / 2.0; }

    static int count() { return created; }

    @Override
    public String toString() {
        return String.format("%s: span %.1f m, w %.1f kN/m, M %.2f kN m, V %.2f kN",
                mark, span, load, maxMoment(), maxShear());
    }
}

public class Main {
    public static void main(String[] args) {
        Beam b1 = new Beam("B1", 6.0, 15.0);
        Beam b2 = new Beam("B2", 4.5, 22.0);
        System.out.println(b1);                     // toString() is called automatically
        System.out.println(b2);

        b1.setLoad(18.0);                           // change state through a method
        System.out.println("after setLoad: " + b1);

        Beam[] floor = {b1, b2, new Beam("B3", 7.2, 12.0)};
        Beam critical = floor[0];
        for (Beam b : floor) {
            if (b.maxMoment() > critical.maxMoment()) {
                critical = b;
            }
        }
        System.out.println("beams created: " + Beam.count());
        System.out.println("critical beam -> " + critical);

        try {
            new Beam("B4", -3.0, 10.0);
        } catch (IllegalArgumentException e) {
            System.out.println("rejected: " + e.getMessage());
        }
    }
}
