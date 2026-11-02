# 1. Architecture and technology choices

## Goal

A public laboratory where 60 first-year civil engineering students (12 teams of 5) build working
applications for real problems, learn Java while doing it, and publish everything openly. It must
cost nothing, need almost no installation, and keep working after the original students graduate.

## Recommended architecture: one static website per project

```
                ┌──────────────────────────── GitHub (free, public) ─────────────────────────────┐
 student ──git──▶  team repository  ──GitHub Actions──▶  build: Maven + TeaVM  ──▶  GitHub Pages │
                │  (copy of the starter template)          (tests, Java → JS)        (live demo)  │
                │                                                                                 │
                │  central lab repository  ◀── pull request: projects/projects.json entry         │
                │  (course, demo, template, directory)  ──Actions──▶  lab website on GitHub Pages │
                └─────────────────────────────────────────────────────────────────────────────────┘
 visitor's browser: HTML/CSS/JS + Java-compiled-to-JavaScript + open data files (CSV/JSON)
```

Every application is a **static site**: HTML, CSS, JavaScript, data files and the Java code compiled
to JavaScript at build time. There is no server to run, patch, pay for or secure, so a project keeps
working for years after its team has moved on.

## Where Java runs, where JavaScript runs, how they talk

| Layer | Technology | Runs where |
|-------|-----------|------------|
| Engineering logic (terrain analysis, hydrology, course programs) | **Java 21** source | compiled by `javac`, then translated to one JavaScript ES module by **TeaVM 0.16** (Apache-2.0) during the build; executed in the browser |
| Same logic on a PC | Java on the JVM | `java civiltech.lab.Cli`, unit tests, lab exams |
| Same logic on a phone | Java compiled by the Android build | `android-app/` |
| Page structure and interaction | HTML, CSS, a thin layer of JavaScript | browser |
| 3D | Three.js 0.170 (MIT) | browser, WebGL |
| Charts | Chart.js 4.4 (MIT) | browser |
| Maps | Leaflet 1.9.4 (BSD-2-Clause) + OpenStreetMap tiles | browser |
| Data | CSV, JSON (GeoJSON-compatible coordinates) | static files beside the page |

**The boundary.** A Java method annotated `@JSExport` becomes a JavaScript function:

```java
// Java (java/web-bridge/src/main/java/lab/web/Bridge.java)
@JSExport
public static String delineate(int row, int col, int snapRadius) { ... return json; }
```

```js
// JavaScript (site/assets/js/catchment.js)
import * as java from '../java/civiltech.js';
const ws = JSON.parse(java.delineate(77, 234, 0));
```

Numbers and strings cross directly, numeric arrays cross as JavaScript typed arrays, and structured
results cross as JSON text. JavaScript never re-implements a calculation; it only draws.

Java is deliberately **not** used for the DOM, WebGL or charts: these are JavaScript's native domain
and forcing Java into them would add complexity without teaching value.

## Why TeaVM (and not the alternatives)

| Option | Licence / cost | Verdict |
|--------|---------------|---------|
| **TeaVM** (Java bytecode → JavaScript/WebAssembly, ahead of time) | Apache-2.0, free | **Chosen.** Small output (about 0.5 MB for the whole lab), fast, no server, supports threads (green threads), files (in-memory) and most of `java.util`. |
| CheerpJ (full JVM in the browser) | proprietary licence, hosted runtime | Runs unmodified JARs including `javac`, but is not open source and depends on a vendor CDN. |
| JWebAssembly, Bytecoder | open source | Smaller communities, less complete class library. |
| GWT / J2CL | Apache-2.0 | Work from Java *source*, heavy tool chains, less suited to beginners. |
| A Java server (Spring Boot etc.) | free software, but hosting costs | Needs a running server, accounts, money and maintenance. Not needed for these applications. |

### Verified TeaVM limitations (and how the lab handles them)

All 49 browser runs of the course programs (every test case of the 34 browser-capable programs) are compared automatically with the JDK (47 identical, 2 documented differences)
(`tools/verification/browser_course_check.mjs`). Findings:

