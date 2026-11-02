# CivilTech Innovation Starter

The common starting point for every team of the **VIT Civil Engineering Innovation Lab**. It is a
complete, working web application whose engineering calculation is written in **Java**, translated
to JavaScript by **TeaVM**, and published on **GitHub Pages** by **GitHub Actions**: no server, no
paid service.

The worked example computes storm runoff (SCS Curve Number) for six sub-catchments of a real hill
catchment beside VIT, using rainfall of Cyclone Fengal (ERA5) and sub-catchments derived from the
Copernicus DEM and ESA WorldCover. Replace it with your own project.

## What is inside

```
src/main/java/app/
  SubCatchment.java       a class for the objects of the problem (fields, constructor, methods)
  RunoffCalculator.java   the calculation module: CSV text in, JSON text out
  WebApi.java             @JSExport methods that JavaScript can call
  ConsoleApp.java         the same calculation from the command line
src/test/java/app/        JUnit tests that compare the Java with hand calculations
web/
  index.html              introduction, data input, results dashboard, method and data sources
  app.js                  calls Java, draws cards, Chart.js charts, Leaflet map, Three.js view, table
  style.css               responsive layout, light and dark theme
  data/                   sample datasets (CSV)
  vendor/                 Chart.js, Leaflet, Three.js (local copies, no CDN needed)
pom.xml                   Maven build: tests + TeaVM
build.sh                  builds the site into dist/
.github/workflows/        test and deploy on every push
.devcontainer/            GitHub Codespaces configuration
```

## Start your project

1. On GitHub press **Use this template → Create a new repository** (public).
2. **Settings → Pages → Source: GitHub Actions**.
3. Push any change: after about two minutes your site is live at
   `https://<user>.github.io/<repository>/`.

## Work on it

In GitHub Codespaces (**Code → Codespaces → Create codespace**) or on a PC with JDK 21 and Maven:

```bash
mvn -q test
```

```bash
bash build.sh
python3 -m http.server 8000 --directory dist
```

Open `http://localhost:8000` (Codespaces opens the forwarded port for you).

Run the calculation without a browser:

```bash
mvn -q compile
java -cp target/classes app.ConsoleApp web/data/subcatchments.csv web/data/rainfall_fengal_2024.csv
```

## How Java and JavaScript talk

```java
@JSExport
public static String calculate(String subCatchmentCsv, String rainfallCsv) { ... }
```

```js
import { calculate } from './java/app.js';
const result = JSON.parse(calculate(subCsv, rainCsv));
```

Strings and numbers pass directly; return results as JSON text. Keep all engineering logic in
Java, and use JavaScript only for the page, charts, map and 3D view.

TeaVM supports most of `java.lang` and `java.util` but not, for example, `java.util.Scanner`,
`ObjectOutputStream` or JDBC, and `%n` in `printf`. Calculation code that takes its data as
parameters (as here) never needs them.

## Make it yours

Search for `CHANGE THIS` in `web/index.html` and `src/main/java/app/`. Replace the sample data in
`web/data/` with your own openly licensed data and list every source and licence on the page.

## Data in the example

* Sub-catchments: derived from Copernicus GLO-30 DEM (© DLR e.V. 2010-2014 and © Airbus Defence
  and Space GmbH 2014-2018, provided under COPERNICUS by the EU and ESA) and ESA WorldCover 2021
  v200 (CC BY 4.0); curve numbers from TR-55 for hydrologic soil group C (assumed).
* Rainfall: ERA5 hourly reanalysis (Hersbach et al., 2020); contains modified Copernicus Climate
  Change Service information 2024.

Licence: MIT.
