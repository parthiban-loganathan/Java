# 4. Build and submit your team project

## Stage plan (one semester)

| Week | Deliverable |
|------|-------------|
| 1-2 | Problem statement (one page): who has the problem, what decision your app supports, which open data exist. Open an issue in the lab repository using the *Project proposal* form. |
| 3 | Hand calculation for one realistic case, algorithm in plain language, flowchart. |
| 4-5 | Java calculation module with a test that reproduces the hand calculation (`mvn test`). |
| 6-7 | Web interface from the starter template: inputs, CSV upload, results table. |
| 8-9 | Charts, map, optional 3D; mobile check. |
| 10 | Deployment on GitHub Pages; README with method, data sources, limitations, team roles. |
| 11 | Pull request to the lab directory. |
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

## Good project ideas (examples, not assignments)

Urban flood hot spots on campus, rain-water harvesting potential of hostel roofs, concrete mix
design (IS 10262), quantity and cost estimation of a building, building energy and solar potential,
water-quality index of lakes, road-condition reporting, soil classification from lab results (IS
1498), disaster-risk mapping for Vellore district, waste-collection routing, smart campus
infrastructure. Original ideas are preferred.

## Submit to the lab directory

1. **Fork** the central lab repository (button *Fork* on GitHub).
2. In your fork, edit `projects/projects.json` and add one entry (copy an existing one):

```json
{
  "id": "smart-rainwater-harvesting",
  "title": "Rain-water harvesting potential of VIT hostels",
  "team": "Team 7, Batch 2026",
  "members": ["github-user-1", "github-user-2"],
  "academicYear": "2026-2027",
  "category": "Water resources",
  "status": "completed",
  "summary": "Estimates monthly harvestable volume for each hostel roof from IMD normals and roof areas measured from open satellite imagery.",
  "javaRole": "Roof catchment volumes, tank sizing by the mass-curve method, and CSV parsing.",
  "repository": "https://github.com/github-user-1/smart-rainwater-harvesting",
  "demo": "https://github-user-1.github.io/smart-rainwater-harvesting/",
  "datasets": [
    {"name": "Name of dataset", "url": "https://...", "licence": "CC BY 4.0"}
  ],
  "tags": ["Java", "Chart.js", "Leaflet"]
}
```

The example values above only illustrate the format; describe your actual project.

3. Commit to your fork and press **Contribute → Open pull request**.
4. The `CI` check validates your entry (allowed categories and statuses are listed in
   `projects/projects.schema.json`). Fix any reported error by committing again to the same branch.
5. A maintainer reviews and merges. Your project then appears on the lab website.
