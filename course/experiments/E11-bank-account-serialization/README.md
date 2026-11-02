# Experiment 11: Implementing Persistent Bank Account Transactions using Serialization

## Problem statement (syllabus)
Sam wants to deposit and withdraw funds using a `BankAccount` class, serialize the `BankAccount` object to a file named `bankAccount.ser` after the transactions, deserialize the `BankAccount` object from the file when restarting, and display the final balance formatted with two decimal places.

## Sample input and expected output

These cases are checked automatically on the JDK by `tools/CourseCheck.java`.

**Case 1**

Input:

```text
1000.00 500.50 300.25
```

Output:

```text
Final Balance: 1200.25
```

## Notes

Serialization writes the whole object in Java's binary format to `bankAccount.ser`. The browser runner cannot execute it (TeaVM does not implement `ObjectOutputStream`); run it on a lab PC or in Codespaces.

## Run on your computer

```bash
javac Main.java
java Main < input.txt
```

Or type the input yourself after `java Main`.
