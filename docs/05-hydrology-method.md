# 5. Catchment simulator: method and limitations

**Status: educational simulation.** The catchment has no stream gauge, so the model is neither
calibrated nor validated against observed discharge. It demonstrates standard methods with real
open data; its discharges are not design or warning values.

## Study area

| Item | Value | How it was obtained |
|------|-------|---------------------|
| Grid | 226 rows × 307 columns, 2 arc-seconds (60.21 m × 61.78 m) | 2 × 2 block mean of Copernicus GLO-30 (1 arc-second), window 12.86-12.985 N, 79.13-79.30 E |
| Default outlet | 12.942083 N, 79.260139 E (row 77, col 234) | steepest compact catchment within 20 km of VIT found by searching all stream cells |
| Catchment area | **38.650 km²** (10 391 cells) | D8 delineation by `civiltech-core` |
| Longest flow path | 15 414 m, drop 205.9 m, slope 0.01336 | idem |
| Land cover | trees 47.3 %, shrubland 32.9 %, cropland 13.8 %, built-up 4.0 %, grassland 1.8 %, bare 0.2 % | ESA WorldCover 2021, majority resampling |
| Area-weighted CN (soil group C) | 74.5 | TR-55 table, see below |
| Kirpich Tc | 2.87 h | equation below |

The delineated area, longest flow path and drop were reproduced exactly by an independent Python
implementation (`tools/verification/crosscheck_terrain.py`).

## 1. Terrain processing

1. **Depression filling**: Priority-Flood + ε (Barnes, Lehman and Mulla, 2014). Edge cells seed a
   priority queue; each cell is visited from its lowest neighbour and raised to the next
   representable floating-point value above it if it is not higher. Every interior cell then has a
   strictly lower neighbour, so flats drain.
