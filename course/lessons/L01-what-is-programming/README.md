# L01 What is programming?

## The idea in one paragraph

A computer does exactly what it is told, very fast, and nothing else. A **program** is a list of
instructions written in a **programming language** that the computer can translate and carry out.
Writing a program is the same activity as a hand calculation in a design office: you read the
given data, apply formulas and rules in a fixed order, and write down the result. The only
difference is that every step must be written precisely enough for a machine that has no common
sense.

| Hand calculation (beam design)            | Program                                   |
|-------------------------------------------|-------------------------------------------|
| Given: span L = 6 m, load w = 15 kN/m      | **Input**: read or store the values        |
| M = w L² / 8 = 67.5 kN·m                   | **Processing**: evaluate the expression     |
| Report M on the drawing sheet              | **Output**: print the result               |

## Algorithm and flowchart

An **algorithm** is the step-by-step recipe, written before any code. A **flowchart** draws it:

```
 ┌───────────┐    ┌──────────────────┐    ┌──────────────────┐    ┌─────────┐
 │  Start    │ -> │ Input L and w    │ -> │ M = w * L * L / 8│ -> │ Print M │ -> End
 └───────────┘    └──────────────────┘    └──────────────────┘    └─────────┘
```

Good habit for every lab exercise: write the algorithm in plain English, do one hand calculation,
then write the code and check that the program reproduces your hand result.

## How Java runs your code

```
 Main.java  --javac-->  Main.class (bytecode)  --java (JVM)-->  output on screen
 source code            compiled, portable                      program runs
```

1. **Source code**: you type Java in a text file whose name matches the public class (`Main.java`).
2. **Compiler** (`javac Main.java`): checks grammar and types, then produces **bytecode** (`Main.class`).
   Most typing mistakes are caught here, before the program ever runs.
3. **Java Virtual Machine** (`java Main`): executes the bytecode. The same `.class` file runs on
   Windows, Linux, macOS and Android-like systems, because each has its own JVM.

In this laboratory the same Java can also be translated to JavaScript (by the open-source TeaVM
compiler) so that it runs inside a web page, and compiled for Android. Lesson L14 explains this.

## Anatomy of the smallest program

```java
public class Main {                                  // a class named Main (file Main.java)
    public static void main(String[] args) {         // execution always starts here
        System.out.println("Hello, Civil Engineer"); // print a line of text
    }                                                // end of main
}                                                    // end of class
```

| Part | Meaning |
|------|---------|
| `public class Main` | Declares a class (a named container of code). The file must be called `Main.java`. |
| `public static void main(String[] args)` | The entry point. The JVM looks for exactly this method. `void` = returns nothing. |
| `{ }` | Braces mark the start and end of a block. Every `{` needs a matching `}`. |
| `System.out.println(...)` | Prints its argument and moves to a new line. `print` (without `ln`) stays on the line. |
| `;` | Ends a statement, like a full stop ends a sentence. |
| `//` | A comment: ignored by the compiler, written for humans. `/* ... */` spans many lines. |

Java is **case-sensitive**: `Main`, `main` and `MAIN` are three different names.

## Run it

* In this website: press **Run** in the panel next to the code.
* On your computer (JDK 21 installed):

```bash
javac Main.java
java Main
```

## Common mistakes

| Message from `javac` | Cause |
|----------------------|-------|
| `class Main is public, should be declared in a file named Main.java` | File name and class name differ. |
| `';' expected` | A statement is missing its semicolon. |
| `reached end of file while parsing` | A `}` is missing. |
| `cannot find symbol` | A name is misspelt or has the wrong capitalisation (`system.out` instead of `System.out`). |

## Try it

1. Print your name and branch on two lines.
2. Replace `println` by `print` in the first statement and observe the difference.
3. Remove one semicolon, run, and read the compiler message carefully.
