# 6. Data sources and licences

All data used by the lab are open, free and downloadable without an account. The preparation is
fully scripted: `tools/data-prep/prepare_vellore_catchment.py` downloads the originals and writes
the files in `site/data/catchment/`.

| Dataset | Used for | Source and access | Licence / attribution |
|---------|----------|-------------------|-----------------------|
| **Copernicus DEM GLO-30**, tile N12E079 | elevation grid | AWS Open Data `copernicus-dem-30m` (registry.opendata.aws/copernicus-dem); doi:10.5270/ESA-c5d3d65 | Free use with attribution: "produced using Copernicus WorldDEM-30 © DLR e.V. 2010-2014 and © Airbus Defence and Space GmbH 2014-2018 provided under COPERNICUS by the European Union and ESA; all rights reserved" |
| **ESA WorldCover 10 m 2021 v200**, tile N12E078 | land cover → curve numbers | AWS Open Data `esa-worldcover`; doi:10.5281/zenodo.7254221 (Zanaga et al., 2022) | CC BY 4.0: "© ESA WorldCover project 2021 / Contains modified Copernicus Sentinel data (2021) processed by ESA WorldCover consortium" |
| **ERA5 hourly data on single levels**, parameters LSP (142) and CP (143), Nov-Dec 2024 | hourly rainfall | NSF NCAR Research Data Archive ds633.0 mirror on AWS (`nsf-ncar-era5`); original: Copernicus Climate Data Store, doi:10.24381/cds.adbb2d47 | Copernicus licence: "Contains modified Copernicus Climate Change Service information 2024" |
| **Open-Meteo Historical Weather API** (optional live download in the simulator) | rainfall for any date | archive-api.open-meteo.com (ERA5-based) | CC BY 4.0, attribution "Weather data by Open-Meteo.com" |
| **OpenStreetMap** tiles | base map | tile.openstreetmap.org | © OpenStreetMap contributors, ODbL; follow the tile usage policy |

## Files in `site/data/catchment/`

| File | Content |
|------|---------|
| `vellore_hills_grid.json` | grid metadata, `elevation` (m, 0.1 m precision) and `landCover` (WorldCover code) arrays, row-major from the north-west corner |
| `era5_precip_grid.json` | hourly precipitation (mm) at 6 × 7 ERA5 points (12.25-13.5 N, 78.5-80.0 E), 720 hours from 2024-11-16 07:00 UTC (value = hour **ending** at the time stamp) |
| `era5_vellore_hourly.csv` | the same, bilinearly interpolated to the default catchment centroid (12.9230 N, 79.2108 E), with IST and UTC hour-ending time stamps |

`starter-template/web/data/subcatchments.csv` is derived from these files by
`tools/data-prep/SubCatchments.java` (tributary sub-catchments of the default catchment).

## Using other open data in your project

Good starting points: India-WRIS (water resources), Bhuvan (ISRO), data.gov.in (Open Government
Data Platform India, Government Open Data License - India), OpenStreetMap (Overpass API),
NASA POWER, Copernicus Data Space, USGS EarthExplorer, SoilGrids (ISRIC, CC BY 4.0), HYSOGs250m
(hydrologic soil groups, ORNL DAAC). Always record: dataset name, version, URL or DOI, access date,
licence, and what you changed.
