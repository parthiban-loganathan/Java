# Experiment 12: Serializing and Deserializing Savings Data to Determine Savings Category

## Problem statement (syllabus)
Alice wants to categorize her savings by creating an object of `SavingsData` with salary and savings data, serializing it to `savings.ser`, deserializing it, and displaying the category based on Savings Percentage = (savings) / salary × 100:
- `"Poor savings"`: between 1% (inclusive) and 10%.
- `"Good savings"`: between 10% (inclusive) and 20%.
- `"High savings"`: ≥ 20%.
- `"Invalid input"`: if none of the above conditions are met.

## Sample input and expected output

These cases are checked automatically on the JDK by `tools/CourseCheck.java`.

**Case 1**

Input:

```text
50000 7500
```

Output:

```text
Good savings
```

**Case 2**

Input:

```text
50000 2500
```

Output:

```text
Poor savings
```

**Case 3**

Input:

```text
50000 12000
```

Output:

```text
High savings
```

**Case 4**

Input:

```text
0 100
```

Output:

```text
Invalid input
```

## Notes

The four test cases cover every branch: 15 % (Good), 5 % (Poor), 24 % (High) and a zero salary (Invalid input).

## Run on your computer

```bash
javac Main.java
java Main < input.txt
```

Or type the input yourself after `java Main`.
