// Controller of the catchment simulator: loads open data, calls the Java engine, draws results.
import { chrome, fetchText, fmt, escapeHtml, cssVar } from './common.js';
import { TerrainView, WORLDCOVER_NAMES, worldCoverCss, RAMPS, rampCss } from './terrain3d.js';

chrome('catchment.html');
const $ = id => document.getElementById(id);
const HOUR = 3600e3;
const IST = 5.5 * HOUR;
const VIT = [12.9692, 79.1559];

let java, grid, era5, view, map, chart;
let ws = null, mask = null, rain = null, result = null, anim = null;
let cnLookup = null, cnDescription = [];
let layers = {};

// ---------------------------------------------------------------- formatting helpers
const pad = n => String(n).padStart(2, '0');
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
function istLabel(ms) {
  const d = new Date(ms + IST);
  return `${pad(d.getUTCDate())} ${MONTHS[d.getUTCMonth()]} ${pad(d.getUTCHours())}:${pad(d.getUTCMinutes())}`;
}
function istInputValue(ms) {
  const d = new Date(ms + IST);
  return `${d.getUTCFullYear()}-${pad(d.getUTCMonth() + 1)}-${pad(d.getUTCDate())}T${pad(d.getUTCHours())}:${pad(d.getUTCMinutes())}`;
}
const timeLabel = h => (rain && rain.startMs != null) ? istLabel(rain.startMs + h * HOUR) + ' IST' : `hour ${fmt(h, 1)}`;

// ---------------------------------------------------------------- start-up
async function init() {
  const [gridText, era5Text, mod] = await Promise.all([
    fetchText('data/catchment/vellore_hills_grid.json'),
    fetchText('data/catchment/era5_precip_grid.json'),
    import('../java/civiltech.js'),
  ]);
  java = mod;
  grid = JSON.parse(gridText);
  era5 = JSON.parse(era5Text);
  era5.startEndMs = Date.parse(era5.start.replace('Z', ':00Z'));
  const info = JSON.parse(java.loadTerrain(gridText));
  java.loadRainfallGrid(era5Text);
  grid.defaultRow = info.outletRow;
  grid.defaultCol = info.outletCol;
  layers.accumulation = java.accumulations();
  layers.maxAccumulation = info.maxAccumulation;

  cnDescription = JSON.parse(java.curveNumberDescription());
  cnLookup = Float64Array.from(java.curveNumberTable(2));

  $('load-status').hidden = true;
  $('app').hidden = false;

  view = new TerrainView($('viewport'), {
    rows: grid.rows, cols: grid.cols, dx: grid.cellSizeX, dy: grid.cellSizeY,
    elevation: grid.elevation, landCover: grid.landCover,
  });
  view.setLayerData('accumulation', layers.accumulation);
  view.layerData.maxAccumulation = layers.maxAccumulation;
  view.onPick((r, c) => moveOutlet(r, c, 3));
  initMap();
  initChart();
  initEra5Inputs();
  bindControls();
  updateLegend();
  moveOutlet(grid.defaultRow, grid.defaultCol, 0);
}

// ---------------------------------------------------------------- catchment
function moveOutlet(row, col, snap) {
  try {
    ws = JSON.parse(java.delineate(row, col, snap));
  } catch (e) {
    $('run-status').textContent = 'Delineation failed: ' + e.message;
    return;
  }
  mask = java.catchmentMask();
  view.setMask(mask);
  view.setOutlet(ws.outletRow * grid.cols + ws.outletCol);
  $('ws-info').innerHTML = `
    <div>Outlet <b>${ws.outletLat.toFixed(5)} N, ${ws.outletLon.toFixed(5)} E</b> (row ${ws.outletRow}, col ${ws.outletCol})</div>
    <div>Area <b>${fmt(ws.areaKm2, 2)} km²</b> &middot; ${ws.cells.toLocaleString('en-IN')} cells</div>
    <div>Longest flow path <b>${fmt(ws.longestFlowPathM / 1000, 2)} km</b>, drop ${fmt(ws.longestPathDropM, 1)} m, slope ${fmt(ws.longestPathSlope, 4)}</div>
    <div>Elevation ${fmt(ws.outletElevation, 0)}-${fmt(ws.maxElevation, 0)} m (mean ${fmt(ws.meanElevation, 0)} m)</div>`;
  if (ws.areaKm2 < 0.1) {
    $('run-status').textContent = 'This outlet drains less than 0.1 km²; click closer to a stream line.';
  }
  drawMapCatchment();
  buildCnTable();
  refreshRainfall().then(run).catch(e => { $('rain-summary').innerHTML = `<span style="color:var(--bad)">${escapeHtml(e.message)}</span>`; });
}

