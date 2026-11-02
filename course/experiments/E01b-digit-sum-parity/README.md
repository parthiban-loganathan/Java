# Experiment 1b: Three-Digit Number Analysis (Sum of Digits & Parity)

## Problem statement (syllabus)
Alex is teaching a class on number properties and wants to create an exercise for his students. He needs a program that takes a three-digit number as input, calculates the sum of its digits, and determines whether the sum is even or odd.

### Concept: How to Extract Digits Using `%` (Remainder) and `/` (Integer Division)

When Java divides two `int` numbers using `/`, it throws away the decimal remainder. When it uses `%`, it keeps *only* the remainder! Let us test this on the three-digit number num = 473:
- **Last digit (`units`):** Divide 473 by 10 arrow quotient is 47, remainder is 3. So `473 % 10 = 3`.
- **Middle digit (`tens`):** First chop off the last digit using `473 / 10 = 47`. Now take the last digit of 47 using `47 % 10 = 7`. Combined: `(num / 10) % 10 = 7`.
- **First digit (`hundreds`):** Chop off the last two digits using `473 / 100 = 4`.
- **Sum & Parity:** sum = 4 + 7 + 3 = 14. To check if 14 is Even or Odd, we check if the remainder when divided by 2 is zero (`sum % 2 == 0`).

## Sample input and expected output

These cases are checked automatically on the JDK by `tools/CourseCheck.java`.

**Case 1**

Input:

```text
473
```

Output:

```text
Sum of digits: 14
Parity: Even
```

**Case 2**

Input:

```text
-509
```

Output:

```text
Sum of digits: 14
Parity: Even
```

**Case 3**

Input:

```text
381
```

Output:

```text
Sum of digits: 12
Parity: Even
```

**Case 4**

Input:

```text
128
```

Output:

```text
Sum of digits: 11
Parity: Odd
```

## Notes

`Math.abs` makes the program work for negative input as well: `-509` gives the digit sum 5 + 0 + 9 = 14 (second test case).

## Run on your computer

```bash
javac Main.java
java Main < input.txt
```

Or type the input yourself after `java Main`.
