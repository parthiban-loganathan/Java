// Cross-check helper for tools/lab/test_lab.py: runs the browser modules in Node.
//   node interop.mjs encrypt PUBLIC.pem INPUT.json   -> {armour, sha256, summary, replay}
//   node interop.mjs decrypt PRIVATE.pem PACKAGE.txt -> decrypted JSON
//   node interop.mjs summary EVENTS.json CODE.txt   -> {summary, replay}
import fs from 'node:fs';
import { encryptPackage, decryptPackage, keyId } from '../../site/assets/js/lab-crypto.js';
import { summarise, replay, fnv } from '../../site/assets/js/lab-activity.js';

const [cmd, a, b] = process.argv.slice(2);
if (cmd === 'encrypt') {
  const pem = fs.readFileSync(a, 'utf8');
  const obj = JSON.parse(fs.readFileSync(b, 'utf8'));
  const code = Object.values(obj.files)[0];
  const pkg = await encryptPackage(obj, pem);
  console.log(JSON.stringify({ ...pkg, keyId: await keyId(pem), summary: summarise(obj.activity.events, code),
    replay: replay(obj.activity.events), fnv: fnv(code) }));
} else if (cmd === 'decrypt') {
  const r = await decryptPackage(fs.readFileSync(b, 'utf8'), fs.readFileSync(a, 'utf8'));
  console.log(JSON.stringify(r));
} else if (cmd === 'summary') {
  const events = JSON.parse(fs.readFileSync(a, 'utf8'));
  const code = fs.readFileSync(b, 'utf8');
  console.log(JSON.stringify({ summary: summarise(events, code), replay: replay(events), fnv: fnv(code) }));
}