// ---------------------------------------------------------------- rainfall sources
function initEra5Inputs() {
  const first = era5.startEndMs - HOUR;
  const s = $('era5-start');
  s.min = istInputValue(first);
  s.max = istInputValue(first + (era5.precip[0][0].length - 1) * HOUR);
  setEra5Window(312, 96);
}
function setEra5Window(index, hours) {
  $('era5-start').value = istInputValue(era5.startEndMs - HOUR + index * HOUR);
  $('era5-hours').value = hours;
}
function era5Index() {
  const ms = Date.parse($('era5-start').value + ':00+05:30');
  return Math.round((ms - (era5.startEndMs - HOUR)) / HOUR);
}

async function refreshRainfall() {
  const src = $('rain-source').value;
  if (src === 'era5') {
    const total = era5.precip[0][0].length;
    let i0 = era5Index();
    let n = Math.max(1, Math.round(+$('era5-hours').value || 1));
    if (!Number.isFinite(i0) || i0 < 0) i0 = 0;
    if (i0 >= total) i0 = total - 1;
    n = Math.min(n, total - i0);
    const values = Array.from(java.rainfallAt(ws.centroidLat, ws.centroidLon, i0, n));
    rain = { label: 'ERA5 reanalysis at the catchment centroid', startMs: era5.startEndMs - HOUR + i0 * HOUR, values, era5Index: i0 };
  } else if (src === 'manual') {
    const values = $('manual-values').value.split(/[\s,;]+/).filter(Boolean).map(Number);
    if (!values.length || values.some(v => !Number.isFinite(v) || v < 0)) throw new Error('Enter non-negative numbers only.');
    rain = { label: 'typed values', startMs: null, values };
  } else if (src === 'csv') {
    if (!rain || rain.source !== 'csv') throw new Error('Choose a CSV file.');
  } else if (src === 'openmeteo') {
    if (!rain || rain.source !== 'openmeteo') throw new Error('Press "Download" to fetch rainfall.');
  }
  rain.source = rain.source || src;
  showRainSummary();
  drawEra5Points();
}

function showRainSummary() {
  const v = rain.values;
  const total = v.reduce((a, b) => a + b, 0), max = Math.max(...v);
  $('rain-summary').innerHTML = `<b>${v.length} h</b>, total <b>${fmt(total, 1)} mm</b>, peak <b>${fmt(max, 1)} mm/h</b>` +
    (rain.startMs != null ? `<br><span class="muted">${istLabel(rain.startMs)} to ${istLabel(rain.startMs + v.length * HOUR)} IST</span>` : '') +
    `<br><span class="muted">${escapeHtml(rain.label)}</span>`;
}

async function fetchOpenMeteo() {
  const s = $('om-start').value, e = $('om-end').value;
  const url = `https://archive-api.open-meteo.com/v1/archive?latitude=${ws.centroidLat.toFixed(4)}&longitude=${ws.centroidLon.toFixed(4)}` +
    `&start_date=${s}&end_date=${e}&hourly=precipitation&timezone=GMT&models=era5`;
  $('rain-summary').innerHTML = '<span class="spinner"></span> downloading...';
  const r = await fetch(url);
  if (!r.ok) throw new Error(`Open-Meteo answered HTTP ${r.status}`);
  const j = await r.json();
  const times = j.hourly?.time, vals = j.hourly?.precipitation;
  if (!times || !vals || !times.length) throw new Error('No hourly data in the response.');
  const firstMissing = vals.findIndex(v => v === null);
  if (firstMissing >= 0) throw new Error(`Missing value at ${times[firstMissing]} UTC; choose an earlier end date.`);
  // Open-Meteo: value at time T is the sum over the preceding hour
  const startMs = Date.parse(times[0] + ':00Z') - HOUR;
  rain = { label: `Open-Meteo ERA5 at ${ws.centroidLat.toFixed(3)} N ${ws.centroidLon.toFixed(3)} E (CC BY 4.0)`, startMs, values: vals.map(Number), source: 'openmeteo' };
  showRainSummary();
}

