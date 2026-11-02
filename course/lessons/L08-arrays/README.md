# L08 Arrays: many values under one name

## One-dimensional array

An **array** stores a fixed number of values of the **same type** in consecutive, numbered slots.

```java
double[] rain = {2.0, 8.5, 22.0, 31.5, 12.0, 4.0};   // create and fill
double[] depth = new double[24];                      // 24 slots, all 0.0
```

```
 index:   0     1     2     3     4     5
        ┌─────┬─────┬─────┬─────┬─────┬─────┐
 rain   │ 2.0 │ 8.5 │22.0 │31.5 │12.0 │ 4.0 │     rain.length == 6
        └─────┴─────┴─────┴─────┴─────┴─────┘
```

* Indices start at **0**; the last index is `length - 1`.
* `rain[3]` reads slot 3; `rain[3] = 30.0;` writes it.
* The size is fixed when the array is created. For a list that grows, use `ArrayList` (L09).
* New arrays are filled with default values: `0` for numbers, `false` for boolean, `'\u0000'` for
  char, `null` for objects.
* Using an index outside 0..length-1 throws `ArrayIndexOutOfBoundsException`. The classic cause is
  `i <= n` instead of `i < n` in a loop.

## Traversal

```java
for (int i = 0; i < rain.length; i++) { ... rain[i] ... }   // when you need the index
for (double r : rain) { ... r ... }                          // enhanced for: values only
```

## Useful helpers in `java.util.Arrays`

`Arrays.toString(a)` (print), `Arrays.sort(a)` (ascending, in place), `Arrays.copyOf(a, n)`,
`Arrays.fill(a, v)`, `Arrays.equals(a, b)`.

An array variable holds a **reference**. `double[] b = a;` does not copy: both names point to the
same slots. Use `a.clone()` or `Arrays.copyOf` for a real copy.

## Two-dimensional array: a grid

A digital elevation model is a table of elevations, row by row:

```java
double[][] z = new double[rows][cols];   // z[row][col]
z.length       // number of rows
z[0].length    // number of columns
```

The program finds, for the centre cell of a 3 x 3 elevation patch, the neighbour with the steepest
downhill slope (drop divided by distance; diagonal neighbours are √2 cell sizes away). This is the
**D8 flow direction** rule (O'Callaghan and Mark, 1984) used by the lab's catchment application to
trace where rainwater flows.

## Try it

1. Find the hour of peak rainfall (the index of the maximum) and print it as "hour 4".
2. Count the cells of the 3 x 3 patch that are higher than 100 m.
