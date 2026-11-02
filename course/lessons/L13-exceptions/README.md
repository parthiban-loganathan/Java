# L13 Exceptions: handling bad data without crashing

An **exception** is an object that Java *throws* when something goes wrong at run time: a text
that is not a number, an index outside an array, a missing file. If nobody *catches* it, the
program stops and prints a stack trace.

```java
try {
    double depth = Double.parseDouble(text);    // may throw NumberFormatException
    check(depth);                               // may throw IllegalArgumentException
} catch (NumberFormatException e) {             // more specific type first
    System.out.println("not a number: " + e.getMessage());
} catch (IllegalArgumentException e) {
    System.out.println("out of range: " + e.getMessage());
} finally {
    // always runs, used to release resources
}
```

## Common exception types

| Exception | Typical cause |
|-----------|---------------|
| `NumberFormatException` | `Integer.parseInt("12a")` |
| `InputMismatchException` | `Scanner.nextInt()` meets a word |
| `ArrayIndexOutOfBoundsException` | index < 0 or >= length |
| `NullPointerException` | calling a method on `null` |
| `ArithmeticException` | integer division by zero (`5 / 0`); note `5.0 / 0` gives `Infinity` |
| `IllegalArgumentException` | a method rejects an argument (thrown by your own checks) |
| `IOException`, `FileNotFoundException` | file problems (checked: the compiler forces you to handle them) |

## Throwing your own

`throw new IllegalArgumentException("rainfall cannot be negative");` stops the method and passes
the exception to the caller. This is how a well-written method refuses invalid input.

## Checked versus unchecked

* **Checked** exceptions (subclasses of `Exception` but not `RuntimeException`, e.g. `IOException`)
  must be caught or declared with `throws` in the method header.
* **Unchecked** exceptions (`RuntimeException` and its subclasses) need not be declared.

## Ordering catch blocks

`NumberFormatException` is a subclass of `IllegalArgumentException`. A catch for the parent also
catches the child, so the child must be caught first. Java rejects `catch (NumberFormatException |
IllegalArgumentException e)` because a multi-catch may not list a type together with its own
subclass (Java Language Specification §14.20). Experiment 6a in this course applies this rule.

## Program walkthrough

Each input line holds a rainfall depth. The program parses and validates each value, reports the
bad ones with the reason, and continues with the next line, a pattern used for every real dataset.

## JVM versus browser

On the JVM every array access is checked and `5 / 0` with integers throws `ArithmeticException`.
The lab website runs this program after translating it to JavaScript with TeaVM, and that
translation omits both checks for speed: in the browser `gauges[3] = 5` silently does nothing
useful and `5 / 0` gives `0`. This is why the website shows the JDK result as the reference and
why programs must be tested on a real JVM before submission.

## Try it

1. Add a check that rejects values above 1000 mm as "implausible".
2. Divide an `int` by zero and a `double` by zero; compare the outcomes.
