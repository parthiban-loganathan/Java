# 4. Build and submit your team project

## Stage plan (one semester)

| Week | Deliverable |
|------|-------------|
| 1-2 | Choose a problem from the **100 Problems** page (or your own) and press **Claim**; write the problem statement (one page): who has the problem, what decision your app supports, which open data exist. |
| 3 | Hand calculation for one realistic case, algorithm in plain language, flowchart. |
| 4-5 | Java calculation module with a test that reproduces the hand calculation (`mvn test`). |
| 6-7 | Web interface from the starter template: inputs, CSV upload, results table. |
| 8-9 | Charts, map, optional 3D; mobile check. |
| 10 | Deployment on GitHub Pages; README with method, data sources, limitations, team roles. |
| 11 | Submission through the website's **Submit** page. |
| 12 | Presentation and viva: every member explains part of the code. |

## Customise the starter template

| File | What to change |
|------|----------------|
| `src/main/java/app/SubCatchment.java` | Replace with the objects of your problem (a `ConcreteMix`, `Borehole`, `RoadSegment`, ...). |
| `src/main/java/app/RunoffCalculator.java` | Your calculation: read CSV text, compute, return JSON. |
| `src/main/java/app/WebApi.java` | Keep thin; rename `calculate` if you like and add more `@JSExport` methods. |
| `src/test/java/app/RunoffCalculatorTest.java` | Your hand-calculation tests. |
| `web/index.html` | Title, problem, objective, team, method, data sources. Sections marked `CHANGE THIS`. |
| `web/app.js` | Which inputs are read and how results are drawn (cards, charts, map, 3D, table). |
| `web/data/` | Your sample datasets (CSV/JSON), small and openly licensed. |

Remove sections you do not need (for example the 3D view) rather than leaving them empty.

## Problem ideas

The lab's **problem bank** has 100 worked problem statements with methods and open data (website page *100 Problems*, or `problems/README.md`). Further directions: urban flood hot spots on campus, rain-water harvesting potential of hostel roofs, concrete mix
design (IS 10262), quantity and cost estimation of a building, building energy and solar potential,
water-quality index of lakes, road-condition reporting, soil classification from lab results (IS
1498), disaster-risk mapping for Vellore district, waste-collection routing, smart campus
infrastructure. Original ideas are preferred.

## Submit to the lab directory

**Recommended (no Git knowledge needed):** use the **Submit** page of the lab website, described in
[3. GitHub for students](03-student-github-guide.md), part 7. It checks every field, builds the directory entry
and opens a pre-filled GitHub issue. A robot validates the entry; the teacher publishes it.

**Alternative for teams comfortable with pull requests:**

1. **Fork** the lab repository.
2. In your fork, edit `projects/projects.json` and add one entry (copy the one produced by the Submit page, or
   follow `projects/projects.schema.json`).
3. Open a pull request. The `CI` check validates the file; a maintainer reviews and merges.

Allowed categories and statuses are listed in `projects/projects.schema.json`. Example of a complete entry
(values illustrate the format only):

```json
{
  "id": "rooftop-rainwater-vit-hostels",
  "problemId": "P013",
  "title": "Rooftop rainwater harvesting for VIT hostels",
  "team": "Team 7, Batch 2026",
  "members": ["github-user-1", "github-user-2"],
  "academicYear": "2026-2027",
  "category": "Water resources",
  "status": "completed",
  "summary": "Estimates the annual harvestable volume of every hostel roof from open building footprints and ERA5 rainfall, and sizes storage tanks.",
  "javaRole": "Roof volumes, monthly tank mass balance and CSV parsing.",
  "repository": "https://github.com/github-user-1/rooftop-rainwater-vit",
  "demo": "https://github-user-1.github.io/rooftop-rainwater-vit/",
  "datasets": [
    {"name": "Google Open Buildings", "url": "https://sites.research.google/open-buildings/", "licence": "CC BY 4.0"}
  ],
  "tags": ["Java", "Chart.js", "Leaflet"]
}
```
