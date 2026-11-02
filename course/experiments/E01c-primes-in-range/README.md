# Experiment 1c: Generating Prime Numbers Within a Specified Range

## Problem statement (syllabus)
Arun is tasked with creating a program that prints prime numbers within a given range. The program should take two integers, `start` and `end`, as input, and output the prime numbers between these two values (inclusive) using a `for` loop.

### Concept: How Does a Computer Check if a Number is Prime?

A prime number is an integer greater than or equal to 2 that can only be divided evenly by 1 and itself (e.g., 2, 3, 5, 7, 11, 13).
To find all primes between `start` and `end`, we use two nested `for` loops:
1. **Outer Loop (`i` goes from `start` to `end`):** Picks one number `i` at a time to test.
2. **Assumption Flag (`boolean isPrime = true;`):** Before testing divisors, we assume `i` is prime.
3. **Inner Loop (`j` goes from `2` up to √{i}, written as `j * j <= i`):** Checks if `i % j == 0`. If any `j` divides `i` with zero remainder, then `i` is NOT prime! We set `isPrime = false;` and `break;` (exit the inner loop early).
4. If the inner loop finishes and `isPrime` is still `true`, we print `i`.

## Sample input and expected output

These cases are checked automatically on the JDK by `tools/CourseCheck.java`.

**Case 1**

Input:

```text
10 30
```

Output:

```text
11 13 17 19 23 29
```

**Case 2**

Input:

```text
-5 12
```

Output:

```text
2 3 5 7 11
```

## Notes

Testing divisors only up to √i (`j * j <= i`) is enough: if i = a × b with a ≤ b, then a ≤ √i. For i = 29 the loop tries j = 2, 3, 4, 5 only.

## Run on your computer

```bash
javac Main.java
java Main < input.txt
```

Or type the input yourself after `java Main`.
