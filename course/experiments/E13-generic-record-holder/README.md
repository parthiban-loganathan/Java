# Experiment 13: Implementing a Generic Record Holder for Academic Data

## Problem statement (syllabus)
Design a generic class named `RecordHolder<T>` that can hold any type of academic record:
- A private instance variable `record` of type `T`, a constructor, `getRecord()`, and `displayRecordInfo()`.
- Concrete class `GradeRecord` (`studentName` as `String`, `grade` as `Double`) that prints `"Grade for [studentName]: [grade]"`.
- Concrete class `EnrollmentRecord` (`courseName` as `String`, `studentId` as `Integer`) that prints `"Student [studentId] is enrolled in [courseName]"`.

### Concept: What is a Generic Class (`<T>`)?

Normally, a class variable must have a fixed type (like `int` or `String`). Writing `class RecordHolder<T>` uses `<T>` as a **Type Placeholder** (like a blank label on an ISO storage container). When we create `RecordHolder<GradeRecord>`, `T` automatically becomes `GradeRecord`. When we create `RecordHolder<EnrollmentRecord>`, `T` becomes `EnrollmentRecord`!

## Sample input and expected output

These cases are checked automatically on the JDK by `tools/CourseCheck.java`.

**Case 1**

Input:

```text
Karthik 92.5
1045 BACSE102
```

Output:

```text
Grade for Karthik: 92.5
Student 1045 is enrolled in BACSE102
```

## Run on your computer

```bash
javac Main.java
java Main < input.txt
```

Or type the input yourself after `java Main`.
