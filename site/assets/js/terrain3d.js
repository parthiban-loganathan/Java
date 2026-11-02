// Three.js view of the terrain grid, catchment, stream network, rain and runoff animation.
import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';

const WORLDCOVER = {
  10: [0x00, 0x64, 0x00], 20: [0xff, 0xbb, 0x22], 30: [0xff, 0xff, 0x4c], 40: [0xf0, 0x96, 0xff],
  50: [0xfa, 0x00, 0x00], 60: [0xb4, 0xb4, 0xb4], 70: [0xf0, 0xf0, 0xf0], 80: [0x00, 0x64, 0xc8],
  90: [0x00, 0x96, 0xa0], 95: [0x00, 0xcf, 0x75], 100: [0xfa, 0xe6, 0xa0],
};
export const WORLDCOVER_NAMES = {
  10: 'Tree cover', 20: 'Shrubland', 30: 'Grassland', 40: 'Cropland', 50: 'Built-up', 60: 'Bare / sparse',
  70: 'Snow and ice', 80: 'Water', 90: 'Herbaceous wetland', 95: 'Mangroves', 100: 'Moss and lichen',
};
export const worldCoverCss = c => `rgb(${(WORLDCOVER[c] || [128, 128, 128]).join(',')})`;

const SCALE = 1 / 100;           // 1 world unit = 100 m

function ramp(stops, t) {
  t = Math.min(1, Math.max(0, t));
  for (let i = 1; i < stops.length; i++) {
    if (t <= stops[i][0]) {
      const [t0, c0] = stops[i - 1], [t1, c1] = stops[i];
      const f = (t - t0) / (t1 - t0 || 1);
      return [0, 1, 2].map(k => c0[k] + f * (c1[k] - c0[k]));
    }
  }
  return stops[stops.length - 1][1];
}
const ELEV = [[0, [0.27, 0.52, 0.30]], [0.35, [0.62, 0.70, 0.38]], [0.65, [0.66, 0.55, 0.38]], [1, [0.93, 0.92, 0.90]]];
const BLUES = [[0, [0.86, 0.93, 0.97]], [0.5, [0.27, 0.62, 0.84]], [1, [0.03, 0.19, 0.42]]];
const VIRIDIS = [[0, [0.27, 0.0, 0.33]], [0.25, [0.23, 0.32, 0.55]], [0.5, [0.13, 0.57, 0.55]], [0.75, [0.37, 0.79, 0.38]], [1, [0.99, 0.91, 0.14]]];
export const RAMPS = { ELEV, BLUES, VIRIDIS };
export const rampCss = stops => `linear-gradient(90deg, ${stops.map(([t, c]) => `rgb(${c.map(v => Math.round(v * 255)).join(',')}) ${t * 100}%`).join(',')})`;

export class TerrainView {
  constructor(container, grid) {
    this.container = container;
    this.g = grid;               // {rows, cols, dx, dy, elevation, landCover}
    this.exaggeration = 3;
    this.layer = 'landcover';
    this.layerData = {};
    this.mask = null;
    this.excessFraction = null;   // per cell 0..1 at the current frame
    this.minZ = Math.min(...grid.elevation);
    this.maxZ = Math.max(...grid.elevation);

    this.renderer = new THREE.WebGLRenderer({ antialias: true, preserveDrawingBuffer: true });
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    container.appendChild(this.renderer.domElement);
    this.scene = new THREE.Scene();
    this.scene.background = new THREE.Color(0x0b1720);
    this.camera = new THREE.PerspectiveCamera(45, 1, 0.5, 5000);
    this.controls = new OrbitControls(this.camera, this.renderer.domElement);
    this.controls.enableDamping = true;
    this.controls.maxPolarAngle = Math.PI * 0.49;

    this.scene.add(new THREE.HemisphereLight(0xdfefff, 0x334433, 1.1));
    const sun = new THREE.DirectionalLight(0xffffff, 1.6);
    sun.position.set(-80, 120, -60);
    this.scene.add(sun);

    this.buildTerrain();
    this.buildRain();
    const w = grid.cols * grid.dx * SCALE, h = grid.rows * grid.dy * SCALE;
    this.camera.position.set(w * 0.1, Math.max(w, h) * 0.75, h * 0.95);
    this.controls.target.set(w * 0.05, 0, h * 0.05);

    this.pickCallback = null;
    this.bindPicking();
    new ResizeObserver(() => this.resize()).observe(container);
    this.resize();
    const loop = () => { this.controls.update(); this.animateRain(); this.renderer.render(this.scene, this.camera); requestAnimationFrame(loop); };
    loop();
  }

