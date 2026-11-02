#!/usr/bin/env python3
"""Build the open-data inputs of the Vellore hill-catchment demonstration.

Sources (all open, no account required):
  * Copernicus GLO-30 DEM (1 arc-second), tile N12E079, AWS Open Data bucket
    `copernicus-dem-30m`. Licence: Copernicus DEM licence (free use with attribution).
  * ESA WorldCover 10 m 2021 v200, tile N12E078, AWS bucket `esa-worldcover`. Licence: CC BY 4.0.
  * ERA5 hourly reanalysis (Hersbach et al., 2020), large-scale (LSP, param 142) and convective
    (CP, param 143) precipitation; total precipitation = LSP + CP. NSF NCAR mirror on AWS
    (`nsf-ncar-era5`, dataset ds633.0). Licence: Copernicus Licence (free use with attribution).

Outputs (written to --out):
  vellore_hills_grid.json      2 arc-second terrain + land-cover grid (row-major, north row first)
  era5_precip_grid.json        hourly precipitation (mm) at ERA5 0.25 degree points around Vellore
  era5_vellore_hourly.csv      hourly precipitation bilinearly interpolated to the default outlet's catchment centroid

Run:  python3 prepare_vellore_catchment.py --work /tmp/era5work --out ../../site/data/catchment
Requires: numpy, rasterio, netCDF4, requests.
"""
import argparse
import datetime as dt
import json
import os
import sys

import numpy as np
import rasterio
import requests
from rasterio.enums import Resampling
from rasterio.transform import from_origin
from rasterio.warp import reproject

DEM_URL = ("https://copernicus-dem-30m.s3.amazonaws.com/Copernicus_DSM_COG_10_N12_00_E079_00_DEM/"
           "Copernicus_DSM_COG_10_N12_00_E079_00_DEM.tif")
WC_URL = ("/vsicurl/https://esa-worldcover.s3.eu-central-1.amazonaws.com/v200/2021/map/"
          "ESA_WorldCover_10m_2021_v200_N12E078_Map.tif")
ERA5_BASE = "https://nsf-ncar-era5.s3.amazonaws.com/e5.oper.fc.sfc.accumu/"
ERA5_FILES = {
    "LSP": ["202411/e5.oper.fc.sfc.accumu.128_142_lsp.ll025sc.2024111606_2024120106.nc",
            "202412/e5.oper.fc.sfc.accumu.128_142_lsp.ll025sc.2024120106_2024121606.nc"],
    "CP": ["202411/e5.oper.fc.sfc.accumu.128_143_cp.ll025sc.2024111606_2024120106.nc",
           "202412/e5.oper.fc.sfc.accumu.128_143_cp.ll025sc.2024120106_2024121606.nc"],
}

# Study window (degrees). Chosen so that the hill catchment south-east of VIT Vellore
# (outlet cell centre 12.942083 N, 79.260139 E, 38.65 km2) lies fully inside with a margin.
WEST, EAST, SOUTH, NORTH = 79.13, 79.30, 12.86, 12.985
ARCSEC = 1.0 / 3600.0
AGG = 2                       # 2 x 2 block mean of 1" pixels -> 2" cells (~60 m)
DEFAULT_OUTLET = (12.942083, 79.260139)   # centre of grid cell row 77, col 234
IST = dt.timedelta(hours=5, minutes=30)
EARTH_R = 6371008.8           # mean Earth radius (IUGG), m


def download(url, path):
    if os.path.exists(path) and os.path.getsize(path) > 0:
        return path
    print("downloading", url, file=sys.stderr)
    with requests.get(url, stream=True, timeout=120) as r:
        r.raise_for_status()
        with open(path + ".part", "wb") as f:
            for chunk in r.iter_content(1 << 20):
                f.write(chunk)
    os.replace(path + ".part", path)
    return path


