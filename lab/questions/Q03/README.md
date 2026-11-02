# Q03 Water demand forecast (arithmetic increase method)

A town's census populations for the last **k** decades are known. The arithmetic increase method assumes the
population grows by the same amount every decade: the average of the decadal increases.

Write a program `Main` with a method

```java
static double averageIncrease(long[] population)
```

that returns the average of `population[j] - population[j - 1]` for j = 1 .. k-1. The program reads **k** (2 to 10),
the **k** populations (oldest first), the number of decades to forecast **d** and the per-capita demand **q** in
litres per person per day, and prints:

```text
Average increase per decade: <rounded to the nearest whole number>
Year +10: population <P>, demand <D> MLD
...
Year +<10*d>: population <P>, demand <D> MLD
```

with P = last census + m × average increase for m = 1 .. d, rounded to the nearest whole number with `Math.round`,
and D = P × q / 1 000 000 in million litres per day with 2 decimals.
