// End-to-end check of the Java Lab page and the Teacher tools page in Chromium.
//   node tools/verification/lab_page_check.mjs SITE_URL [WORK_DIR]
// SITE_URL serves the built site. GitHub is simulated: requests to raw.githubusercontent.com get
// a control.json with an open session W99 and a freshly generated teacher key.
// Checks: identity, typing is recorded, paste and drop are blocked and logged, leaving the page is
// logged, the submission package decrypts to exactly the typed code and log, the issue link is
// right, saved work is restored after reload, practice mode cannot submit, the teacher page opens
// and replays the submission. If the in-browser compiler is deployed: Check passes both samples.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { chromium } from 'playwright';
import { generateKeyPair, decryptPackage } from '../../site/assets/js/lab-crypto.js';
import { replay, summarise } from '../../site/assets/js/lab-activity.js';

const [siteUrl, workArg] = process.argv.slice(2);
const work = workArg || fs.mkdtempSync(path.join(os.tmpdir(), 'labcheck-'));
const root = path.resolve(path.dirname(new URL(import.meta.url).pathname), '../..');
const failures = [];
const check = (ok, what) => { console.log(`${ok ? 'ok  ' : 'FAIL'} ${what}`); if (!ok) failures.push(what); };

const keys = await generateKeyPair();
fs.writeFileSync(path.join(work, 'lab-private-key.pem'), keys.privatePem);
const now = Date.now();
const iso = ms => new Date(ms).toISOString().replace(/\.\d+Z$/, 'Z');
const control = { format: 1, updated_at: iso(now), settings: { regnoPattern: '^[0-9]{2}[A-Z]{3}[0-9]{4}$' },
  sessions: [{ id: 'W99', title: 'Check', questions: ['Q01', 'Q02'], windows: [{ opened_at: iso(now - 3600e3), closes_at: iso(now + 3600e3), closed_at: null }] }] };

// The program, typed line by line without indentation (the editor indents, as for a student).
const program = fs.readFileSync(path.join(root, 'lab-system/gradebook/solutions/Q01/Main.java'), 'utf8')
  .split('\n').map(l => l.trim()).join('\n').trim();

