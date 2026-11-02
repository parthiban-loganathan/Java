// Compiles and runs every course program with the IN-BROWSER compiler (teavm-javac) inside
// Chromium, exactly as the Lab page does, and compares the output with the JDK reference.
//   node tools/verification/browser_javac_check.mjs SITE_URL COURSE_DIR [REPORT.md]
// SITE_URL must serve the built site including assets/javac/. Needs the playwright package.
import fs from 'node:fs';
import path from 'node:path';
import { chromium } from 'playwright';

const [siteUrl, courseDir, reportPath] = process.argv.slice(2);
const norm = s => s.replace(/\r\n/g, '\n').split('\n').map(l => l.trimEnd()).join('\n').trim();

const programs = [];
for (const group of ['lessons', 'experiments']) {
  for (const folder of fs.readdirSync(path.join(courseDir, group)).sort()) {
    const dir = path.join(courseDir, group, folder);
    const metaFile = path.join(dir, 'meta.properties');
    if (!fs.existsSync(metaFile)) continue;
    const meta = fs.readFileSync(metaFile, 'utf8');
    if (meta.includes('requires=mysql')) continue;
    const files = {};
    for (const f of fs.readdirSync(dir)) if (f.endsWith('.java')) files[f] = fs.readFileSync(path.join(dir, f), 'utf8');
    const cases = [];
    for (let c = 1; ; c++) {
      const suf = c === 1 ? '' : `-${c}`;
      const exp = path.join(dir, `expected${suf}.txt`);
      if (!fs.existsSync(exp)) break;
      const inp = path.join(dir, `input${suf}.txt`);
      cases.push({ input: fs.existsSync(inp) ? fs.readFileSync(inp, 'utf8') : '', expected: fs.readFileSync(exp, 'utf8') });
    }
    programs.push({ id: folder.split('-')[0], files, cases,
      differs: /browserDiffers=true/.test(meta) || /browser=false/.test(meta) || /browserJavac=false/.test(meta) });   // documented in meta.properties
  }
}

const browser = await chromium.launch();
const page = await browser.newPage();
page.on('pageerror', e => console.log('pageerror', e.message));
await page.goto(new URL('index.html', siteUrl).href);
const t0 = Date.now();
await page.evaluate(async () => {
  window.runner = await import('./assets/js/java-runner.js');
  await window.runner.warmUp();
});
console.log(`compiler loaded in ${Date.now() - t0} ms`);

const rows = [];
let pass = 0, fail = 0, known = 0;
for (const p of programs) {
  const r = await page.evaluate(async ({ files, cases }) => {
    const c0 = performance.now();
    const comp = await window.runner.compile(files);
    const compileMs = Math.round(performance.now() - c0);
    if (!comp.ok) return { compiled: false, compileMs, diagnostics: comp.diagnostics.map(d => `${d.fileName}:${d.lineNumber} ${d.message}`).join(' | ') };
    const results = [];
    for (const c of cases) {
      const out = await window.runner.run(comp.wasm, c.input, { timeLimitMs: 10000 });
      results.push({ ...out, expected: c.expected });
    }
    return { compiled: true, compileMs, results };
  }, p);
  let status, detail = '';
  if (!r.compiled) {
    status = 'COMPILE-FAIL';
    detail = r.diagnostics.slice(0, 300);
  } else {
    const bad = r.results.filter(x => norm(x.stdout) !== norm(x.expected));
    status = bad.length === 0 ? 'PASS' : `FAIL ${r.results.length - bad.length}/${r.results.length}`;
    if (bad.length) {
      const b = bad[0];
      detail = `exception=${b.exception} timedOut=${b.timedOut} got=${JSON.stringify(b.stdout.slice(0, 400))} want=${JSON.stringify(b.expected.slice(0, 400))}`;
    }
  }
  if (status === 'PASS') pass++;
  else if (p.differs) { known++; status += ' (documented difference)'; }
  else fail++;
  rows.push(`| ${p.id} | ${status} | ${r.compileMs} | ${detail.replace(/\|/g, '/')} |`);
  console.log(p.id, status, r.compileMs + 'ms', detail);
}
await browser.close();
const report = ['# In-browser compiler check', '', `${pass} passed, ${fail} failed, ${known} documented differences`, '',
  '| Program | Result | Compile ms | Detail |', '|---|---|---|---|', ...rows].join('\n') + '\n';
if (reportPath) fs.writeFileSync(reportPath, report);
console.log(`${pass} passed, ${fail} failed, ${known} documented differences`);
process.exit(fail ? 1 : 0);