function readCsv(file) {
  const reader = new FileReader();
  reader.onload = () => {
    try {
      const values = Array.from(java.parseRainfallCsv(String(reader.result)));
      rain = { label: `uploaded file ${file.name}`, startMs: null, values, source: 'csv' };
      showRainSummary();
      run();
    } catch (e) {
      $('rain-summary').innerHTML = `<span style="color:var(--bad)">Could not read the file: ${escapeHtml(e.message)}</span>`;
    }
  };
  reader.readAsText(file);
}

// ---------------------------------------------------------------- curve numbers
function buildCnTable() {
  const counts = {};
  for (let i = 0; i < mask.length; i++) if (mask[i]) counts[grid.landCover[i]] = (counts[grid.landCover[i]] || 0) + 1;
  const total = Object.values(counts).reduce((a, b) => a + b, 0);
  const rows = cnDescription.filter(d => counts[d.code]).sort((a, b) => counts[b.code] - counts[a.code]);
  $('cn-table').innerHTML = `<thead><tr><th>Land cover</th><th>Area</th><th>CN</th></tr></thead><tbody>` +
    rows.map(d => `<tr title="${escapeHtml(d.coverType)}">
      <td><span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:${worldCoverCss(d.code)};margin-right:6px"></span>${WORLDCOVER_NAMES[d.code]}</td>
      <td>${fmt(100 * counts[d.code] / total, 1)}%</td>
      <td><input type="number" min="1" max="100" step="1" data-code="${d.code}" value="${cnLookup[d.code]}"></td></tr>`).join('') + '</tbody>';
  $('cn-table').querySelectorAll('input').forEach(inp => inp.addEventListener('change', () => {
    const v = Number(inp.value);
    if (v > 0 && v <= 100) { cnLookup[+inp.dataset.code] = v; run(); }
    else inp.value = cnLookup[+inp.dataset.code];
  }));
}

// ---------------------------------------------------------------- simulation
let running = false;
async function run() {
  if (running || !rain) return;
  running = true;
  $('run').disabled = true;
  $('run-status').innerHTML = '<span class="spinner"></span> Java is computing...';
  await new Promise(r => setTimeout(r, 20));
  try {
    const step = +$('step').value;
    const frame = step >= 30 ? Math.max(step, 60) : 30;
    const t0 = performance.now();
    result = JSON.parse(java.simulate(Float64Array.from(rain.values), cnLookup, step, Math.max(0, +$('tc').value || 0),
      $('velocity').value === '1', Math.max(0, +$('clark').value || 0), 0.25, frame));
    const ms = performance.now() - t0;
    result.cellCN = java.cellCurveNumbers();
    result.travel = java.cellTravelTimes();
    result.channels = java.channelCells();
    result.frames = java.channelFrames();
    result.classExcess = java.frameClassExcess();
    $('run-status').textContent = `Computed in ${ms.toFixed(0)} ms. Volume check: unit hydrograph ${fmt(100 * result.volumeScsM3 / result.excessVolumeM3, 3)}%, time-area ${fmt(100 * result.volumeTimeAreaM3 / result.excessVolumeM3, 3)}% of runoff generated.`;
    prepareAnimation();
    showStats();
    updateChart();
    showTable();
    let maxT = 0;
    for (const t of result.travel) if (Number.isFinite(t) && t > maxT) maxT = t;
    view.layerData.maxTravel = maxT;
    view.setLayerData('cn', result.cellCN);
    view.setLayerData('travel', result.travel);
    updateLegend();
  } catch (e) {
    $('run-status').innerHTML = `<span style="color:var(--bad)">${escapeHtml(e.message || String(e))}</span>`;
  } finally {
    running = false;
    $('run').disabled = false;
  }
}

