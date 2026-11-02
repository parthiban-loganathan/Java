# Q01 Peak runoff by the rational method

A small urban catchment drains to one culvert. The rational method gives the peak runoff

Q = C i A / 360

with Q in m³/s, C the runoff coefficient (no unit), i the rainfall intensity in mm/h and A the area in hectares.

Write a program `Main` that reads, from the keyboard, **C**, **i** and **A** (three numbers, in this order) and prints
exactly three lines:

```text
Runoff coefficient C = <C, 2 decimals>
Peak runoff Q = <Q, 3 decimals> m3/s
Culvert class: <class>
```

where the class is `small` if Q < 1, `medium` if 1 <= Q < 5 and `large` if Q >= 5.
If C is not between 0 and 1 (both included), print only the line `Invalid runoff coefficient`.

Use `Scanner`, `double` and `System.out.printf` (for example `%.3f`).
