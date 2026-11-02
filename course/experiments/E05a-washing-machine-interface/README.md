# Experiment 5a: Designing a Washing Machine Control System

## Problem statement (syllabus)
Define an `interface` named `Motor` with methods `void run()` and `double consume(double capacity)`. Create a class called `WashingMachine` that implements the `Motor` interface:
- Implement `run()` to print `"Washing machine is running."`
- Implement `consume()` to print `"Washing machine is consuming electricity."`
- Implement `consume(double capacity)` to calculate electricity consumption in kWh using capacity × 0.05.

### Concept: What is an `interface` and What is Method Overloading?

- **Interface (`interface Motor`):** Like an IS / BIS quality standard certificate. It lists method signatures that any motor-driven appliance must provide. A class signs this contract using the keyword `implements Motor` (and must mark implemented methods as `public`).
- **Method Overloading:** Notice that `WashingMachine` has two methods named `consume`: one takes no input (`consume()`) and prints a message, while the other takes a decimal input (`consume(double capacity)`) and computes capacity × 0.05. Java automatically picks the right one based on whether you pass a number inside the parentheses!

## Sample input and expected output

These cases are checked automatically on the JDK by `tools/CourseCheck.java`.

**Case 1**

Input:

```text
7.5
```

Output:

```text
Washing machine is running.
Washing machine is consuming electricity.
Electricity consumption: 0.38 kWh
```

## Notes

`7.5 × 0.05 = 0.375`. `printf("%.2f")` rounds half-up, so `0.38` is printed.

## Run on your computer

```bash
javac Main.java
java Main < input.txt
```

Or type the input yourself after `java Main`.