2. **Flow direction**: D8 (O'Callaghan and Mark, 1984): steepest drop divided by distance to the
   eight neighbours (diagonal distance √(dx² + dy²)).
3. **Flow accumulation**: cells processed upstream-first (topological order, Kahn's algorithm).
4. **Delineation**: a cell belongs to the catchment if its receiver does (one downstream-first
   sweep); the same sweep gives each cell's flow length to the outlet.

The Copernicus DEM is a surface model (it includes buildings and trees). At 60 m this matters
little in the hills but can misplace flow paths in the built-up plain.

## 2. Time of concentration (Kirpich, 1940)

```text
Tc [min] = 0.0195 · L^0.77 · S^-0.385      L = longest flow path (m), S = drop / L (m/m)
```

Kirpich derived the equation from small (0.4 to 45 ha), steep agricultural catchments in Tennessee;
applying it to 38 km² is an extrapolation, so the page lets you override Tc.

## 3. Runoff generation: SCS Curve Number (USDA NRCS NEH-630, Chapter 10)

```text
S  = 25400 / CN − 254          potential maximum retention (mm)
Ia = 0.2 · S                   initial abstraction (mm)
Q  = (P − Ia)² / (P − Ia + S)  for P > Ia, else 0       (P, Q cumulative, mm)
```

* Applied **cell by cell** with the CN of each cell's land cover, then summed; this differs from
  applying one area-weighted CN because the equation is non-linear.
* Hourly rainfall is spread uniformly over each hour; the computation step (default 10 min) is the
  difference of cumulative runoff.
* Antecedent runoff condition II (average). λ = 0.2 as in NEH-630; studies of observed events
  (Woodward et al., 2003) suggest λ = 0.05 fits better, but then CN must be converted too.

### Curve numbers (TR-55, 1986, Tables 2-2a to 2-2c)

| WorldCover class | Assumed TR-55 cover type | A | B | C | D |
|------------------|-------------------------|---|---|---|---|
| 10 Tree cover | Woods, fair | 36 | 60 | 73 | 79 |
| 20 Shrubland | Brush, fair | 35 | 56 | 70 | 77 |
| 30 Grassland | Pasture/grassland/range, fair | 49 | 69 | 79 | 84 |
| 40 Cropland | Row crops, straight row, good | 67 | 78 | 85 | 89 |
| 50 Built-up | Residential, 1/8 acre or less (65 % impervious) | 77 | 85 | 90 | 92 |
| 60 Bare / sparse | Fallow, bare soil | 77 | 86 | 91 | 94 |
| 80 Water | *assumption*: impervious | 98 | 98 | 98 | 98 |
| 90 Wetland | *assumption*: pasture, good | 39 | 61 | 74 | 80 |

The class-to-cover-type mapping is a modelling assumption and is editable on the page. No soil
group map is bundled; the user chooses A-D. (Openly available global maps such as HYSOGs250m exist
and are a good extension project.)

## 4. Routing A: SCS curvilinear unit hydrograph (NEH-630, Chapter 16)

```text
lag       TL = 0.6 · Tc
time to peak   Tp = D/2 + TL           D = computation step
peak rate      qp = 0.208 · A / Tp     m³/s per mm of runoff (A in km², Tp in h; SI form of 484)
ordinates      q(t) = qp · f(t/Tp)     f = dimensionless ordinates of Table 16-1, linear interpolation
```

The ordinates are rescaled so the unit hydrograph contains exactly 1 mm over the catchment (the
tabulated curve integrates to 0.99969 of that for the default case), then convolved with the
excess series.

## 5. Routing B: distributed time-area translation (+ optional Clark reservoir)

Every cell's runoff travels along its D8 path to the outlet. Travel time of cell *i*:

* **uniform velocity** (default): Tᵢ = Tc · Lᵢ / Lmax, so the most remote cell arrives after Tc;
* **slope weighted**: segment times ∝ length / √slope (minimum slope 0.001), scaled so the
  longest travel time equals Tc.

Outlet discharge at time t is the sum over cells of their runoff generated at t − Tᵢ (computed as
exact centred step averages, so volume is conserved to machine precision). Pure translation does
not attenuate peaks; the optional **Clark** linear reservoir (Clark, 1945) with storage coefficient
R (h) adds attenuation. The reservoir equation is integrated exactly for linearly varying inflow:

```text
O₂ = I₂ − a(I₂ − I₁) + (O₁ − I₁ + a(I₂ − I₁))·k,   k = e^(−Δt/R), a = R/Δt
```

R cannot be estimated without observed hydrographs; R = 0 (no reservoir) is the default.

The **animation** shows routing B: blue tint = cumulative runoff generated in each cell; dots =
discharge through stream cells (upstream area ≥ 0.25 km²), computed by the same translation using
travel-time bins one step wide.

## 6. Rainfall

ERA5 hourly total precipitation (large-scale + convective) for 16 November - 16 December 2024,
0.25° grid, interpolated bilinearly to the catchment centroid. Cyclone Fengal window (29 Nov 11:30
to 3 Dec 11:30 IST, 96 h): **126.9 mm**, peak 11.0 mm/h at the default centroid. ERA5 is a
reanalysis at about 31 km resolution and smooths convective extremes; station records of the India
Meteorological Department, where accessible, would be the reference for validation.

## 7. Results for the default case (soil group C, 10-min step)

| Quantity | Value |
|----------|-------|
| Direct runoff | 61.5 mm (runoff coefficient 0.49), 2.378 million m³ |
| Peak, SCS unit hydrograph | 41.2 m³/s, 30 Nov 17:40 IST |
| Peak, time-area translation | 45.9 m³/s, 30 Nov 17:19 IST |
| Volume conservation | SCS 100.000 %, time-area 100.000 % of generated runoff |

Identical numbers are produced by the JVM (`java civiltech.lab.Cli ... --from 312 --hours 96`) and
by the browser build (`tools/verification/browser_hydrology_check.mjs`).

## 8. Main limitations

1. No calibration or validation data (ungauged).
2. Event model: no evaporation, baseflow, groundwater or soil-moisture accounting between storms.
3. Curve numbers from a global land-cover map and an assumed soil group.
4. Kirpich applied outside its calibration range.
5. D8 on a 60 m surface model; tanks (eris) and culverts of the region are not represented,
   although tanks strongly affect runoff in Tamil Nadu.
6. Reanalysis rainfall, uniform over the catchment.

## References

* Barnes, R., Lehman, C., Mulla, D. (2014). Priority-flood: an optimal depression-filling and
  watershed-labeling algorithm for digital elevation models. *Computers & Geosciences* 62, 117-127.
  doi:10.1016/j.cageo.2013.04.024
* Clark, C.O. (1945). Storage and the unit hydrograph. *Transactions of the ASCE* 110, 1419-1446.
* Hersbach, H., et al. (2020). The ERA5 global reanalysis. *Quarterly Journal of the Royal
  Meteorological Society* 146(730), 1999-2049. doi:10.1002/qj.3803
* Kirpich, Z.P. (1940). Time of concentration of small agricultural watersheds. *Civil Engineering*
  10(6), 362.
* O'Callaghan, J.F., Mark, D.M. (1984). The extraction of drainage networks from digital elevation
  data. *Computer Vision, Graphics, and Image Processing* 28(3), 323-344.
* USDA Soil Conservation Service (1986). *Urban Hydrology for Small Watersheds*, Technical Release
  55, 2nd ed.
* USDA NRCS. *National Engineering Handbook, Part 630 Hydrology*: Chapter 7 Hydrologic Soil Groups
  (2009), Chapter 10 Estimation of Direct Runoff from Storm Rainfall (2004), Chapter 15 Time of
  Concentration (2010), Chapter 16 Hydrographs (2007).
* Woodward, D.E., Hawkins, R.H., Jiang, R., Hjelmfelt, A.T., Van Mullem, J.A., Quan, Q.D. (2003).
  Runoff curve number method: examination of the initial abstraction ratio. *World Water &
  Environmental Resources Congress 2003*, ASCE.
