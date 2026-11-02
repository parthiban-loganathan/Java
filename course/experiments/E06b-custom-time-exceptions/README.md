# Experiment 6b: Implementing Custom Exceptions for Time Input Validation

## Problem statement (syllabus)
Alice is tasked with creating a program that validates user input for time values (`hours`, `minutes`, and `seconds`). If any value falls outside the valid range, throw a custom exception: `InvalidHourException`, `InvalidMinuteException`, or `InvalidSecondException`. If no exceptions occur, print `"Correct Time - "` followed by `"hours:minutes:seconds"`.

### Concept: How to Create Your Own Custom Exception Class

To create a custom exception in Java, simply create a class that `extends Exception` and passes the error message up to the parent `Exception` class via `super(msg);`:

```java
class InvalidHourException extends Exception {
    public InvalidHourException(String msg) {
        super(msg);
    }
}
```

## Sample input and expected output

These cases are checked automatically on the JDK by `tools/CourseCheck.java`.

**Case 1**

Input:

```text
14 35 45
```

Output:

```text
Correct Time - 14:35:45
```

**Case 2**

Input:

```text
24 10 10
```

Output:

```text
InvalidHourException: Invalid Hour
```

**Case 3**

Input:

```text
10 75 10
```

Output:

```text
InvalidMinuteException: Invalid Minute
```

**Case 4**

Input:

```text
10 10 60
```

Output:

```text
InvalidSecondException: Invalid Second
```

## Run on your computer

```bash
javac Main.java
java Main < input.txt
```

Or type the input yourself after `java Main`.