function showStats() {
  const r = result;
  const at = h => timeLabel(h);
  const cards = [
    ['Catchment area', fmt(r.areaKm2, 2), 'km²'],
    ['Area-weighted CN', fmt(r.weightedCurveNumber, 1), ''],
    [r.tcFromKirpich ? 'Tc (Kirpich)' : 'Tc (given)', fmt(r.tcHours, 2), 'h'],
    ['Rainfall', fmt(r.totalRainMm, 1), 'mm'],
    ['Direct runoff', fmt(r.totalExcessMm, 1), `mm (C = ${fmt(r.runoffCoefficient, 2)})`],
    ['Runoff volume', fmt(r.excessVolumeM3 / 1e6, 3), 'million m³'],
    ['Peak, SCS unit hydrograph', fmt(r.peakScs, 1), `m³/s at ${at(r.peakScsTimeHours)}`],
    [+$('clark').value > 0 ? 'Peak, time-area + Clark' : 'Peak, time-area', fmt(r.peakClark, 1), `m³/s at ${at(r.peakClarkTimeHours)}`],
  ];
  $('stats').innerHTML = cards.map(([l, v, u]) => `<div class="stat"><div class="label">${l}</div><div class="value">${v} <span class="unit">${u}</span></div></div>`).join('');
}

function showTable() {
  const r = result;
  const head = '<thead><tr><th>Hour ending</th><th>Rain (mm)</th><th>Runoff (mm)</th><th>Mean Q, SCS UH (m³/s)</th><th>Mean Q, time-area' +
    (+$('clark').value > 0 ? ' + Clark' : '') + ' (m³/s)</th></tr></thead>';
  const rows = r.hourlyRainMm.map((p, h) => `<tr><td>${timeLabel(h + 1)}</td><td>${fmt(p, 2)}</td><td>${fmt(r.hourlyExcessMm[h], 2)}</td>` +
    `<td>${fmt(r.hourlyMeanFlowScs[h], 2)}</td><td>${fmt(r.hourlyMeanFlowClark[h], 2)}</td></tr>`).join('');
  $('hourly').innerHTML = head + '<tbody>' + rows + '</tbody>';
}

function downloadCsv() {
  if (!result) return;
  const r = result;
  const lines = ['# Educational simulation, not calibrated. VIT Civil Engineering Innovation Lab catchment simulator.',
    `# Outlet ${ws.outletLat.toFixed(5)} N ${ws.outletLon.toFixed(5)} E, area ${r.areaKm2.toFixed(3)} km2, CN ${r.weightedCurveNumber.toFixed(1)}, Tc ${r.tcHours.toFixed(3)} h`,
    `# Rainfall: ${rain.label}`,
    'hour_ending,rain_mm,runoff_mm,mean_q_scs_m3s,mean_q_timearea_m3s'];
  r.hourlyRainMm.forEach((p, h) => lines.push([timeLabel(h + 1), p.toFixed(3), r.hourlyExcessMm[h].toFixed(3),
    r.hourlyMeanFlowScs[h].toFixed(3), r.hourlyMeanFlowClark[h].toFixed(3)].join(',')));
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob([lines.join('\n') + '\n'], { type: 'text/csv' }));
  a.download = 'catchment_simulation.csv';
  a.click();
  setTimeout(() => URL.revokeObjectURL(a.href), 1000);
}

// ---------------------------------------------------------------- chart
const markerPlugin = {
  id: 'timeMarker',
  afterDatasetsDraw(c) {
    if (anim == null || !result) return;
    const x = c.scales.x.getPixelForValue(anim.time);
    const { top, bottom } = c.chartArea;
    const ctx = c.ctx;
    ctx.save();
    ctx.strokeStyle = cssVar('--bad');
    ctx.setLineDash([4, 3]);
    ctx.beginPath(); ctx.moveTo(x, top); ctx.lineTo(x, bottom); ctx.stroke();
    ctx.restore();
  },
};

function initChart() {
  chart = new Chart($('hydrograph'), {
    data: { datasets: [] },
    options: {
      animation: false, maintainAspectRatio: false, parsing: false, normalized: true,
      interaction: { mode: 'nearest', axis: 'x', intersect: false },
      scales: {
        x: { type: 'linear', title: { display: true, text: 'time (IST)' }, ticks: { callback: v => timeLabel(v).replace(' IST', ''), maxRotation: 0, maxTicksLimit: 6 } },
        y: { position: 'left', title: { display: true, text: 'discharge (m³/s)' }, beginAtZero: true },
        rain: { position: 'right', reverse: true, title: { display: true, text: 'rain (mm/h)' }, beginAtZero: true, grid: { drawOnChartArea: false }, ticks: { callback: v => Number.isInteger(v) ? v : '' } },
      },
      plugins: { tooltip: { callbacks: { title: items => timeLabel(items[0].parsed.x) } } },
    },
    plugins: [markerPlugin],
  });
  restyleChart();
  document.addEventListener('themechange', () => { restyleChart(); chart.update(); });
}

