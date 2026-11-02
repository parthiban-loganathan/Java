# L11 Classes and objects

So far every program was a sequence of statements in `main`. Real engineering software models
**things**: beams, footings, catchments, rain gauges. A **class** describes a kind of thing; an
**object** is one particular thing built from that description.

| Idea | Civil engineering analogy | Java |
|------|---------------------------|------|
| class | a standard drawing for a beam type | `class Beam { ... }` |
| object (instance) | one beam built on site from that drawing | `new Beam(6.0, 15.0)` |
| field | a dimension or property written on the drawing | `private double span;` |
| constructor | the setting-out instructions used when it is built | `Beam(double span, double load)` |
| method | a calculation that applies to every beam of that type | `double maxMoment()` |

## Writing a class

```java
class Beam {
    private final double span;   // fields: the state of each object
    private double load;

    Beam(double span, double load) {     // constructor: same name as the class, no return type
        this.span = span;                // this.span = the field; span = the parameter
        this.load = load;
    }

    double maxMoment() { return load * span * span / 8; }
}
```

* `new Beam(6, 15)` allocates a new object and runs the constructor.
* `this` means "the object whose method is running".
* Each object has its own copy of the fields: two beams can have different spans.

## Encapsulation

Fields are `private` so that other classes cannot put an object into an invalid state (a negative
span). Access goes through methods (**getters/setters**) which can check values. The constructor
below throws `IllegalArgumentException` for a non-positive span.

## `static` versus instance members

A `static` field belongs to the class itself, shared by all objects (here, a counter of how many
beams were created). An instance field belongs to one object.

## `toString`

When an object is printed or joined to a string, Java calls its `toString()` method. Overriding it
(`@Override`) gives readable output instead of `Beam@1b6d3586`.

## Arrays and lists of objects

`Beam[] beams = { new Beam(...), ... };` or `List<Beam>`: the loop then works on objects.

## Try it

1. Add a method `double maxShear()` returning wL/2.
2. Add a class `Footing` with length, width and bearing pressure, and a method for the safe load.
