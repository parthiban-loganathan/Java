# VIT Civil Engineering Innovation Lab

An open, student-driven laboratory where first-year civil engineering students at VIT Vellore learn
Java by building working applications for real engineering problems, with open data, free tools and
public source code.

**New to GitHub? Read [`docs/00-start-here.md`](docs/00-start-here.md) first** (click-by-click set-up), then
[`docs/09-teaching-plan.md`](docs/09-teaching-plan.md) (what to teach and demonstrate each week).

**Website (GitHub Pages):** `https://parthiban-loganathan.github.io/Java/` once Pages is enabled
(Settings → Pages → Source: GitHub Actions; the site is deployed from `main`).

| Part | What it is | Where |
|------|-----------|-------|
| Learn Java | 14 lessons from zero (programs, `int`, `char`, `double`, strings, input, decisions, loops, arrays, `ArrayList` and `Vector`, methods, classes, maps, exceptions, apps) and the 23 BACSE102 experiments, each runnable in the browser with automatically verified outputs | [`course/`](course/), page `learn.html` |
| 3D catchment simulator | Hourly rainfall-runoff for a 38.65 km² hill catchment beside VIT: Copernicus DEM, ESA WorldCover, ERA5 rainfall of Cyclone Fengal; Java does depression filling, D8, delineation, Kirpich Tc, SCS Curve Number, SCS unit hydrograph and time-area routing; Three.js animates it | [`java/civiltech-core`](java/civiltech-core), page `catchment.html` |
| CivilTech Innovation Starter | The one template every team copies: Java module + tests, CSV upload, Chart.js, Leaflet, Three.js, results table, Pages deployment | [`starter-template/`](starter-template/) |
| Android app | The same Java core compiled into a native Android app (APK built by GitHub Actions) | [`android-app/`](android-app/) |
| Java Lab assessments | Students type (paste blocked), compile, run and check Java in the browser and submit with register number and name; the teacher opens and closes each assessment with one button; submissions are encrypted for the teacher and carry the full typing log; a private gradebook decrypts, re-tests with hidden cases in isolated containers, flags pasting, large insertions, time away and similar programs, and writes mark sheets; the Teacher tools page replays the typing | [`lab/`](lab/), [`tools/lab/`](tools/lab/), [`lab-system/`](lab-system/), pages `lab.html`, `teacher.html`, [`docs/10-lab-assessments.md`](docs/10-lab-assessments.md) |
| Problem bank | 100 real civil, environmental and climate problems with methods, open data and Java concepts; claim and submit from the website | [`problems/`](problems/), pages `problems.html`, `submit.html` |
| Project directory | Student projects, validated by a schema, shown on the website | [`projects/`](projects/) |
| Guides | Start here, architecture, GitHub set-up, student guide, submission, method, data, Android, teacher guide, teaching plan | [`docs/`](docs/) |

## Quick start

GitHub Codespaces (nothing to install): **Code → Codespaces → Create codespace on main**, then:

```bash
bash tools/build-site.sh
```

```bash
python3 -m http.server 8000 --directory build/site
```

On a PC: JDK 21, Maven 3.9, Python 3 (for the preview server) and Node.js 20+ (only for the
verification scripts).

## Verification (all run in CI)

```bash
mvn -B -f java/pom.xml install
```

```bash
java tools/CourseCheck.java
```

```bash
node tools/verification/browser_course_check.mjs java/web-bridge/target/js/civiltech.js course
```

```bash
node tools/verification/browser_hydrology_check.mjs java/web-bridge/target/js/civiltech.js site/data/catchment
```

```bash
python3 tools/verification/crosscheck_terrain.py site/data/catchment/vellore_hills_grid.json 77 234
```

* 12 unit tests of the core library (plus 3 in the starter template) (hand calculations, mass balance, D8 on synthetic terrain,
  the real catchment), all passing.
* All 37 course programs compile and reproduce their expected output on JDK 21 (Experiment 16
  against a real MySQL 8.4 server in CI).
* 49 test cases run in the browser build: 47 identical to the JDK, 2 documented differences of the
  TeaVM runtime (exception message wording; no array-bounds / integer-division checks).
* The catchment (38.650 km², longest path 15 414 m, drop 205.9 m) is reproduced exactly by an
  independent Python implementation; JVM and browser give identical hydrographs.

## Correction to the course handout

Experiment 6a in the printed handout uses
`catch (NumberFormatException | IllegalArgumentException e)`, which does not compile because
`NumberFormatException` is a subclass of `IllegalArgumentException`. The course version uses two
ordered catch blocks and explains why (`course/experiments/E06a-grade-validation-exceptions`).

## Licences

Code MIT, text CC BY 4.0, data under their original licences; see [`NOTICE.md`](NOTICE.md) and
[`docs/06-data-sources.md`](docs/06-data-sources.md). Contributions: [`CONTRIBUTING.md`](CONTRIBUTING.md).