function restyleChart() {
  const text = cssVar('--muted'), grid = cssVar('--border');
  Chart.defaults.color = text;
  for (const s of Object.values(chart.options.scales)) {
    s.ticks.color = text; s.title.color = text; s.grid = { ...(s.grid || {}), color: grid };
  }
  chart.options.plugins.legend = { labels: { color: text } };
}

function updateChart() {
  const r = result;
  const pts = arr => r.timeHours.map((t, i) => ({ x: t, y: arr[i] }));
  const rainPts = r.hourlyRainMm.map((p, h) => ({ x: h + 0.5, y: p }));
  const clark = +$('clark').value > 0;
  chart.data.datasets = [
    { type: 'bar', label: 'rain (mm/h)', data: rainPts, yAxisID: 'rain', backgroundColor: 'rgba(76, 140, 220, .45)', barPercentage: 1, categoryPercentage: 1, order: 3 },
    { type: 'line', label: 'SCS unit hydrograph', data: pts(r.flowScs), borderColor: '#d9480f', pointRadius: 0, borderWidth: 2, order: 1 },
    { type: 'line', label: 'time-area (translation)', data: pts(r.flowTimeArea), borderColor: '#0b6e8a', pointRadius: 0, borderWidth: 1.5, borderDash: clark ? [5, 3] : [], order: 2 },
  ];
  if (clark) chart.data.datasets.push({ type: 'line', label: `time-area + Clark (R = ${$('clark').value} h)`, data: pts(r.flowClark), borderColor: '#2b8a3e', pointRadius: 0, borderWidth: 2, order: 1 });
  chart.options.scales.x.min = 0;
  chart.options.scales.x.max = r.timeHours[r.timeHours.length - 1];
  chart.options.scales.rain.max = Math.ceil(Math.max(...r.hourlyRainMm) * 3) || 1;
  chart.update();
}

// ---------------------------------------------------------------- map
let mapLayers = {};
function initMap() {
  map = L.map('map', { scrollWheelZoom: false });
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 17, attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
  }).addTo(map);
  const south = grid.north - grid.rows * grid.cellDeg, east = grid.west + grid.cols * grid.cellDeg;
  L.rectangle([[south, grid.west], [grid.north, east]], { color: '#888', weight: 1, fill: false, dashArray: '4 4' })
    .bindTooltip('Extent of the elevation grid').addTo(map);
  L.circleMarker(VIT, { radius: 6, color: '#7048e8', fillOpacity: 0.9 }).bindTooltip('VIT Vellore campus').addTo(map);
  map.fitBounds([[south, grid.west], [grid.north, east]]);
  map.on('click', e => {
    const row = Math.floor((grid.north - e.latlng.lat) / grid.cellDeg);
    const col = Math.floor((e.latlng.lng - grid.west) / grid.cellDeg);
    if (row >= 0 && row < grid.rows && col >= 0 && col < grid.cols) moveOutlet(row, col, 3);
  });
}

function drawMapCatchment() {
  for (const k of ['boundary', 'outlet', 'centroid']) if (mapLayers[k]) map.removeLayer(mapLayers[k]);
  const segs = [], d = grid.cellDeg;
  for (let r = 0; r < grid.rows; r++) {
    for (let c = 0; c < grid.cols; c++) {
      const i = r * grid.cols + c;
      if (!mask[i]) continue;
      const n = grid.north - r * d, s = n - d, w = grid.west + c * d, e = w + d;
      if (r === 0 || !mask[i - grid.cols]) segs.push([[n, w], [n, e]]);
      if (r === grid.rows - 1 || !mask[i + grid.cols]) segs.push([[s, w], [s, e]]);
      if (c === 0 || !mask[i - 1]) segs.push([[n, w], [s, w]]);
      if (c === grid.cols - 1 || !mask[i + 1]) segs.push([[n, e], [s, e]]);
    }
  }
  mapLayers.boundary = L.polyline(segs, { color: '#e8590c', weight: 2 }).addTo(map);
  mapLayers.outlet = L.circleMarker([ws.outletLat, ws.outletLon], { radius: 7, color: '#c92a2a', fillOpacity: 1 })
    .bindTooltip(`Outlet, ${fmt(ws.areaKm2, 2)} km²`).addTo(map);
  mapLayers.centroid = L.circleMarker([ws.centroidLat, ws.centroidLon], { radius: 4, color: '#1c7ed6', fillOpacity: 1 })
    .bindTooltip('Catchment centroid (rainfall interpolation point)').addTo(map);
}

