# Experiment 1a: Calculating Interest for Financing Options

## Problem statement (syllabus)
Bobby is a financial analyst working for a construction company. He is tasked with evaluating two financing options for funding a new construction project. Each option involves different principal amounts and annual interest rates. Write a program for him that takes the principal amounts and interest rates for both financing options as input and calculates the respective interests for the financial options.

```text
Total Interest Paid = (Principal Amount × Annual Interest Rate) / 100
```

### Civil engineering analogy: Comparing Bank Loans for Highway Construction

Before starting a construction project, the planning engineer evaluates two bank loan offers. Because money and interest rates have decimal values (e.g., 7.5%), we use the `double` data type.

## Hand calculation and plan
1. Suppose Option 1 has Principal_1 = 500000 and Rate_1 = 7.5. On paper, Interest_1 = (500000 × 7.5) / 100 = 37500.00.
2. Suppose Option 2 has Principal_2 = 600000 and Rate_2 = 6.8. On paper, Interest_2 = (600000 × 6.8) / 100 = 40800.00.
3. In Java, we read these four `double` values using `sc.nextDouble()`, apply the formula, and print both results using `printf("%.2f%n", ...)` so they show two decimal places.

## Line-by-line notes

- **Lines 8–13:** `sc.nextDouble()` reads the four decimal numbers typed by the user.
- **Lines 16–17:** Multiplies principal by rate and divides by `100.0`.
- **Lines 20–21:** `%.2f` formats the decimal number to 2 decimal digits; `%n` creates a new line.

## Sample input and expected output

These cases are checked automatically on the JDK by `tools/CourseCheck.java`.

**Case 1**

Input:

```text
500000 7.5
600000 6.8
```

Output:

```text
Interest for Option 1: 37500.00
Interest for Option 2: 40800.00
```

## Run on your computer

```bash
javac Main.java
java Main < input.txt
```

Or type the input yourself after `java Main`.