| Gap in TeaVM 0.16 | Handling |
|-------------------|----------|
| No `java.util.Scanner` | `java/teavm-patches` adds a JDK-compatible `Scanner` that reads the page's input box. |
| `printf`: no `%n`; `%.2f` rounds half-even (0.405 → 0.40, JDK gives 0.41) | `teavm-patches` replaces `Formatter` with a patched copy (Apache-2.0) that supports `%n` and rounds like the JDK. |
| No object serialization (`ObjectOutputStream`) | Experiments 11 and 12 are marked *JDK only*; the page shows the JDK output. |
| No JDBC | Experiment 16 runs in Codespaces or on a lab PC with MySQL. |
| Array index and integer division by zero are not checked | Documented in Lesson 13; the JDK output is the reference. |
| `NumberFormatException` message wording differs | Documented in Experiment 6a. |

## Is a static GitHub Pages site sufficient?

For the demonstration and for most first-year projects, **yes**: calculations, visualisation,
CSV/JSON upload (read locally with the browser's FileReader, never uploaded anywhere), maps, 3D and
downloads all work without a server.

| Need | Static site? | Free solution when it is really needed |
|------|-------------|------------------------------------------|
| Calculations, charts, maps, 3D | yes | - |
| Reading a user's CSV/JSON file | yes (stays in the browser) | - |
| Fetching open APIs (Open-Meteo, ...) | yes, if the API allows cross-origin requests | - |
| Saving a user's work | only in that browser (`localStorage`) | export/import a file |
| Shared database, multi-user data | no | collect data as files through pull requests; or a free-tier backend (e.g. Supabase, Firebase) **only** with the teacher's approval, because of accounts and data-protection duties |
| Authentication | no | GitHub accounts for contributors; avoid storing personal data |
| Heavy processing (large rasters) | partly | pre-process with Python/Java in GitHub Actions and publish the result as a static file (this is how the lab's DEM and ERA5 data were prepared) |
| Secret API keys | no (anything in a static site is public) | use keyless open APIs |

## Free-tier facts that matter (checked November 2026)

* **GitHub Free** accounts and organizations: unlimited public repositories; **GitHub Pages** for
  public repositories; **GitHub Actions** on standard GitHub-hosted runners is free for public
  repositories.
* **GitHub Pages limits**: published site at most 1 GB; soft bandwidth limit 100 GB per month;
  deployments time out after 10 minutes; source repositories should stay around 1 GB. The
  10-builds-per-hour soft limit does not apply to sites deployed with a custom Actions workflow
  (as here). Pages is not intended for commercial use.
* **GitHub Codespaces** (browser-based VS Code): personal GitHub Free accounts include
  120 core-hours and 15 GB-month storage per month; GitHub Pro includes 180 core-hours and
  20 GB-month. A 2-core codespace therefore gives about 60 hours per month on the free plan.
  Students with the **GitHub Student Developer Pack** receive GitHub Pro. Stop codespaces when not
  in use; they also stop automatically after a period of inactivity.
* Nobody needs a paid account. Students without a laptop can do everything in Codespaces from a
  lab PC; students without internet at home can work offline with JDK 21 and VS Code.

### Hidden costs and risks

| Item | Mitigation |
|------|-----------|
| Codespaces hours run out | Use 2-core machines, stop them, prefer local JDK when available. |
| Large data files (> 100 MB per file is refused by GitHub; > 1 GB site fails) | Pre-process to the study area; store derived grids, not raw tiles. |
| OpenStreetMap tile policy | Fine for low-traffic educational use with attribution; heavy use requires another tile provider. |
| Live APIs change or disappear | Bundle a copy of the data used in the demo (as the lab does for ERA5). |
| Student personal data on a public site | Publish GitHub usernames and team names only; names only with consent. |
| Maintenance after graduation | Static sites, pinned library versions, automated tests in CI. |

## Repository layout (central lab)

```
course/            lessons L01-L14, experiments E01a-E16 (README, Java, inputs, JDK outputs)
java/              Maven build: civiltech-core (library + tests), teavm-patches, web-bridge (TeaVM)
site/              static website source (pages, CSS, JS, vendored libraries, open data)
starter-template/  CivilTech Innovation Starter (copy into a new repository)
android-app/       Android app reusing civiltech-core
projects/          project directory (JSON + schema)
tools/             build, verification and data-preparation scripts
docs/              these guides
.github/           CI, Pages deployment, templates
.devcontainer/     Codespaces / VS Code dev container (JDK 21, Maven, Node, MySQL)
```