function drawEra5Points() {
  if (mapLayers.era5) map.removeLayer(mapLayers.era5);
  if (rain.source !== 'era5') return;
  const group = L.layerGroup();
  const n = rain.values.length, i0 = rain.era5Index;
  const totals = [];
  era5.lats.forEach((la, i) => era5.lons.forEach((lo, j) => {
    let s = 0;
    for (let k = i0; k < i0 + n; k++) s += era5.precip[i][j][k];
    totals.push([la, lo, s]);
  }));
  const max = Math.max(...totals.map(t => t[2])) || 1;
  for (const [la, lo, s] of totals) {
    L.circleMarker([la, lo], { radius: 3 + 14 * Math.sqrt(s / max), color: '#1971c2', weight: 1, fillOpacity: 0.35 })
      .bindTooltip(`ERA5 ${la.toFixed(2)} N ${lo.toFixed(2)} E: ${fmt(s, 1)} mm`).addTo(group);
  }
  mapLayers.era5 = group.addTo(map);
}

// ---------------------------------------------------------------- animation
function prepareAnimation() {
  const r = result;
  const nF = r.frameTimeHours.length, nC = r.channels.length, nK = r.classCurveNumber.length;
  let qMax = 0;
  for (const q of r.frames) if (q > qMax) qMax = q;
  const cellClass = new Int16Array(grid.rows * grid.cols).fill(-1);
  for (let i = 0; i < cellClass.length; i++) {
    if (mask[i]) cellClass[i] = r.classCurveNumber.indexOf(r.cellCN[i]);
  }
  let eMax = 0;
  for (let k = 0; k < nK; k++) eMax = Math.max(eMax, r.classExcess[(nF - 1) * nK + k]);
  anim = { time: 0, playing: false, nF, nC, nK, qMax: qMax || 1, eMax: eMax || 1, cellClass };
  view.setChannels(r.channels);
  $('time').max = r.frameTimeHours[nF - 1];
  $('time').step = 0.05;
  $('time').disabled = false;
  $('play').disabled = false;
  setTime(0);
}

function setTime(t) {
  const r = result, a = anim;
  a.time = t;
  const dtF = r.frameTimeHours[1] - r.frameTimeHours[0];
  const fpos = Math.min(a.nF - 1, t / dtF), f0 = Math.floor(fpos), f1 = Math.min(a.nF - 1, f0 + 1), w = fpos - f0;
  const q = new Float32Array(a.nC);
  for (let c = 0; c < a.nC; c++) {
    const v = (1 - w) * r.frames[f0 * a.nC + c] + w * r.frames[f1 * a.nC + c];
    q[c] = Math.sqrt(Math.max(0, v) / a.qMax);
  }
  view.setChannelLevels(q);
  const classFrac = new Float32Array(a.nK);
  for (let k = 0; k < a.nK; k++) classFrac[k] = ((1 - w) * r.classExcess[f0 * a.nK + k] + w * r.classExcess[f1 * a.nK + k]) / a.eMax;
  const ef = new Float32Array(a.cellClass.length);
  for (let i = 0; i < ef.length; i++) if (a.cellClass[i] >= 0) ef[i] = classFrac[a.cellClass[i]];
  view.setExcessFraction(ef);
  const hour = Math.floor(t);
  const intensity = hour < rain.values.length ? rain.values[hour] : 0;
  view.setRainIntensity(intensity);
  const si = Math.min(r.timeHours.length - 1, Math.round(t / r.stepHours));
  $('hud').innerHTML = `<b>${timeLabel(t)}</b><br>rain ${fmt(intensity, 1)} mm/h &middot; Q<sub>SCS</sub> <b>${fmt(r.flowScs[si], 1)}</b> m³/s &middot; ` +
    `Q<sub>time-area</sub> <b>${fmt(r.flowClark[si], 1)}</b> m³/s<br><span style="opacity:.75">blue tint: runoff generated so far; dots: stream discharge</span>`;
  $('time').value = t;
  $('time-label').textContent = timeLabel(t);
  chart.draw();
}

