# L14 From console programs to web and Android apps

Every exercise so far read from the keyboard and printed text. Real applications have buttons,
charts and maps, but the **engineering logic is the same Java**. The skill that makes this possible
is separating the program into layers:

```
┌──────────────────────────────┐   ┌───────────────────────────────┐   ┌──────────────────────┐
│  Input / Output (interface)  │   │  Engineering logic (pure Java) │   │  Data                │
│  console: Scanner, println   │-->│  classes and methods with no   │<--│  CSV / JSON files,   │
│  web: HTML + JavaScript      │   │  input/output of their own     │   │  open datasets       │
│  Android: Activity + Views   │<--│  (e.g. RunoffCalculator)       │   │                      │
└──────────────────────────────┘   └───────────────────────────────┘   └──────────────────────┘
```

The logic layer receives values as parameters and returns results. It never calls `Scanner` or
`System.out`. Because of that, the very same class can be used by:

| Front end | How the Java logic runs | Used in this lab |
|-----------|------------------------|------------------|
| Console | `javac` + `java` on the JVM | all exercises |
| Web page | the open-source **TeaVM** compiler translates the Java bytecode to JavaScript at build time; the page calls exported Java methods from JavaScript | exercise runner and catchment simulator on the lab website |
| Android | the Android build compiles the same `.java` files into the app | `android-app/` |

Data travels between layers as simple text formats:

* **CSV** (comma-separated values): one record per line, e.g. `2024-11-30 14:30,12.4`.
* **JSON** (JavaScript Object Notation): nested objects and arrays,
  e.g. `{"peak": 48.3, "series": [0, 1.2, 4.5]}`. JavaScript reads JSON directly, which is why the
  lab's Java code returns JSON strings to the web page.

## Program walkthrough

`RunoffCalculator` is a pure logic class (SCS Curve Number, see L07). `Main` is a thin console
front end: it reads CSV text, calls the calculator and prints a table plus one JSON line. The
lab's web page does exactly the same, with JavaScript instead of `Main` and a chart instead of the
table. The input rows are real ERA5 reanalysis values for the catchment south-east of VIT during
Cyclone Fengal (local time, 1 December 2024), taken from the lab's dataset
`site/data/catchment/era5_vellore_hourly.csv`.

## Try it

1. Add a `peakHour()` method to `RunoffCalculator` and include it in the JSON.
2. Open the lab's *Catchment simulator* page and find the same idea: inputs, Java calculation,
   JSON, chart.