  worldX(col) { return (col - (this.g.cols - 1) / 2) * this.g.dx * SCALE; }
  worldZ(row) { return (row - (this.g.rows - 1) / 2) * this.g.dy * SCALE; }
  worldY(z) { return (z - this.minZ) * SCALE * this.exaggeration; }

  buildTerrain() {
    const { rows, cols } = this.g;
    const geo = new THREE.BufferGeometry();
    const pos = new Float32Array(rows * cols * 3);
    for (let r = 0; r < rows; r++) {
      for (let c = 0; c < cols; c++) {
        const i = r * cols + c;
        pos[3 * i] = this.worldX(c);
        pos[3 * i + 1] = this.worldY(this.g.elevation[i]);
        pos[3 * i + 2] = this.worldZ(r);
      }
    }
    const idx = new Uint32Array((rows - 1) * (cols - 1) * 6);
    let k = 0;
    for (let r = 0; r < rows - 1; r++) {
      for (let c = 0; c < cols - 1; c++) {
        const a = r * cols + c, b = a + 1, d = a + cols, e = d + 1;
        idx[k++] = a; idx[k++] = d; idx[k++] = b;
        idx[k++] = b; idx[k++] = d; idx[k++] = e;
      }
    }
    geo.setAttribute('position', new THREE.BufferAttribute(pos, 3));
    geo.setAttribute('color', new THREE.BufferAttribute(new Float32Array(rows * cols * 3), 3));
    geo.setIndex(new THREE.BufferAttribute(idx, 1));
    geo.computeVertexNormals();
    this.terrain = new THREE.Mesh(geo, new THREE.MeshLambertMaterial({ vertexColors: true }));
    this.scene.add(this.terrain);
    this.recolor();
  }

  setExaggeration(x) {
    this.exaggeration = x;
    const pos = this.terrain.geometry.attributes.position;
    for (let i = 0; i < this.g.elevation.length; i++) pos.array[3 * i + 1] = this.worldY(this.g.elevation[i]);
    pos.needsUpdate = true;
    this.terrain.geometry.computeVertexNormals();
    if (this.maskLines) this.setMask(this.mask);
    if (this.channelPoints) this.setChannels(this.channelCells);
    if (this.outletMarker) this.setOutlet(this.outletIndex);
  }

  setLayerData(name, values) { this.layerData[name] = values; if (this.layer === name) this.recolor(); }
  setLayer(name) { this.layer = name; this.recolor(); }

  baseColor(i) {
    const g = this.g;
    switch (this.layer) {
      case 'elevation': return ramp(ELEV, (g.elevation[i] - this.minZ) / (this.maxZ - this.minZ));
      case 'accumulation': {
        const a = this.layerData.accumulation;
        return a ? ramp(BLUES, Math.log(a[i]) / Math.log(this.layerData.maxAccumulation)) : [0.5, 0.5, 0.5];
      }
      case 'cn': {
        const cn = this.layerData.cn;
        return cn && cn[i] > 0 ? ramp(VIRIDIS, (cn[i] - 30) / 70) : null;
      }
      case 'travel': {
        const t = this.layerData.travel;
        return t && Number.isFinite(t[i]) ? ramp(VIRIDIS, 1 - t[i] / this.layerData.maxTravel) : null;
      }
      default: {
        const c = WORLDCOVER[g.landCover[i]] || [128, 128, 128];
        return [c[0] / 255, c[1] / 255, c[2] / 255];
      }
    }
  }