def build_grid(work):
    dem_path = download(DEM_URL, os.path.join(work, "dem_N12E079.tif"))
    with rasterio.open(dem_path) as src:
        t = src.transform
        # pixel index of the window, snapped to the AGG block lattice of the tile
        c0 = int(np.floor((WEST - t.c) / t.a / AGG)) * AGG
        c1 = int(np.ceil((EAST - t.c) / t.a / AGG)) * AGG
        r0 = int(np.floor((t.f - NORTH) / -t.e / AGG)) * AGG
        r1 = int(np.ceil((t.f - SOUTH) / -t.e / AGG)) * AGG
        a = src.read(1, window=((r0, r1), (c0, c1))).astype(np.float64)
        rows, cols = a.shape[0] // AGG, a.shape[1] // AGG
        dem = a.reshape(rows, AGG, cols, AGG).mean(axis=(1, 3))
        west = t.c + c0 * t.a
        north = t.f + r0 * t.e
    cell_deg = AGG * ARCSEC
    grid_tr = from_origin(west, north, cell_deg, cell_deg)

    lc = np.zeros((rows, cols), dtype=np.uint8)
    with rasterio.open(WC_URL) as wc:
        reproject(rasterio.band(wc, 1), lc, dst_transform=grid_tr, dst_crs="EPSG:4326",
                  resampling=Resampling.mode)

    lat_c = north - rows * cell_deg / 2.0
    dy = EARTH_R * np.radians(cell_deg)
    dx = dy * np.cos(np.radians(lat_c))
    # relative dx variation across the window (spherical approximation, reported for transparency)
    dx_n = dy * np.cos(np.radians(north)); dx_s = dy * np.cos(np.radians(north - rows * cell_deg))
    return dict(dem=dem, lc=lc, west=west, north=north, cell_deg=cell_deg, rows=rows, cols=cols,
                dx=dx, dy=dy, dx_rel_var=abs(dx_s - dx_n) / dx)


def load_era5(work):
    import netCDF4
    out = {}
    for var, files in ERA5_FILES.items():
        times, vals = [], []
        for rel in files:
            p = download(ERA5_BASE + rel, os.path.join(work, os.path.basename(rel)))
            d = netCDF4.Dataset(p)
            lat = d.variables["latitude"][:]
            lon = d.variables["longitude"][:]
            la = np.where((lat <= 13.5) & (lat >= 12.25))[0]
            lo = np.where((lon >= 78.5) & (lon <= 80.0))[0]
            arr = np.asarray(d.variables[var][:, :, la[0]:la[-1] + 1, lo[0]:lo[-1] + 1], dtype=np.float64)
            init = d.variables["forecast_initial_time"][:]
            fh = d.variables["forecast_hour"][:]
            for i in range(len(init)):
                for k in range(len(fh)):
                    times.append(int(init[i]) + int(fh[k]))   # hours since 1900-01-01 (end of hour)
                    vals.append(arr[i, k])
            lats, lons = np.asarray(lat[la]), np.asarray(lon[lo])
            d.close()
        out[var] = (np.array(times), np.array(vals), lats, lons)
    t1, lsp, lats, lons = out["LSP"]
    t2, cp, _, _ = out["CP"]
    if not np.array_equal(t1, t2):
        raise RuntimeError("LSP and CP time axes differ")
    order = np.argsort(t1)
    tp_mm = np.clip((lsp + cp)[order] * 1000.0, 0.0, None)  # m -> mm; tiny negative packing noise -> 0
    return t1[order], tp_mm, lats, lons


