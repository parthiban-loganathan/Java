# Course material

Two tracks, both runnable on the lab website and on any computer with JDK 21.

* **Lessons L01-L14**: programming from zero (what a program is, variables, `int`, `char`,
  `double`, strings, input, decisions, loops, arrays, `ArrayList` and `Vector`, methods, classes,
  maps, exceptions, and how console code becomes a web or Android app). Every example uses civil
  engineering data.
* **Experiments E01a-E16**: the BACSE102 laboratory programs from the course handout, with
  explanations, hand calculations and automatically verified sample input and output.

Each folder contains:

| File | Purpose |
|------|---------|
| `README.md` | explanation, walkthrough, exercises |
| `Main.java` | the program (Experiment 16: `EmployeeApp.java`) |
| `input.txt`, `input-2.txt`, ... | keyboard input for each test case |
| `expected.txt`, `expected-2.txt`, ... | output produced by the JDK for that input |
| `meta.properties` | title, order, and whether it can run in the browser |

Verify everything on your machine:

```bash
java tools/CourseCheck.java
```

## Lessons

| ID | Topic | Runs in browser |
|----|-------|-----------------|
| [L01](lessons/L01-what-is-programming/) | What is programming? | yes |
| [L02](lessons/L02-variables-and-data-types/) | Variables and data types | yes |
| [L03](lessons/L03-operators-and-expressions/) | Operators and expressions | yes |
| [L04](lessons/L04-characters-and-strings/) | Characters and strings | yes |
| [L05](lessons/L05-keyboard-input/) | Keyboard input with Scanner | yes |
| [L06](lessons/L06-decisions/) | if, else, switch | yes |
| [L07](lessons/L07-loops/) | for, while, nested | yes |
| [L08](lessons/L08-arrays/) | Arrays (1-D and 2-D) | yes |
| [L09](lessons/L09-arraylist-and-vector/) | ArrayList and Vector | yes |
| [L10](lessons/L10-methods/) | Methods | yes |
| [L11](lessons/L11-classes-and-objects/) | Classes and objects | yes |
| [L12](lessons/L12-maps-and-sets/) | Maps and sets | yes |
| [L13](lessons/L13-exceptions/) | Exceptions | yes |
| [L14](lessons/L14-from-console-to-apps/) | From console programs to web and Android apps | yes |

## Experiments

| ID | Topic | Runs in browser |
|----|-------|-----------------|
| [E01a](experiments/E01a-financing-interest/) | Calculating Interest for Financing Options | yes |
| [E01b](experiments/E01b-digit-sum-parity/) | Three-Digit Number Analysis (Sum of Digits & Parity) | yes |
| [E01c](experiments/E01c-primes-in-range/) | Generating Prime Numbers Within a Specified Range | yes |
| [E02a](experiments/E02a-merged-step-distance/) | Calculating Total Distance from Merged Daily Step Counts | yes |
| [E02b](experiments/E02b-primes-in-2d-grid/) | Identifying and Locating Prime Numbers in a 2D Grid | yes |
| [E03a](experiments/E03a-rectangle-class/) | Designing a Rectangle Class to Calculate Area and Perimeter | yes |
| [E03b](experiments/E03b-array-concatenator/) | Developing an ArrayConcatenator Class for Merging Arrays | yes |
| [E04a](experiments/E04a-employee-bonus-inheritance/) | Calculating Employee Bonuses Using Hierarchical Inheritance | yes |
| [E04b](experiments/E04b-game-character-abstract/) | Designing a Fantasy Game Character System | yes |
| [E05a](experiments/E05a-washing-machine-interface/) | Designing a Washing Machine Control System | yes |
| [E05b](experiments/E05b-unique-characters/) | Unique Character Extraction | yes |
| [E06a](experiments/E06a-grade-validation-exceptions/) | Validating Student Grade Input with Multi-Catch Exception Handling | yes |
| [E06b](experiments/E06b-custom-time-exceptions/) | Implementing Custom Exceptions for Time Input Validation | yes |
| [E07](experiments/E07-producer-consumer-threads/) | Synchronizing Producer and Consumer Threads with a Shared Message Buffer | yes |
| [E08](experiments/E08-prices-with-tax-files/) | Calculating Final Prices with Tax (File Input and Output) | yes |
| [E09](experiments/E09-sentiment-from-file/) | Basic Sentiment Analysis (Classifying Sentences from File Input) | yes |
| [E10](experiments/E10-speed-conversion-files/) | Converting Speed Units (km/h to m/s) with File Input and Output | yes |
| [E11](experiments/E11-bank-account-serialization/) | Implementing Persistent Bank Account Transactions using Serialization | no (JDK only) |
| [E12](experiments/E12-savings-category-serialization/) | Serializing and Deserializing Savings Data to Determine Savings Category | no (JDK only) |
| [E13](experiments/E13-generic-record-holder/) | Implementing a Generic Record Holder for Academic Data | yes |
| [E14](experiments/E14-student-registration-arraylist/) | Student Registration System using `ArrayList` | yes |
| [E15](experiments/E15-median-temperature-hashmap/) | Calculating Median Temperature from City Data using `HashMap` | yes |
| [E16](experiments/E16-employee-jdbc/) | Employee Management System using Java and JDBC | no (JDK only) |

Experiment 6a is corrected relative to the printed handout (the original multi-catch does not
compile); its README explains why.