  recolor() {
    const col = this.terrain.geometry.attributes.color.array;
    const n = this.g.elevation.length;
    const water = [0.12, 0.45, 0.95];
    for (let i = 0; i < n; i++) {
      let c = this.baseColor(i);
      if (!c) c = ramp(ELEV, (this.g.elevation[i] - this.minZ) / (this.maxZ - this.minZ));
      let shade = 1;
      if (this.mask && !this.mask[i]) shade = 0.45;
      if (this.excessFraction && this.mask && this.mask[i]) {
        const f = Math.min(1, this.excessFraction[i]) * 0.75;
        c = [c[0] + f * (water[0] - c[0]), c[1] + f * (water[1] - c[1]), c[2] + f * (water[2] - c[2])];
      }
      col[3 * i] = c[0] * shade; col[3 * i + 1] = c[1] * shade; col[3 * i + 2] = c[2] * shade;
    }
    this.terrain.geometry.attributes.color.needsUpdate = true;
  }

  setMask(mask) {
    this.mask = mask;
    if (this.maskLines) { this.scene.remove(this.maskLines); this.maskLines.geometry.dispose(); }
    const { rows, cols, dx, dy } = this.g;
    const pts = [];
    const hx = dx * SCALE / 2, hz = dy * SCALE / 2;
    const y = i => this.worldY(this.g.elevation[i]) + 0.15;
    for (let r = 0; r < rows; r++) {
      for (let c = 0; c < cols; c++) {
        const i = r * cols + c;
        if (!mask[i]) continue;
        const x = this.worldX(c), z = this.worldZ(r), yy = y(i);
        if (r === 0 || !mask[i - cols]) pts.push(x - hx, yy, z - hz, x + hx, yy, z - hz);
        if (r === rows - 1 || !mask[i + cols]) pts.push(x - hx, yy, z + hz, x + hx, yy, z + hz);
        if (c === 0 || !mask[i - 1]) pts.push(x - hx, yy, z - hz, x - hx, yy, z + hz);
        if (c === cols - 1 || !mask[i + 1]) pts.push(x + hx, yy, z - hz, x + hx, yy, z + hz);
      }
    }
    const geo = new THREE.BufferGeometry();
    geo.setAttribute('position', new THREE.BufferAttribute(new Float32Array(pts), 3));
    this.maskLines = new THREE.LineSegments(geo, new THREE.LineBasicMaterial({ color: 0xffe066 }));
    this.scene.add(this.maskLines);
    this.recolor();
  }

  setOutlet(index) {
    this.outletIndex = index;
    if (!this.outletMarker) {
      this.outletMarker = new THREE.Mesh(new THREE.ConeGeometry(0.8, 3, 16), new THREE.MeshLambertMaterial({ color: 0xff3355 }));
      this.outletMarker.rotation.x = Math.PI;
      this.scene.add(this.outletMarker);
    }
    const r = Math.floor(index / this.g.cols), c = index % this.g.cols;
    this.outletMarker.position.set(this.worldX(c), this.worldY(this.g.elevation[index]) + 1.6, this.worldZ(r));
  }

  /** Stream cells drawn as points whose size and colour follow the discharge. */
  setChannels(cells) {
    this.channelCells = cells;
    if (this.channelPoints) { this.scene.remove(this.channelPoints); this.channelPoints.geometry.dispose(); }
    if (!cells || !cells.length) return;
    const n = cells.length;
    const pos = new Float32Array(n * 3);
    for (let k = 0; k < n; k++) {
      const i = cells[k], r = Math.floor(i / this.g.cols), c = i % this.g.cols;
      pos[3 * k] = this.worldX(c); pos[3 * k + 1] = this.worldY(this.g.elevation[i]) + 0.12; pos[3 * k + 2] = this.worldZ(r);
    }
    const geo = new THREE.BufferGeometry();
    geo.setAttribute('position', new THREE.BufferAttribute(pos, 3));
    geo.setAttribute('q', new THREE.BufferAttribute(new Float32Array(n), 1));
    const mat = new THREE.ShaderMaterial({
      transparent: true, depthWrite: false,
      uniforms: { pixelRatio: { value: this.renderer.getPixelRatio() } },
      vertexShader: `attribute float q; varying float vq; uniform float pixelRatio;
        void main() { vq = q; vec4 mv = modelViewMatrix * vec4(position, 1.0);
          gl_PointSize = pixelRatio * (2.0 + 9.0 * q) * (60.0 / -mv.z); gl_Position = projectionMatrix * mv; }`,
      fragmentShader: `varying float vq;
        void main() { vec2 d = gl_PointCoord - 0.5; float r = length(d); if (r > 0.5) discard;
          vec3 c = mix(vec3(0.55, 0.85, 1.0), vec3(0.05, 0.35, 1.0), vq);
          gl_FragColor = vec4(c, (0.35 + 0.65 * vq) * smoothstep(0.5, 0.25, r)); }`,
    });
    this.channelPoints = new THREE.Points(geo, mat);
    this.scene.add(this.channelPoints);
  }

