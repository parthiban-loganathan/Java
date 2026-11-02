# 8. Teacher guide: 12 teams, timeline, assessment

The innovation lab runs **alongside** the BACSE102 syllabus. It is not part of the syllabus
assessment unless the department decides so; the experiments in `course/` cover the syllabus and the
projects give those concepts a purpose.

## Mapping syllabus concepts to project work

| Concept (course material) | Where it appears in the projects |
|---------------------------|----------------------------------|
| variables, data types, operators (L02-L03, E01) | every formula in the calculation module |
| decisions, loops (L06-L07, E01c) | classification rules, time stepping |
| arrays, 2-D arrays (L08, E02) | time series, grids (the DEM is a 2-D array) |
| `ArrayList`, maps (L09, L12, E14-E15) | records read from CSV, lookup tables |
| methods, classes, objects (L10-L11, E03) | `SubCatchment`, `Beam`, ... classes with tested methods |
| inheritance, interfaces (E04-E05) | alternative methods behind one interface (e.g. several runoff methods) |
| exceptions (L13, E06) | validating uploaded data with clear messages |
| files (E08-E10) | CSV/JSON input and output |
| threads (E07) | background computation in the Android app |
| collections, generics (E13-E15) | result tables |

## Running the 12 teams

* Teams of 5 with rotating roles: Java calculation, interface, visualisation, data and
  documentation, testing and presentation.
* Week 1: demonstrate the catchment simulator and the starter template live; every team creates its
  repository from the template and deploys it unchanged on day one (success builds confidence).
* Weekly 30-minute stand-up per team: show the last push, the Actions status, the next step.
* Proposals arrive as issues (*Project proposal* form) in the lab repository; approve when the
  problem is real, the data are open and the scope fits 10 weeks.

## Suggested assessment rubric (100 marks)

| Criterion | Marks | Evidence |
|-----------|-------|----------|
| Problem definition and engineering relevance | 15 | README problem statement, proposal issue |
| Correctness of the engineering calculation | 20 | hand calculation, unit tests passing in Actions |
| Java code quality (classes, methods, validation) | 15 | source code review |
| Use and documentation of open data | 10 | data section with licences |
| Interface, visualisation, mobile usability | 10 | live demo |
| Deployment and version control | 10 | Pages URL, commit history from all members |
| Limitations and honesty about results | 10 | README limitations section |
| Presentation and individual viva | 10 | each member explains part of the code |

## AI assistance policy (suggested)

AI tools may be used to explain errors, suggest code and draft text. Students must (1) state in the
README which tools were used and for what, (2) be able to explain every line in the viva, and (3)
verify calculations against hand calculations and tests. Unexplained code is not credited.

## Long-term maintenance

* Each year's completed projects stay in the directory with their academic year.
* New teams may adopt a previous project: fork it, improve it, and add a new directory entry that
  credits the original team.
* Keep `CI` green; update pinned library versions once a year in one pull request.
