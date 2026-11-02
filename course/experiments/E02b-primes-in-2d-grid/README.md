# Experiment 2b: Identifying and Locating Prime Numbers in a 2D Grid

## Problem statement (syllabus)
Yashna is creating a program to analyse a 2D grid, identifying prime numbers and printing their coordinates. The input consists of the grid's dimensions and elements. The program's output displays the coordinates of each prime number found in the grid.

### Civil engineering analogy: 2D Survey Grids & Column Layout Coordinates

On a building site, columns are located at the intersection of Row i and Column j. In Java, a 2D array `int[][] grid = new int[rows][cols];` creates a table with `rows` horizontal rows and `cols` vertical columns.
**Important:** Java starts counting rows and columns from `0`! So in a 2 × 3 grid, the row indices are `0, 1` and the column indices are `0, 1, 2`.

## Line-by-line notes

- **Lines 5–11:** Defines a reusable helper method `isPrime(int n)` so our main loop stays clean and easy to read.
- **Lines 22–26:** The outer loop moves row by row (`i`), and the inner loop moves across each column (`j`) to store numbers in `grid[i][j]`.

## Sample input and expected output

These cases are checked automatically on the JDK by `tools/CourseCheck.java`.

**Case 1**

Input:

```text
2 3
4 7 9
11 15 2
```

Output:

```text
7 found at (0, 1)
11 found at (1, 0)
2 found at (1, 2)
```

## Run on your computer

```bash
javac Main.java
java Main < input.txt
```

Or type the input yourself after `java Main`.
