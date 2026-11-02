# L05 Reading keyboard input with `Scanner`

Programs become useful when the same code works for any data. `java.util.Scanner` reads text typed
on the keyboard (standard input, `System.in`) and converts it into Java values.

```java
import java.util.Scanner;              // 1. make the class available

Scanner sc = new Scanner(System.in);   // 2. create one Scanner for the whole program
double span = sc.nextDouble();         // 3. read values in the order they are typed
sc.close();                            // 4. release it at the end
```

## How Scanner reads

The input is split into **tokens** separated by whitespace (spaces, tabs, new lines). Each `next...`
call consumes the next token:

| Method | Reads | Example input -> value |
|--------|-------|-----------------------|
| `nextInt()` | one integer token | `25` -> `25` |
| `nextDouble()` | one decimal token | `7.5` -> `7.5` |
| `nextBoolean()` | `true` / `false` | `true` -> `true` |
| `next()` | one word | `Vellore` -> `"Vellore"` |
| `next().charAt(0)` | first character of a word | `C` -> `'C'` |
| `nextLine()` | the rest of the current line | `Clayey sand` -> `"Clayey sand"` |
| `hasNextInt()`, `hasNext()` | checks whether more input of that kind exists | |

Because tokens are separated by any whitespace, `6 15` on one line and `6` and `15` on two lines
are read identically by two `nextDouble()` calls.

## The `nextLine()` trap

After `nextInt()` or `nextDouble()`, the line break typed after the number is still waiting. A
following `nextLine()` returns that empty remainder immediately. Fix: call `sc.nextLine();` once to
discard it before reading a full line.

## Wrong input

If the user types `abc` where `nextInt()` expects a number, Java throws
`InputMismatchException` and the program stops. Lesson L13 shows how to handle this.

## Running with input on this website

Type the input into the **Input** box exactly as you would type it at the keyboard, then press
**Run**. On the command line you can also redirect a file: `java Main < input.txt`.

## Try it

1. Read the length, breadth and depth of a footing and print its concrete volume.
2. Read a full site address with `nextLine()` after reading an integer, and fix the trap.