let lastTick = 0;
function tick(ts) {
  if (!anim || !anim.playing) return;
  if (lastTick) {
    const hoursPerSecond = Math.max(4, result.timeHours[result.timeHours.length - 1] / 25);
    let t = anim.time + (ts - lastTick) / 1000 * hoursPerSecond;
    if (t >= +$('time').max) { t = +$('time').max; togglePlay(false); }
    setTime(t);
  }
  lastTick = ts;
  requestAnimationFrame(tick);
}
function togglePlay(force) {
  if (!anim) return;
  anim.playing = force ?? !anim.playing;
  $('play').textContent = anim.playing ? 'Pause' : 'Play';
  if (anim.playing) {
    if (anim.time >= +$('time').max) setTime(0);
    lastTick = 0;
    requestAnimationFrame(tick);
  }
}

// ---------------------------------------------------------------- legend and controls
function updateLegend() {
  const layer = $('layer').value;
  let html = '';
  if (layer === 'landcover') {
    html = [10, 20, 30, 40, 50, 60, 80].map(c => `<div><span style="display:inline-block;width:10px;height:10px;background:${worldCoverCss(c)};margin-right:5px"></span>${WORLDCOVER_NAMES[c]}</div>`).join('');
  } else if (layer === 'elevation') {
    const zs = grid.elevation;
    html = `Elevation (m)<div class="bar" style="background:${rampCss(RAMPS.ELEV)}"></div>${fmt(Math.min(...zs), 0)} &ndash; ${fmt(Math.max(...zs), 0)}`;
  } else if (layer === 'accumulation') {
    html = `Upstream cells (log scale)<div class="bar" style="background:${rampCss(RAMPS.BLUES)}"></div>1 &ndash; ${layers.maxAccumulation.toLocaleString('en-IN')}`;
  } else if (layer === 'cn') {
    html = `Curve number<div class="bar" style="background:${rampCss(RAMPS.VIRIDIS)}"></div>30 &ndash; 100 (run first)`;
  } else {
    html = `Travel time to outlet<div class="bar" style="background:${rampCss([...RAMPS.VIRIDIS].reverse().map(([t, c]) => [1 - t, c]))}"></div>0 &ndash; ${fmt(view.layerData.maxTravel || 0, 2)} h`;
  }
  $('legend').innerHTML = html;
}

function bindControls() {
  $('run').addEventListener('click', async () => {
    try { await refreshRainfall(); await run(); }
    catch (e) { $('run-status').innerHTML = `<span style="color:var(--bad)">${escapeHtml(e.message)}</span>`; }
  });
  $('reset-outlet').addEventListener('click', () => moveOutlet(grid.defaultRow, grid.defaultCol, 0));
  $('rain-source').addEventListener('change', () => {
    for (const s of ['era5', 'openmeteo', 'csv', 'manual']) $('src-' + s).hidden = $('rain-source').value !== s;
  });
  document.querySelectorAll('[data-preset]').forEach(b => b.addEventListener('click', async () => {
    const [i, n] = b.dataset.preset.split(',').map(Number);
    setEra5Window(i, n);
    await refreshRainfall();
    run();
  }));
  $('om-fetch').addEventListener('click', async () => {
    try { await fetchOpenMeteo(); run(); }
    catch (e) { $('rain-summary').innerHTML = `<span style="color:var(--bad)">${escapeHtml(e.message)}</span>`; }
  });
  $('csv-file').addEventListener('change', e => { if (e.target.files[0]) readCsv(e.target.files[0]); });
  $('soil').addEventListener('change', () => {
    cnLookup = Float64Array.from(java.curveNumberTable(+$('soil').value));
    buildCnTable();
    run();
  });
  for (const id of ['step', 'velocity']) $(id).addEventListener('change', run);
  for (const id of ['tc', 'clark']) $(id).addEventListener('change', run);
  $('layer').addEventListener('change', () => { view.setLayer($('layer').value); updateLegend(); });
  $('exag').addEventListener('input', () => view.setExaggeration(+$('exag').value));
  $('play').addEventListener('click', () => togglePlay());
  $('time').addEventListener('input', () => { if (anim) { togglePlay(false); setTime(+$('time').value); } });
  $('download').addEventListener('click', downloadCsv);
}

init().catch(e => {
  $('load-status').className = 'notice bad small';
  $('load-status').textContent = 'Could not start: ' + e.message;
  console.error(e);
});
