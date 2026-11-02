# L09 Growing lists: `ArrayList` and `Vector`

An array has a fixed size. Field data rarely does: a borehole log gets a new reading every 1.5 m
until refusal. The **Java Collections Framework** provides lists that grow and shrink.

## `ArrayList<E>`

```java
import java.util.ArrayList;

ArrayList<Integer> spt = new ArrayList<>();   // empty list of Integer
spt.add(8);                                    // append
spt.add(0, 5);                                 // insert at index 0
int first = spt.get(0);                        // read
spt.set(1, 9);                                 // replace
spt.remove(Integer.valueOf(9));                // remove by value
spt.size();                                    // number of elements
```

| Method | Meaning |
|--------|---------|
| `add(x)`, `add(i, x)` | append, insert at index i |
| `get(i)`, `set(i, x)` | read, replace |
| `remove(i)` / `remove(obj)` | remove by index / by value |
| `size()`, `isEmpty()`, `clear()` | |
| `contains(x)`, `indexOf(x)` | search (`-1` if absent) |
| `Collections.sort(list)`, `Collections.max(list)` | utilities from `java.util.Collections` |

`<Integer>` is a **type parameter** (generics): the list accepts only `Integer` objects and the
compiler checks it. `new ArrayList<>()` lets the compiler fill in the type (the "diamond").

## Wrapper classes and autoboxing

Collections store **objects**, not primitives. Each primitive has a wrapper class:

| primitive | wrapper |
|-----------|---------|
| `int` | `Integer` |
| `double` | `Double` |
| `char` | `Character` |
| `boolean` | `Boolean` |
| `long` | `Long` |

Java converts automatically: `spt.add(8)` *boxes* 8 into an `Integer`; `int n = spt.get(0)`
*unboxes* it. Trap: on an `ArrayList<Integer>`, `remove(2)` removes **index 2**, while
`remove(Integer.valueOf(2))` removes the **value 2**.

## `Vector<E>`

`java.util.Vector` is the original growable array from Java 1.0. It has the same methods as
`ArrayList` (it also implements `List`), but every method is `synchronized`, which makes each
single call safe when several threads use the list, and slower otherwise. New code uses
`ArrayList` and, when threads share a list, chooses an explicit concurrent collection. You will
still meet `Vector` in older code and textbooks, so the program uses both.

| | `ArrayList` | `Vector` |
|--|------------|----------|
| introduced | Java 1.2 | Java 1.0 |
| synchronized | no | yes (every method) |
| growth when full | +50 % | doubles |
| recommendation | default choice | legacy code only |

Mathematical **vectors** (force components, coordinates) are a different idea; in Java they are
usually represented by a small class or a `double[]`.

## Program to an interface

```java
List<Double> depths = new ArrayList<>();   // variable type is the interface List
```

Writing `List` on the left lets you switch to another implementation later without changing the
rest of the code.

## Program walkthrough

Reads SPT N values of a borehole until the input ends, stores them in an `ArrayList`, computes
statistics, demonstrates the `remove` trap, and repeats a few operations on a `Vector`.

## Try it

1. Read depths and N values into two lists and print the depth at which N first exceeds 30.
2. Replace `ArrayList` by `LinkedList` (same methods); does the output change?
