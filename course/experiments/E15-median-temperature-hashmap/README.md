# Experiment 15: Calculating Median Temperature from City Data using `HashMap`

## Problem statement (syllabus)
As a data analyst for a weather forecasting agency, you have a `HashMap` storing temperature data for various cities. Sort the readings in ascending order and report the median temperature for each city (if the count is odd, the median is the middle number; if even, the median is the average of the two middle numbers).

### Concept: What is a `HashMap` and How is the Median Calculated?

- **`HashMap<String, List<Double>>`:** Works like a two-column lookup table where the **Key** is the city name (`"Vellore"`) and the **Value** is its list of temperature readings (`List<Double>`). We insert data using `map.put(city, temps)`.
- **Sorting & Median:** `Collections.sort(temps)` sorts the list from lowest to highest.
  - If n = 5 (odd): integer division `5 / 2` gives index `2` (the 3rd number, which is the exact middle!).
  - If n = 4 (even): we average index `(4 / 2) - 1 = 1` and index `4 / 2 = 2`.

## Sample input and expected output

These cases are checked automatically on the JDK by `tools/CourseCheck.java`.

**Case 1**

Input:

```text
2
Vellore 5
34.5 31.0 36.2 33.8 32.0
Chennai 4
30.0 34.0 32.0 36.0
```

Output:

```text
City: Vellore, Median Temperature: 33.80
City: Chennai, Median Temperature: 33.00
```

## Notes

The handout declares the variable as `HashMap` but creates a `LinkedHashMap` (a subclass). `LinkedHashMap` keeps insertion order, so the cities print in input order; with a plain `HashMap` the order would be unspecified.

## Run on your computer

```bash
javac Main.java
java Main < input.txt
```

Or type the input yourself after `java Main`.
