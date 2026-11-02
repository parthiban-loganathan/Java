# L03 Operators and expressions

An **expression** combines values, variables and operators and produces one value:
`w * L * L / 8` is an expression; `double m = w * L * L / 8;` stores its value.

## Arithmetic operators

| Operator | Meaning | Example | Result |
|----------|---------|---------|--------|
| `+` | addition | `7 + 2` | `9` |
| `-` | subtraction | `7 - 2` | `5` |
| `*` | multiplication | `7 * 2` | `14` |
| `/` | division | `7 / 2` and `7.0 / 2` | `3` and `3.5` |
| `%` | remainder (modulo) | `7 % 2` | `1` |

**Integer division** is the most common beginner bug: when both operands are integers, `/`
discards the fraction. `1 / 3 * 300` is `0`, while `1.0 / 3 * 300` is `100.0`.

## Precedence (which part is evaluated first)

1. Parentheses `( )`
2. Unary `-x`, `!b`, casts `(int) x`
3. `*`, `/`, `%` (left to right)
4. `+`, `-` (left to right)
5. Comparisons `<`, `<=`, `>`, `>=`, then `==`, `!=`
6. `&&`, then `||`
7. Assignment `=`, `+=`, `-=`, `*=`, `/=`

When in doubt, add parentheses: they cost nothing and make formulas readable.

## Assignment shortcuts and increment

`total += load;` means `total = total + load;`. `i++` adds one to `i` (used in loops).

## The `Math` class

| Method | Meaning |
|--------|---------|
| `Math.sqrt(x)` | square root |
| `Math.pow(a, b)` | a to the power b |
| `Math.abs(x)` | absolute value |
| `Math.PI` | π |
| `Math.sin(x)`, `Math.cos(x)` | trigonometry, x in **radians** (`Math.toRadians(deg)`) |
| `Math.max(a, b)`, `Math.min(a, b)` | larger / smaller value |
| `Math.round(x)`, `Math.ceil(x)`, `Math.floor(x)` | rounding |

## Program walkthrough: simply supported beam

For span L = 6 m and uniformly distributed load w = 15 kN/m:

* reaction R = wL/2 = 45 kN
* maximum bending moment M = wL²/8 = 67.5 kN·m
* area of one 16 mm bar = π d²/4 = 201.06 mm²

The program also shows integer division, the remainder operator and compound assignment.

## Try it

1. Compute the maximum deflection 5wL⁴/(384EI) with E = 25 000 N/mm², I = 3.125 × 10⁹ mm⁴
   (convert units carefully: w in N/mm, L in mm).
2. Predict, then run: `(int) (0.29 * 100)`. It prints `28`, because `0.29 * 100` is stored as
   `28.999999999999996` and a cast cuts the fraction off. Use `Math.round` when you mean rounding.
