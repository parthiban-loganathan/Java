# Experiment 4b: Designing a Fantasy Game Character System

## Problem statement (syllabus)
Jessica is designing a fantasy game character system that includes an `abstract` class named `GameCharacter` with two abstract methods: `attack()` and `defend()`. Two subclasses, `Warrior` and `Wizard`, extend `GameCharacter`. The program prompts the player to choose a character class (`1` for Warrior, `2` for Wizard) and input their strength or magic power. Dynamic calculations involve tripling the strength (strength × 3) for the Warrior's attack and doubling the magic power (power × 2) for the Wizard's attack.

### Concept: What is an `abstract` Class and `@Override`?

- An `abstract class` is a general template that says: "Every `GameCharacter` must have an `attack()` and a `defend()` method, but each specific character type will calculate them differently."
- An `abstract` method has no curly braces—just a semicolon: `abstract void attack();`.
- When `Warrior` and `Wizard` extend `GameCharacter`, they provide the actual formula using `@Override`.

## Sample input and expected output

These cases are checked automatically on the JDK by `tools/CourseCheck.java`.

**Case 1**

Input:

```text
1 25
```

Output:

```text
Warrior Attack: 75
Warrior defends with shield!
```

**Case 2**

Input:

```text
2 40
```

Output:

```text
Wizard Attack: 80
Wizard defends with magic barrier!
```

## Run on your computer

```bash
javac Main.java
java Main < input.txt
```

Or type the input yourself after `java Main`.
