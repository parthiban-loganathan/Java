# Experiment 3b: Developing an ArrayConcatenator Class for Merging Arrays

## Problem statement (syllabus)
Roshni is tasked with developing a program for concatenating two arrays provided by the user. Create a class named `ArrayConcatenator` that includes a constructor to concatenate the elements of the input arrays and print the elements of the resulting array separated by a space (1 ≤ N, M ≤ 10; 1 ≤ elements ≤ 100).

### Concept: What Does "Concatenating Two Arrays in a Constructor" Mean?

"Concatenate" simply means joining two lists end-to-end. If Array 1 has N = 3 numbers (`10 20 30`) and Array 2 has M = 4 numbers (`40 50 60 70`), the constructor creates a new array of size N + M = 7, copies all elements from Array 1 into the first 3 slots, and copies all elements from Array 2 into the remaining 4 slots.

## Sample input and expected output

These cases are checked automatically on the JDK by `tools/CourseCheck.java`.

**Case 1**

Input:

```text
3
10 20 30
4
40 50 60 70
```

Output:

```text
10 20 30 40 50 60 70
```

## Run on your computer

```bash
javac Main.java
java Main < input.txt
```

Or type the input yourself after `java Main`.
