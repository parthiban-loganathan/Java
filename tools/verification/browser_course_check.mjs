// Runs every browser-enabled course program through the TeaVM-compiled module and compares the
// output with the JDK reference (expected*.txt).  Usage:
//   node tools/verification/browser_course_check.mjs java/web-bridge/target/js/civiltech.js course
import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const [modulePath, courseDir] = process.argv.slice(2);
const java = await import(pathToFileURL(path.resolve(modulePath)).href);
const norm = s => s.replace(/\r\n/g, '\n').split('\n').map(l => l.trimEnd()).join('\n').trim();
const run = (id, input) => new Promise(res => java.runProgram(id, input, (out, done) => res([out, done])));

let pass = 0, fail = 0, skipped = 0, known = 0;
for (const group of ['lessons', 'experiments']) {
  for (const folder of fs.readdirSync(path.join(courseDir, group)).sort()) {
    const dir = path.join(courseDir, group, folder);
    const metaFile = path.join(dir, 'meta.properties');
    if (!fs.existsSync(metaFile)) continue;
    const meta = Object.fromEntries(fs.readFileSync(metaFile, 'utf8').split('\n')
      .filter(l => l.includes('=')).map(l => [l.slice(0, l.indexOf('=')), l.slice(l.indexOf('=') + 1)]));
    const id = folder.split('-')[0];
    if (meta.browser !== 'true') { skipped++; console.log(`SKIP ${id} (JDK only)`); continue; }
    for (let c = 1; ; c++) {
      const suf = c === 1 ? '' : `-${c}`;
      const exp = path.join(dir, `expected${suf}.txt`);
      if (!fs.existsSync(exp)) break;
      const inpFile = path.join(dir, `input${suf}.txt`);
      const input = fs.existsSync(inpFile) ? fs.readFileSync(inpFile, 'utf8') : '';
      const sqlFile = path.join(dir, 'setup.sql');
      globalThis.labSql = fs.existsSync(sqlFile) ? fs.readFileSync(sqlFile, 'utf8') : '';   // as learn.js does
      const [out, done] = await run(id, input);
      const ok = norm(out) === norm(fs.readFileSync(exp, 'utf8'));
      if (ok && done) { pass++; console.log(`PASS ${id} case ${c}`); }
      else if (meta.browserDiffers === 'true') {
        known++; console.log(`DIFF ${id} case ${c} (documented in meta.properties)\n  browser: ${norm(out).replace(/\n/g, '\n           ')}`);
      } else {
        fail++; console.log(`FAIL ${id} case ${c} finished=${done}\n--- expected\n${norm(fs.readFileSync(exp, 'utf8'))}\n--- browser\n${norm(out)}`);
      }
    }
  }
}
console.log(`${pass} passed, ${fail} failed, ${known} documented differences, ${skipped} programs JDK-only`);
process.exit(fail ? 1 : 0);
