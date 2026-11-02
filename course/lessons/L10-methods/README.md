# L10 Methods: naming a calculation and reusing it

A **method** is a named block of code that receives inputs (**parameters**), does one job, and may
return one result. Writing each formula once, as a method, removes copy-paste errors.

```java
//   modifiers  return type  name       parameters
public static   double       peakFlow(double c, double iMmPerH, double areaHa) {
    return c * iMmPerH * areaHa / 360.0;        // value sent back to the caller
}

double q = peakFlow(0.7, 60, 12.5);             // call: arguments matched by position
```

| Part | Meaning |
|------|---------|
| `static` | belongs to the class; call it without creating an object (L11 explains objects) |
| return type | type of the value returned; `void` when nothing is returned |
| parameters | local variables that receive copies of the arguments |
| `return` | ends the method and hands back the value |

## Scope

Variables declared inside a method (or inside any `{ }` block) exist only there. Two methods can
both have a variable called `area` without any conflict.

## Pass by value

Java always passes a **copy of the value**. For primitives the method cannot change the caller's
variable. For arrays and objects the copied value is a *reference*, so the method can change the
elements the caller sees, but cannot make the caller's variable point to a different array.

## Overloading

Several methods may share a name if their parameter lists differ. Java chooses by the argument
types: `area(0.3)` (circle radius) versus `area(0.3, 0.5)` (rectangle).

## Program walkthrough

* `peakFlow` implements the **Rational Method**, Q = C i A / 360 (Q in m³/s, i in mm/h, A in
  hectares), used for small urban catchments.
* `kirpichMinutes` implements the Kirpich (1940) time of concentration,
  Tc = 0.0195 L^0.77 S^-0.385 (minutes; L in m; S in m/m), also used by the lab's catchment app.
* `area` is overloaded; `doubleAll` shows that an array argument can be modified, while
  `tryToChange` shows that an `int` argument cannot.

## Try it

1. Add a method `boolean isSafe(double stress, double allowable)`.
2. Write `double[] cumulative(double[] rain)` that returns the running total.
