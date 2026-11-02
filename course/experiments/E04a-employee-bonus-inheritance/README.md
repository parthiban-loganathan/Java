# Experiment 4a: Calculating Employee Bonuses Using Hierarchical Inheritance

## Problem statement (syllabus)
Shasha, an HR manager, requires a program to compute bonuses for developers and designers using hierarchical inheritance:
- Base class: `Employee` with a `baseSalary` attribute.
- Subclasses: `Developer` (bonus percentage = 10%) and `Designer` (bonus percentage = 5%) extending `Employee`, each with `bonusPercentage`, `workHours`, and methods to calculate and display final income:
    

```text
Final Income = Base Salary + ((Base Salary × Bonus Percentage × Work Hours) / 100)
```

### Concept: What is Hierarchical Inheritance (`extends` and `super`)?

When multiple child classes share a common parent class, it is called **Hierarchical Inheritance**:
- Both `Developer` and `Designer` are employees who have a `baseSalary`. Instead of writing `baseSalary` twice, we put `baseSalary` in the parent class `Employee`.
- Writing `class Developer extends Employee` means `Developer` automatically inherits `baseSalary` from `Employee`.
- Writing `super(baseSalary);` inside the child constructor sends the salary value up to the parent `Employee` constructor.

## Sample input and expected output

These cases are checked automatically on the JDK by `tools/CourseCheck.java`.

**Case 1**

Input:

```text
50000 8
40000 6
```

Output:

```text
Developer Final Income: 90000.00
Designer Final Income: 52000.00
```

## Run on your computer

```bash
javac Main.java
java Main < input.txt
```

Or type the input yourself after `java Main`.
