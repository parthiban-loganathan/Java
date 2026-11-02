# Experiment 9: Basic Sentiment Analysis (Classifying Sentences from File Input)

## Problem statement (syllabus)
Mr. Styles needs a program that writes an input sentence to `input.txt`, reads the sentence from `input.txt`, classifies its sentiment based on predefined keywords, writes the sentiment to `output.txt`, and displays it:
- **Positive keywords:** `happy, good, excellent, positive`
- **Negative keywords:** `sad, bad, terrible, negative`
- **Anything else:** `Neutral`

## Sample input and expected output

These cases are checked automatically on the JDK by `tools/CourseCheck.java`.

**Case 1**

Input:

```text
The bridge foundation is in excellent condition.
```

Output:

```text
Positive
```

**Case 2**

Input:

```text
Drainage on the site is BAD after rain
```

Output:

```text
Negative
```

**Case 3**

Input:

```text
The slab was cast on Monday
```

Output:

```text
Neutral
```

## Notes

Matching uses `contains`, so a keyword inside a longer word also matches (`"badge"` contains `"bad"`). Splitting the sentence into words (`split("\\W+")`) would avoid that; try it as an extension.

## Run on your computer

```bash
javac Main.java
java Main < input.txt
```

Or type the input yourself after `java Main`.