def bilinear(lats, lons, field, lat, lon):
    """Bilinear interpolation on a regular lat/lon grid (lats descending)."""
    i = np.searchsorted(-lats, -lat) - 1
    j = np.searchsorted(lons, lon) - 1
    la0, la1, lo0, lo1 = lats[i], lats[i + 1], lons[j], lons[j + 1]
    u = (lon - lo0) / (lo1 - lo0)
    v = (la0 - lat) / (la0 - la1)
    return ((1 - u) * (1 - v) * field[..., i, j] + u * (1 - v) * field[..., i, j + 1]
            + (1 - u) * v * field[..., i + 1, j] + u * v * field[..., i + 1, j + 1])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--work", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--centroid", nargs=2, type=float, default=None,
                    help="lat lon used for the bundled rainfall CSV (default: centroid of the default "
                         "catchment as delineated by the Java core, 12.9230 79.2108)")
    args = ap.parse_args()
    os.makedirs(args.work, exist_ok=True)
    os.makedirs(args.out, exist_ok=True)

    g = build_grid(args.work)
    grid = {
        "name": "Vellore hills, Tamil Nadu, India",
        "description": "2 arc-second (~60 m) terrain and land-cover grid around a hill catchment south-east of VIT Vellore",
        "crs": "EPSG:4326",
        "rows": g["rows"], "cols": g["cols"],
        "west": round(g["west"], 10), "north": round(g["north"], 10), "cellDeg": g["cell_deg"],
        "cellSizeX": round(g["dx"], 3), "cellSizeY": round(g["dy"], 3),
        "cellSizeXRelativeVariation": round(float(g["dx_rel_var"]), 6),
        "defaultOutlet": {"lat": DEFAULT_OUTLET[0], "lon": DEFAULT_OUTLET[1]},
        "elevationUnits": "m above EGM2008 geoid (surface model: includes buildings and trees)",
        "landCoverLegend": {"10": "Tree cover", "20": "Shrubland", "30": "Grassland", "40": "Cropland",
                            "50": "Built-up", "60": "Bare / sparse vegetation", "70": "Snow and ice",
                            "80": "Permanent water bodies", "90": "Herbaceous wetland", "95": "Mangroves",
                            "100": "Moss and lichen"},
        "sources": [
            {"name": "Copernicus GLO-30 Digital Elevation Model", "tile": "N12E079",
             "url": "https://registry.opendata.aws/copernicus-dem/",
             "attribution": "Copernicus DEM - Global and European Digital Elevation Model (COP-DEM) GLO-30, "
                            "produced using Copernicus WorldDEM-30 (c) DLR e.V. 2010-2014 and (c) Airbus Defence "
                            "and Space GmbH 2014-2018 provided under COPERNICUS by the European Union and ESA; all rights reserved",
             "processing": "2 x 2 block mean of 1 arc-second pixels"},
            {"name": "ESA WorldCover 10 m 2021 v200", "tile": "N12E078",
             "url": "https://esa-worldcover.org/", "licence": "CC BY 4.0",
             "attribution": "(c) ESA WorldCover project 2021 / Contains modified Copernicus Sentinel data (2021) "
                            "processed by ESA WorldCover consortium",
             "processing": "majority (mode) resampling of 10 m classes to the 2 arc-second grid"},
        ],
        "elevation": [round(float(v), 1) for v in g["dem"].ravel()],
        "landCover": [int(v) for v in g["lc"].ravel()],
    }
    with open(os.path.join(args.out, "vellore_hills_grid.json"), "w") as f:
        json.dump(grid, f, separators=(",", ":"))

    t, tp, lats, lons = load_era5(args.work)
    base = dt.datetime(1900, 1, 1)
    times_utc = [(base + dt.timedelta(hours=int(h))) for h in t]
    era = {
        "name": "ERA5 hourly total precipitation (LSP + CP)",
        "units": "mm per hour; value is the accumulation over the hour ENDING at the time stamp",
        "timeZone": "UTC (add 5 h 30 min for IST)",
        "start": times_utc[0].strftime("%Y-%m-%dT%H:%MZ"),
        "stepHours": 1,
        "lats": [float(v) for v in lats], "lons": [float(v) for v in lons],
        "precip": [[[round(float(tp[k, i, j]), 3) for k in range(len(t))] for j in range(len(lons))]
                   for i in range(len(lats))],
        "source": {"name": "ERA5 hourly data on single levels (Hersbach et al., 2020)",
                   "doi": "10.24381/cds.adbb2d47",
                   "mirror": "NSF NCAR Research Data Archive ds633.0 on AWS Open Data (nsf-ncar-era5)",
                   "attribution": "Contains modified Copernicus Climate Change Service information 2024"},
    }
    steps = np.diff(t)
    if not np.all(steps == 1):
        raise RuntimeError("ERA5 time axis is not continuous hourly")
    with open(os.path.join(args.out, "era5_precip_grid.json"), "w") as f:
        json.dump(era, f, separators=(",", ":"))

    lat_c, lon_c = args.centroid if args.centroid else (12.9230, 79.2108)
    series = bilinear(lats, lons, tp, lat_c, lon_c)
    with open(os.path.join(args.out, "era5_vellore_hourly.csv"), "w") as f:
        f.write("# ERA5 hourly total precipitation bilinearly interpolated to %.4f N %.4f E\n" % (lat_c, lon_c))
        f.write("# Contains modified Copernicus Climate Change Service information 2024\n")
        f.write("hour_end_ist,hour_end_utc,precip_mm\n")
        for tu, v in zip(times_utc, series):
            f.write("%s,%s,%.3f\n" % ((tu + IST).strftime("%Y-%m-%d %H:%M"), tu.strftime("%Y-%m-%d %H:%M"), v))
    print("grid %d x %d, dx %.2f m, dy %.2f m; ERA5 %d hours; wrote %s" %
          (g["rows"], g["cols"], g["dx"], g["dy"], len(t), args.out), file=sys.stderr)


if __name__ == "__main__":
    main()
