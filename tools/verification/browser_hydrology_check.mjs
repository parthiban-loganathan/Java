// Runs the catchment model through the TeaVM build and prints its summary, for comparison with
// the JVM (civiltech.lab.Cli).  Usage:
//   node tools/verification/browser_hydrology_check.mjs java/web-bridge/target/js/civiltech.js site/data/catchment
import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
const [modulePath, dataDir] = process.argv.slice(2);
const java = await import(pathToFileURL(path.resolve(modulePath)).href);
let t = performance.now();
const info = JSON.parse(java.loadTerrain(fs.readFileSync(path.join(dataDir, 'vellore_hills_grid.json'), 'utf8')));
console.log(`terrain ${info.rows} x ${info.cols} loaded and routed in ${(performance.now() - t).toFixed(0)} ms`);
t = performance.now();
const ws = JSON.parse(java.delineate(info.outletRow, info.outletCol, 0));
console.log(`delineated in ${(performance.now() - t).toFixed(0)} ms: area ${ws.areaKm2.toFixed(3)} km2`);
const rain = java.parseRainfallCsv(fs.readFileSync(path.join(dataDir, 'era5_vellore_hourly.csv'), 'utf8')).slice(312, 408);
t = performance.now();
const res = JSON.parse(java.simulate(Float64Array.from(rain), java.curveNumberTable(2), 10, 0, false, 0, 0.25, 30));
console.log(`simulated in ${(performance.now() - t).toFixed(0)} ms`);
const keys = ['areaKm2', 'weightedCurveNumber', 'tcHours', 'totalRainMm', 'totalExcessMm', 'peakScs', 'peakScsTimeHours',
  'peakTimeArea', 'volumeScsM3', 'volumeTimeAreaM3', 'excessVolumeM3', 'channelCount'];
console.log(JSON.stringify(Object.fromEntries(keys.map(k => [k, res[k]]))));
console.log('frames', java.channelFrames().length, 'channels', java.channelCells().length, 'class excess', java.frameClassExcess().length);