  /** q01: discharge of every channel cell scaled to 0..1. */
  setChannelLevels(q01) {
    if (!this.channelPoints) return;
    const a = this.channelPoints.geometry.attributes.q;
    a.array.set(q01);
    a.needsUpdate = true;
  }

  setExcessFraction(f) { this.excessFraction = f; this.recolor(); }

  buildRain() {
    const N = 5000;
    const w = this.g.cols * this.g.dx * SCALE, h = this.g.rows * this.g.dy * SCALE;
    const pos = new Float32Array(N * 3);
    for (let i = 0; i < N; i++) {
      pos[3 * i] = (Math.random() - 0.5) * w;
      pos[3 * i + 1] = Math.random() * 60;
      pos[3 * i + 2] = (Math.random() - 0.5) * h;
    }
    const geo = new THREE.BufferGeometry();
    geo.setAttribute('position', new THREE.BufferAttribute(pos, 3));
    geo.setDrawRange(0, 0);
    this.rain = new THREE.Points(geo, new THREE.PointsMaterial({ color: 0xbfe6ff, size: 0.35, transparent: true, opacity: 0.75 }));
    this.rainCount = 0;
    this.scene.add(this.rain);
  }

  /** Rain particles proportional to intensity (mm/h), saturating at 15 mm/h. */
  setRainIntensity(mmPerHour) {
    this.rainCount = Math.round(Math.min(1, mmPerHour / 15) * 5000);
    this.rain.geometry.setDrawRange(0, this.rainCount);
  }

  animateRain() {
    if (!this.rainCount) return;
    const p = this.rain.geometry.attributes.position.array;
    for (let i = 0; i < this.rainCount; i++) {
      p[3 * i + 1] -= 0.9;
      if (p[3 * i + 1] < 0) p[3 * i + 1] += 60;
    }
    this.rain.geometry.attributes.position.needsUpdate = true;
  }

  onPick(cb) { this.pickCallback = cb; }

  bindPicking() {
    const el = this.renderer.domElement;
    let down = null;
    el.addEventListener('pointerdown', e => { down = [e.clientX, e.clientY]; });
    el.addEventListener('pointerup', e => {
      if (!down || Math.hypot(e.clientX - down[0], e.clientY - down[1]) > 5 || !this.pickCallback) return;
      const rect = el.getBoundingClientRect();
      const ndc = new THREE.Vector2(((e.clientX - rect.left) / rect.width) * 2 - 1, -((e.clientY - rect.top) / rect.height) * 2 + 1);
      const ray = new THREE.Raycaster();
      ray.setFromCamera(ndc, this.camera);
      const hit = ray.intersectObject(this.terrain)[0];
      if (!hit) return;
      const col = Math.round(hit.point.x / (this.g.dx * SCALE) + (this.g.cols - 1) / 2);
      const row = Math.round(hit.point.z / (this.g.dy * SCALE) + (this.g.rows - 1) / 2);
      if (row >= 0 && row < this.g.rows && col >= 0 && col < this.g.cols) this.pickCallback(row, col);
    });
  }

  resize() {
    const w = this.container.clientWidth, h = this.container.clientHeight;
    if (!w || !h) return;
    this.renderer.setSize(w, h, false);
    this.camera.aspect = w / h;
    this.camera.updateProjectionMatrix();
  }
}
