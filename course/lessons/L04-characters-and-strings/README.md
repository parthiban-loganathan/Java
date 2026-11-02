# L04 Characters and strings

## `char`: one character

A `char` holds a single character in single quotes: `'A'`, `'7'`, `'#'`. Internally it is an
unsigned 16-bit number, the character's **Unicode** (UTF-16) code: `'A'` is 65, `'a'` is 97,
`'0'` is 48. Because it is a number you can compare and do arithmetic with it:

```java
char grade = 'B';
grade++;                       // now 'C'
int code = 'A';                // 65 (widening char -> int)
char c = (char) 66;            // 'B'
int digit = '7' - '0';         // 7: converts a digit character to its value
```

The `Character` class has helpers: `Character.isDigit(c)`, `isLetter`, `isUpperCase`,
`toUpperCase(c)`.

## `String`: a sequence of characters

`String` is a **class** (note the capital S), not a primitive. A string literal uses double quotes.
Strings are **immutable**: methods return a new string; the original never changes.

| Method | Example (`s = "Concrete M25"`) | Result |
|--------|------------------------------|--------|
| `length()` | `s.length()` | `12` |
| `charAt(i)` | `s.charAt(0)` | `'C'` (index starts at 0) |
| `substring(a, b)` | `s.substring(9, 12)` | `"M25"` (from a up to, not including, b) |
| `indexOf(x)` | `s.indexOf("M")` | `9` (`-1` when absent) |
| `toUpperCase()` | | `"CONCRETE M25"` |
| `contains(x)` | `s.contains("M25")` | `true` |
| `replace(a, b)` | `s.replace("M25", "M30")` | `"Concrete M30"` |
| `split(regex)` | `"6,15,25".split(",")` | array `{"6", "15", "25"}` |
| `trim()` | `"  x  ".trim()` | `"x"` |
| `equals(t)` | `s.equals("Concrete M25")` | `true` |

**Compare strings with `equals`, never with `==`.** `==` checks whether two variables refer to the
same object in memory, not whether the text is the same.

## Converting between text and numbers

```java
int n = Integer.parseInt("25");       // text -> int
double v = Double.parseDouble("7.5"); // text -> double
String t = String.valueOf(3.5);       // number -> text, also "" + 3.5
```

## Escape sequences

`\n` new line, `\t` tab, `\"` a double quote inside a string, `\\` a backslash.

## Formatted output with `printf`

`System.out.printf(format, values...)` replaces each placeholder by the next value:

| Placeholder | Meaning | `printf("[%...]", 3.14159)` |
|-------------|---------|-----------------------------|
| `%d` | integer | (for ints) |
| `%f` | decimal, 6 places | `[3.141590]` |
| `%.2f` | decimal, 2 places | `[3.14]` |
| `%8.2f` | width 8, right aligned | `[    3.14]` |
| `%s` | text | |
| `%c` | character | |
| `%n` | new line (platform independent) | |

`String.format(...)` takes the same arguments and returns the text instead of printing it.

## Try it

1. Read a soil description such as `"Clayey SAND"` and print it in lower case and its length.
2. Split the CSV line `"BH-01,12.5,SC"` into borehole id, depth and soil class.
