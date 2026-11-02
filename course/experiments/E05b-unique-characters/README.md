# Experiment 5b: Unique Character Extraction

## Problem statement (syllabus)
Ashok needs to implement a program using the `java.lang` package to process a given string. The goal is to extract and display a new string containing only the unique characters from the provided input string using the `java.lang.StringBuilder` class.

### Concept: How `StringBuilder` Removes Duplicate Characters

Suppose the input word is `"engineering"`. We want to keep each letter only the *first* time it appears, producing `"engir"`:
1. Create an empty `StringBuilder` box named `sb`.
2. Loop through the input string character by character using `input.charAt(i)`.
3. Check if that character is already inside `sb` using `sb.indexOf(String.valueOf(ch))`. If a character is **not** found inside `sb`, `indexOf` returns `-1`.
4. Whenever `indexOf` returns `-1`, append the character using `sb.append(ch)`.

## Sample input and expected output

These cases are checked automatically on the JDK by `tools/CourseCheck.java`.

**Case 1**

Input:

```text
engineering
```

Output:

```text
engir
```

## Run on your computer

```bash
javac Main.java
java Main < input.txt
```

Or type the input yourself after `java Main`.
