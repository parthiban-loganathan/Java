# Experiment 14: Student Registration System using `ArrayList`

## Problem statement (syllabus)
Raman wants to streamline the student registration process using an `ArrayList<String>` to store student names and retrieve a student's name based on the index entered.

### Concept: How `ArrayList` Works

Unlike a basic array (`String[]`) whose size is locked forever once created, an `ArrayList<String>` is a resizable list:
- `students.add(name)` adds a new student to the end of the list.
- `students.size()` tells you how many students are currently in the list.
- `students.get(index)` fetches the student at position `index` (starting at `0`).

## Sample input and expected output

These cases are checked automatically on the JDK by `tools/CourseCheck.java`.

**Case 1**

Input:

```text
3
Arun
Divya
Rohan
1
```

Output:

```text
Student at index 1: Divya
```

**Case 2**

Input:

```text
2
Arun
Divya
5
```

Output:

```text
Invalid index
```

## Run on your computer

```bash
javac Main.java
java Main < input.txt
```

Or type the input yourself after `java Main`.