const browser = await chromium.launch();
const context = await browser.newContext();
await context.route('https://raw.githubusercontent.com/**', route => {
  const p = new URL(route.request().url()).pathname.replace(/^\/[^/]+\/[^/]+\/[^/]+\//, '');
  if (p === 'lab/control.json') return route.fulfill({ body: JSON.stringify(control), contentType: 'application/json', headers: { 'access-control-allow-origin': '*' } });
  if (p === 'lab/teacher-public-key.pem') return route.fulfill({ body: keys.publicPem, headers: { 'access-control-allow-origin': '*' } });
  const local = path.join(root, p);
  if (p.startsWith('lab/questions/') && fs.existsSync(local)) return route.fulfill({ body: fs.readFileSync(local), headers: { 'access-control-allow-origin': '*' } });
  return route.fulfill({ status: 404, body: 'not found', headers: { 'access-control-allow-origin': '*' } });
});
const page = await context.newPage();
const errors = [];
page.on('pageerror', e => errors.push(e.message));
page.on('dialog', d => d.accept());

await page.goto(new URL('lab.html?s=W99&q=Q01', siteUrl).href);
await page.waitForSelector('#session-banner.ok');
check((await page.textContent('#session-banner')).includes('W99 is OPEN'), 'banner shows the open session');
await page.fill('#regno', '24bcl0123');
await page.fill('#name', 'Test Student');
await Promise.all([page.waitForNavigation(), page.click('#start')]);
await page.waitForSelector('#workspace:not([hidden])');
check((await page.textContent('#statement')).includes('rational method'), 'question Q01 statement shown');
check((await page.textContent('#statement')).includes('Peak runoff Q = 0.389 m3/s'), 'sample case shown');
check((await page.$$eval('#question option', o => o.map(x => x.value))).join(',') === 'Q01,Q02', 'session questions listed');

await page.click('#editor');
await page.keyboard.type(program);
const typed = await page.evaluate(() => ace.edit('editor').getValue());
check(typed.includes('Peak runoff Q = %.3f m3/s%n') && typed.split('\n').some(l => l.startsWith('        double c')), 'typed program in editor, auto-indented');

await page.evaluate(() => {
  const ta = document.querySelector('#editor textarea');
  const dt = new DataTransfer();
  dt.setData('text/plain', 'System.out.println("COPIED FROM ELSEWHERE");');
  ta.dispatchEvent(new ClipboardEvent('paste', { clipboardData: dt, bubbles: true, cancelable: true }));
});
await page.evaluate(() => {
  const dt = new DataTransfer();
  dt.setData('text/plain', 'DROPPED TEXT');
  document.querySelector('#editor .ace_content').dispatchEvent(new DragEvent('drop', { dataTransfer: dt, bubbles: true, cancelable: true }));
});
await page.evaluate(() => { window.dispatchEvent(new Event('blur')); window.dispatchEvent(new Event('focus')); });
const code = await page.evaluate(() => ace.edit('editor').getValue());
check(!code.includes('COPIED') && !code.includes('DROPPED'), 'paste and drop did not change the code');
check(code === typed, 'code unchanged after blocked attempts');
check((await page.textContent('.toast').catch(() => '')).length > 0, 'student told that pasting is not allowed');

const compilerMissing = (await page.textContent('#compiler-status')).includes('not installed');
if (!compilerMissing) {
  await page.waitForFunction(() => /ready|could not/.test(document.getElementById('compiler-status').textContent), null, { timeout: 120000 });
  check((await page.textContent('#compiler-status')).includes('ready'), 'in-browser compiler ready');
  await page.click('#check');
  await page.waitForFunction(() => /sample cases passed|does not compile/.test(document.body.textContent), null, { timeout: 120000 });
  check((await page.textContent('#check-results')).includes('2 of 2 sample cases passed'), 'Check: 2 of 2 sample cases passed');
  await page.fill('#stdin', '1.5\n10\n10\n');
  await page.click('#run');
  await page.waitForFunction(() => document.getElementById('run-info').textContent.endsWith('ms'), null, { timeout: 60000 });
  check((await page.textContent('#output')).trim() === 'Invalid runoff coefficient', 'Run with own input');
} else {
  console.log('note: in-browser compiler not deployed on this site; Run and Check not tested');
}

await page.click('#submit');
await page.waitForFunction(() => document.getElementById('package').value.includes('END LAB SUBMISSION'), null, { timeout: 30000 });
const armour = await page.inputValue('#package');
const href = await page.getAttribute('#open-github', 'href');
const u = new URL(href);
check(u.pathname.endsWith('/issues/new') && u.searchParams.get('template') === 'lab-submission.yml' &&
  u.searchParams.get('session') === 'W99' && u.searchParams.get('question') === 'Q01' &&
  u.searchParams.get('title') === '[Lab] W99 Q01' && /^[0-9a-f]{16}$/.test(u.searchParams.get('receipt')), 'issue link pre-fills the form');
const { data, sha256 } = await decryptPackage(armour, keys.privatePem);
check(u.searchParams.get('receipt') === sha256.slice(0, 16), 'receipt = package hash');
check(data.regno === '24BCL0123' && data.name === 'Test Student' && data.session === 'W99' && data.question === 'Q01', 'identity and question in package');
check(data.files['Main.java'] === code, 'package contains exactly the typed code');
const ev = data.activity.events;
const s = summarise(ev, code);
check(replay(ev) === code && s.replayMatches, 'typing log replays to the code');
check(s.pasteBlocked === 1 && ev.some(e => e[0] === 'p' && e[3].includes('COPIED FROM ELSEWHERE')), 'paste attempt recorded with its text');
check(s.dropBlocked === 1 && s.awayCount === 1 && s.bulkInserts === 0, 'drop and leaving the page recorded; no bulk insertions');
check(s.typedChars === s.insertedChars && s.typedChars >= program.replace(/\s/g, '').length, 'all characters were typed');
check(ev.some(e => e[0] === 's'), 'submission event recorded');
fs.writeFileSync(path.join(work, 'package.txt'), armour);
fs.writeFileSync(path.join(work, 'submission.json'), JSON.stringify({ meta: { issue: 1 }, ...data }));

await page.reload();
await page.waitForSelector('#workspace:not([hidden])');
check(await page.evaluate(() => ace.edit('editor').getValue()) === code, 'work restored after reload');
await page.goto(new URL('lab.html?s=practice&q=E01a', siteUrl).href);
await page.waitForSelector('#workspace:not([hidden])');
check(await page.isDisabled('#submit'), 'practice mode: Submit disabled');
check(await page.evaluate(() => ace.edit('editor').getValue()) === '', 'practice question starts empty');
check((await page.$$eval('#question option', o => o.length)) >= 30, 'practice lists the course programs');

const teacher = await context.newPage();
teacher.on('pageerror', e => errors.push('teacher: ' + e.message));
await teacher.goto(new URL('teacher.html', siteUrl).href);
await teacher.setInputFiles('#file-json', path.join(work, 'submission.json'));
await teacher.click('#open');
await teacher.waitForSelector('#view:not([hidden])');
check((await teacher.textContent('#final-code')) === code && (await teacher.textContent('#replay-code')) === code, 'teacher page: submission.json opens, replay = code');
check((await teacher.textContent('#flags')).includes('paste attempt'), 'teacher page flags the paste attempt');
await teacher.evaluate(() => { const s = document.getElementById('slider'); s.value = '0'; s.dispatchEvent(new Event('input')); });
check((await teacher.textContent('#replay-code')) === '', 'teacher page: replay at time 0 is empty');
await teacher.reload();
await teacher.setInputFiles('#file-key', path.join(work, 'lab-private-key.pem'));
await teacher.fill('#pkg', 'Issue text before\n' + armour + '\nafter');
await teacher.click('#open');
await teacher.waitForSelector('#view:not([hidden])');
check((await teacher.textContent('#who')).includes('24BCL0123 Test Student: W99 Q01'), 'teacher page decrypts a package with the private key');
await teacher.click('#gen');
await teacher.waitForSelector('#keys:not([hidden])', { timeout: 60000 });
check((await teacher.inputValue('#public-pem')).includes('BEGIN PUBLIC KEY') && /^[0-9a-f]{16}$/.test(await teacher.textContent('#kid')), 'teacher page creates a key pair');

check(errors.length === 0, 'no JavaScript errors' + (errors.length ? ': ' + errors.join(' | ') : ''));
await browser.close();
console.log(`${failures.length ? 'FAILED' : 'PASSED'}: ${failures.length} problem(s). Files in ${work}`);
process.exit(failures.length ? 1 : 0);
