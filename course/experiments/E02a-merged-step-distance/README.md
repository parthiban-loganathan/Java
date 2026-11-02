# Experiment 2a: Calculating Total Distance from Merged Daily Step Counts

## Problem statement (syllabus)
You are given the number of steps a person takes on two consecutive days. Your task is to merge the step counts from both days and calculate the total distance the person covers, assuming that each step covers a fixed distance of 1 unit.

### Concept: What is a 1D Array (`int[
`)?]
Instead of creating dozens of separate variables, an **Array** stores multiple values of the same type in a single numbered row of lockers.
- `int[] mergedSteps = {day1Steps, day2Steps};` creates an array with 2 lockers: locker `0` (`mergedSteps[0]`) holds Day 1's steps, and locker `1` (`mergedSteps[1]`) holds Day 2's steps.

## Sample input and expected output

These cases are checked automatically on the JDK by `tools/CourseCheck.java`.

**Case 1**

Input:

```text
4500 6200
```

Output:

```text
Total Distance Covered: 10700
```

## Run on your computer

```bash
javac Main.java
java Main < input.txt
```

Or type the input yourself after `java Main`.
