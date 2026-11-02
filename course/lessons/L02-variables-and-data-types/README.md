# L02 Variables and data types

## Variable

A **variable** is a named storage location in memory. In Java every variable has a **type**, fixed
when it is declared, which decides what can be stored and how much memory it uses.

```java
double span = 6.0;      // declaration (type + name) and initialisation (= value)
span = 7.5;             // assignment: replace the stored value
```

`=` means *store the value on the right into the variable on the left*. It is not the
mathematical equality; `x = x + 1` is a valid instruction ("increase x by one").

Naming rules: letters, digits, `_` and `$`; must not start with a digit; must not be a keyword
(`int`, `class`, ...). Convention: `camelCase` for variables (`liveLoad`), `UPPER_CASE` for constants.

## The eight primitive types

| Type | Size | Range / precision | Typical engineering use |
|------|------|-------------------|-------------------------|
| `byte` | 8 bit | -128 to 127 | raw sensor bytes |
| `short` | 16 bit | -32 768 to 32 767 | rarely used |
| `int` | 32 bit | -2 147 483 648 to 2 147 483 647 | counts: number of bars, storeys, cells |
| `long` | 64 bit | about ±9.22 × 10^18 | time in milliseconds, very large counts |
| `float` | 32 bit | about 7 significant decimal digits | graphics, memory-limited arrays |
| `double` | 64 bit | about 15-16 significant decimal digits | **all engineering quantities** |
| `char` | 16 bit | one UTF-16 code unit, 0 to 65 535 | a grade letter `'A'`, a symbol |
| `boolean` | - | `true` or `false` | results of checks: `isSafe` |

Literals: `42` is an `int`, `42L` a `long`, `3.5` a `double`, `3.5f` a `float`, `'A'` a `char`
(single quotes), `"A"` a `String` (double quotes, not a primitive; see L04).

## Integers versus decimals

* Integer types store exact whole numbers but **overflow silently** when the result is outside
  their range: `Integer.MAX_VALUE + 1` becomes `-2147483648`.
* `float` and `double` store binary fractions. Most decimal fractions (0.1, 0.2) cannot be stored
  exactly, so `0.1 + 0.2` prints `0.30000000000000004`. For engineering this tiny error is
  harmless; for money use `long` paise or `BigDecimal`.

## Type conversion (casting)

* **Widening** (safe, automatic): `int` -> `long` -> `float` -> `double`. `double d = 5;` stores `5.0`.
* **Narrowing** (may lose information, must be written explicitly): `int bars = (int) 7.9;`
  stores `7` (the fraction is cut off, not rounded). Use `Math.round(7.9)` to round.

## Constants and `var`

* `final double GRAVITY = 9.81;` cannot be changed later; the compiler stops any attempt.
* `var count = 12;` lets the compiler infer the type (`int` here). The type is still fixed.

## Program walkthrough

The program declares one variable of every primitive type for a reinforced-concrete beam, prints
them, then demonstrates overflow, floating-point representation and casting.

Notice the self-weight line: 0.3 × 0.5 × 6 × 25 is exactly 22.5 on paper, but the program prints
`22.499999999999996`, because 0.3 has no exact binary representation. Results are therefore
formatted for reports with `printf("%.2f", value)`, which rounds to two decimals (`22.50`).

## Try it

1. Change `numberOfBars` to `Integer.MAX_VALUE` and add one. What happens?
2. Compute the self-weight of the beam: width × depth × length × 25 kN/m³.
3. Store `9.99` in an `int` using a cast and with `Math.round`; compare.
