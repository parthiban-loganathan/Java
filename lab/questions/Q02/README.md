# Q02 Concrete cube test results

Cubes of side 150 mm are crushed in the lab. The compressive strength of one cube is

strength (N/mm²) = load (kN) × 1000 / (150 × 150)

Write a program `Main` that reads **fck** (the characteristic strength in N/mm², an integer), then **n** (the number of
cubes, 1 to 20), then the **n** failure loads in kN, and prints:

```text
Cube 1: <strength, 2 decimals> N/mm2
...
Cube n: <strength, 2 decimals> N/mm2
Mean strength: <mean, 2 decimals> N/mm2
Lowest cube: <number of the cube with the lowest strength>
Result: <ACCEPTED or REJECTED>
```

The result is `ACCEPTED` when the mean strength is at least fck + 4 and no single cube is below fck - 4;
otherwise `REJECTED`. If two cubes have the same lowest strength, give the first one.

Store the strengths in an array.
