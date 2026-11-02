# Experiment 6a: Validating Student Grade Input with Multi-Catch Exception Handling

## Problem statement (syllabus)
Sampad wants to implement a program that takes input for a student's name and grade, validates the input using `validateGrade()`, and displays the grade:
- `validateGrade()` throws an `IllegalArgumentException` if the grade is less than 0 or greater than 100.
- If the input is a non-integer, catch `NumberFormatException` and print the built-in exception message.
- If the input is out-of-range, catch `IllegalArgumentException` and print the built-in exception message.
- Catch both exceptions using a **multi-catch block**.

### Concept: Understanding `throw` and Multi-Catch (`|`)

- `Integer.parseInt(gradeInput)` converts a text string like `"85"` into the integer `85`. If the user typed `"eighty"`, `parseInt` automatically throws a `NumberFormatException`.
- Inside `validateGrade(int grade)`, if `grade < 0 || grade > 100`, we manually trigger an alarm using `throw new IllegalArgumentException("...");`.
- A **multi-catch block** uses a single vertical bar `|` to catch both exception types in one place: `catch (NumberFormatException | IllegalArgumentException e)`.

## Sample input and expected output

These cases are checked automatically on the JDK by `tools/CourseCheck.java`.

**Case 1**

Input:

```text
Sampad
85
```

Output:

```text
Student: Sampad, Grade: 85
```

**Case 2**

Input:

```text
Sampad
140
```

Output:

```text
java.lang.IllegalArgumentException: Grade must be between 0 and 100.
```

**Case 3**

Input:

```text
Sampad
eighty
```

Output:

```text
java.lang.NumberFormatException: For input string: "eighty"
```

## Notes

### Correction to the printed handout

The handout used `catch (NumberFormatException | IllegalArgumentException e)`. That line **does not compile**:

```text
error: Alternatives in a multi-catch statement cannot be related by subclassing
  Alternative NumberFormatException is a subclass of alternative IllegalArgumentException
```

A multi-catch may not list a class together with one of its own subclasses (Java Language
Specification §14.20), and `NumberFormatException extends IllegalArgumentException`. Two correct
versions exist:

1. Two ordered catch blocks, subclass first (used here, so each error can be handled separately).
2. A single `catch (IllegalArgumentException e)`, which catches both, because a parent-type catch
   also catches every subclass.

Both print exactly the expected outputs above.

## Run on your computer

```bash
javac Main.java
java Main < input.txt
```

Or type the input yourself after `java Main`.
