# Experiment 8: Calculating Final Prices with Tax (File Input and Output)

## Problem statement (syllabus)
Ella wants to calculate final prices after applying a 10% tax to her purchases (1 ≤ N ≤ 10). Create a file named `prices.txt` and write the entered item prices to this file. Then read from `prices.txt`, apply a 10% tax to each item (price × 1.10), write the taxed amounts to `tax.txt`, and display the taxed amounts separated by a space with two decimal places.

## Sample input and expected output

These cases are checked automatically on the JDK by `tools/CourseCheck.java`.

**Case 1**

Input:

```text
3
100.0 250.5 80.0
```

Output:

```text
110.00 275.55 88.00
```

## Notes

The program creates `prices.txt` and `tax.txt` in the current folder. In the browser runner these files live in a temporary in-memory file system that disappears after the run.

## Run on your computer

```bash
javac Main.java
java Main < input.txt
```

Or type the input yourself after `java Main`.
