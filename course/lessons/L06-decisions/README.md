# L06 Making decisions: `if`, `else`, `switch`

## Conditions

A **condition** is an expression whose value is `boolean` (`true` or `false`).

| Operator | Meaning | Example |
|----------|---------|---------|
| `==`, `!=` | equal, not equal (numbers, chars) | `bars == 4` |
| `<`, `<=`, `>`, `>=` | comparisons | `stress <= allowable` |
| `&&` | AND: both must be true | `depth > 0 && depth < 30` |
| `\|\|` | OR: at least one true | `grade == 'D' \|\| slope > 0.1` |
| `!` | NOT | `!isSafe` |

`&&` and `||` are *short-circuit*: the right side is evaluated only if needed, so
`n != 0 && total / n > 5` never divides by zero.

## `if` / `else if` / `else`

```java
if (rain == 0) {
    category = "No rain";
} else if (rain <= 2.4) {
    category = "Very light rain";
} else {
    category = "...";
}
```

The conditions are tested from top to bottom and **only the first true branch runs**. Order the
thresholds so each test can assume the previous ones failed.

## `switch`

When one variable is compared with several constant values, `switch` is clearer:

```java
switch (group) {
    case 'A': description = "low runoff potential"; break;
    case 'D': description = "high runoff potential"; break;
    default:  description = "unknown";
}
```

`break` leaves the switch; without it execution "falls through" into the next case. Java 14+ also
offers the arrow form `case 'A' -> ...` which never falls through.

## Program walkthrough

1. Classifies 24-hour rainfall with the terminology of the India Meteorological Department (IMD):
   no rain (0), very light (0.1-2.4 mm), light (2.5-15.5), moderate (15.6-64.4), heavy (64.5-115.5),
   very heavy (115.6-204.4), extremely heavy (≥ 204.5 mm).
2. Describes a hydrologic soil group (USDA NRCS, NEH Part 630 Chapter 7): A low, B moderately low,
   C moderately high, D high runoff potential when thoroughly wet.

Test the program with several inputs: a value on each boundary (2.4, 2.5, 64.4, 64.5) is the best
way to find off-by-one mistakes in conditions.

## Try it

1. Add a check that rejects negative rainfall with a message.
2. Rewrite the `switch` in the arrow form.
