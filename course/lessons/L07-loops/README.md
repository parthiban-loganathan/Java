# L07 Repeating work: loops

Engineering calculations are repetitive: every hour of a storm, every bar, every survey station.
A **loop** repeats a block of statements.

## `for` loop: when the number of repetitions is known

```java
for (int hour = 1; hour <= 6; hour++) {   // start; condition; update
    ...
}
```

1. `int hour = 1` runs once at the start.
2. Before every pass the condition `hour <= 6` is checked; the loop ends when it is false.
3. After every pass `hour++` runs.

## `while` loop: when you repeat until something happens

```java
while (storage > 0) { ... }               // condition checked before each pass
do { ... } while (answer != 0);           // body runs at least once
```

Make sure something inside the loop changes the condition, otherwise the loop never ends.

## `break` and `continue`

`break` leaves the loop immediately; `continue` skips to the next pass.

## Accumulator pattern

```java
double total = 0;                 // initialise before the loop
for (...) { total += value; }     // update inside the loop
```

## Program walkthrough: storm runoff table

The program reads the hourly rainfall of a storm and applies the SCS Curve Number equation
(USDA NRCS NEH-630 Chapter 10) to the **cumulative** rainfall P:

```
S  = 25400 / CN - 254          (mm)
Ia = 0.2 S
Q  = (P - Ia)^2 / (P - Ia + S)  if P > Ia, else 0
```

Hourly runoff is the difference between consecutive cumulative values. For CN = 80:
S = 63.5 mm and Ia = 12.7 mm, so no runoff appears until 12.7 mm of rain has fallen.
A `while` loop then counts how many hours a 40 mm detention pond needs to empty at 6 mm/h, and a
nested loop prints a small multiplication-style table of runoff depth for several P and CN.

## Try it

1. Stop the storm loop with `break` when cumulative rain exceeds 100 mm.
2. Print only the hours with runoff greater than zero using `continue`.
