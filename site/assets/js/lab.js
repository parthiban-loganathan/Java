// Java Lab: students type a program, compile and run it in the browser (java-runner.js), check
// it against sample cases, and submit an encrypted package (lab-crypto.js) with the activity log
// (lab-activity.js) as a GitHub issue. Opening and closing is controlled by lab/control.json
// (written by the "Lab session control" workflow); GitHub's issue time decides acceptance.
import { chrome, fetchText, fetchJson, escapeHtml, renderMarkdown, safeGet, safeSet, LAB, REPO_URL, newIssueUrl } from './common.js';
import * as runner from './java-runner.js';
import { Recorder, summarise, fnv, FORMAT } from './lab-activity.js';
import { encryptPackage, keyId } from './lab-crypto.js';

chrome('lab.html');
const $ = id => document.getElementById(id);
const params = new URLSearchParams(location.search);
const MAX_ARMOUR = 60000;          // a GitHub issue body holds 65 536 characters
const DEFAULT_REGNO = '^[0-9]{2}[A-Z]{3}[0-9]{4}$';

let control = null, catalog = [], identity = null, question = null, mode = 'practice';
let editor = null, recorder = null, work = null, lastRun = null, lastCheck = null, compilerReady = false;

function syncTheme() {
  const dark = getComputedStyle(document.documentElement).colorScheme.includes('dark');
  $('hl-light').disabled = dark;
  $('hl-dark').disabled = !dark;
  if (editor) editor.setTheme(dark ? 'ace/theme/tomorrow_night' : 'ace/theme/chrome');
}
syncTheme();
document.addEventListener('themechange', syncTheme);

function toast(text) {
  const el = document.createElement('div');
  el.className = 'toast';
  el.textContent = text;
  document.body.append(el);
  setTimeout(() => el.remove(), 4000);
}

/** A file of the lab folder: fresh from GitHub, or the copy published with the website. */
async function labFile(path, json = false) {
  for (const url of [`${LAB.raw}lab/${path}?t=${Date.now()}`, `lab/${path}`]) {
    try {
      const r = await fetch(url, { cache: 'no-store' });
      if (r.ok) return json ? await r.json() : await r.text();
    } catch { /* try the next source */ }
  }
  return null;
}

// ---------- sessions (same rules as tools/lab/control.py) ----------
function windowEnd(w) {
  const ends = [w.closed_at, w.closes_at].filter(Boolean).map(Date.parse);
  return ends.length ? Math.min(...ends) : Infinity;
}
function openWindow(s, now = Date.now()) {
  return (s.windows || []).find(w => Date.parse(w.opened_at) <= now && now < windowEnd(w)) || null;
}
const timeText = ms => new Date(ms).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' });
const openSessions = () => (control?.sessions || []).filter(s => openWindow(s));

function banner() {
  const open = openSessions();
  const el = $('session-banner');
  if (!control) {
    el.className = 'notice warn';
    el.textContent = 'The lab status could not be loaded (no internet?). Practice still works; submissions need GitHub.';
  } else if (open.length) {
    el.className = 'notice ok';
    el.innerHTML = open.map(s => {
      const end = windowEnd(openWindow(s));
      return `<b>${escapeHtml(s.id)} is OPEN</b> for submissions` + (s.title ? `: ${escapeHtml(s.title)}` : '') +
        (Number.isFinite(end) ? ` (closes automatically at ${timeText(end)})` : ' (until the teacher closes it)');
    }).join('<br>') + '<br><span class="small">The time recorded by GitHub when you create the issue decides whether it is accepted.</span>';
  } else {
    el.className = 'notice';
    el.textContent = 'No assessment is open now. You can practise any course program; submissions open during the lab hour.';
  }
}

// ---------- identity ----------
function regnoPattern() {
  try { return new RegExp(control?.settings?.regnoPattern || DEFAULT_REGNO); } catch { return new RegExp(DEFAULT_REGNO); }
}
function loadIdentity() {
  try { identity = JSON.parse(safeGet('lab-identity')); } catch { identity = null; }
  if (identity) { $('regno').value = identity.regno; $('name').value = identity.name; }
}
$('identity').addEventListener('submit', e => {
  e.preventDefault();
  const regno = $('regno').value.trim().toUpperCase().replace(/\s+/g, '');
  const name = $('name').value.trim().replace(/\s+/g, ' ');
  if (!regnoPattern().test(regno)) { $('identity-msg').textContent = `Register number must look like 24BCL0123 (pattern ${regnoPattern().source}).`; return; }
  if (name.length < 3) { $('identity-msg').textContent = 'Enter your full name.'; return; }
  const changed = identity && identity.regno !== regno;
  identity = { regno, name };
  safeSet('lab-identity', JSON.stringify(identity));
  if (changed || !editor) location.reload(); else identityShown();
});
function identityShown() {
  $('identity-msg').innerHTML = identity ? `Working as <b>${escapeHtml(identity.regno)}</b> ${escapeHtml(identity.name)}` : 'Enter your register number and name, then press Start.';
}

// ---------- questions ----------
async function labQuestion(id) {
  const md = await labFile(`questions/${id}/README.md`);
  if (md === null) return null;
  const cases = [];
  for (let n = 1; n <= 10; n++) {
    const suf = n === 1 ? '' : `-${n}`;
    const exp = await labFile(`questions/${id}/expected${suf}.txt`);
    if (exp === null) break;
    cases.push({ input: (await labFile(`questions/${id}/input${suf}.txt`)) ?? '', expected: exp });
  }
  const title = (md.match(/^#\s+(.+)$/m) || [, id])[1];
  return { id, title, markdown: md, base: `lab/questions/${id}/`, cases, note: '' };
}
async function courseQuestion(p) {
  const base = `course/${p.path}/`;
  const cases = [];
  for (const suf of p.cases) {
    const exp = await fetchText(`${base}expected${suf}.txt`).catch(() => null);
    if (exp === null) continue;
    cases.push({ input: await fetchText(`${base}input${suf}.txt`).catch(() => ''), expected: exp });
  }
  return { id: p.id, title: p.title, markdown: await fetchText(base + 'README.md'), base, cases, note: p.browser ? '' : p.note };
}
function questionIds() {
  if (mode === 'practice') return catalog.filter(p => !p.requires).map(p => p.id);
  return (control.sessions.find(s => s.id === mode)?.questions) || [];
}
function titleOf(id) {
  const p = catalog.find(x => x.id === id);
  return p ? p.title : id;
}

// ---------- work saved per register number, mode and question ----------
const workKey = () => `lab-work:${identity.regno}:${mode}:${question.id}`;
let saveTimer = null;
function saveNow() {
  clearTimeout(saveTimer);
  saveTimer = null;
  work.code = editor.getValue();
  work.stdin = $('stdin').value;
  work.updated = Date.now();
  safeSet(workKey(), JSON.stringify(work));
}
function save() {
  clearTimeout(saveTimer);
  saveTimer = setTimeout(() => { saveNow(); stats(); }, 300);
}
window.addEventListener('pagehide', () => { if (work && editor) saveNow(); });
function stats() {
  const s = summarise(work.events, editor.getValue());
  const min = ms => (ms / 60000).toFixed(1);
  $('stats').innerHTML = [
    ['Typed characters', s.typedChars], ['Active typing', `${min(s.activeMs)} min`], ['Runs', s.runs],
    ['Best check', s.bestTotal ? `${s.bestPassed}/${s.bestTotal}` : '-'],
    ['Pastes blocked', s.pasteBlocked], ['Times left the page', s.awayCount],
  ].map(([k, v]) => `<span>${k}: <b>${v}</b></span>`).join('');
}

// ---------- compile, run, check ----------
function codeFiles() {
  const code = editor.getValue();
  return { [runner.fileNameFor(code)]: code };
}
function showDiagnostics(diags) {
  editor.session.setAnnotations(diags.filter(d => d.lineNumber > 0).map(d => ({
    row: d.lineNumber - 1, column: Math.max(0, (d.columnNumber || 1) - 1), text: d.message,
    type: /error/i.test(d.severity) ? 'error' : 'warning' })));
}
function diagText(diags) {
  return diags.map(d => (d.lineNumber > 0 ? `Line ${d.lineNumber}: ` : '') + d.message).join('\n');
}
async function compile() {
  $('file-name').textContent = runner.fileNameFor(editor.getValue());
  const c = await runner.compile(codeFiles());
  showDiagnostics(c.diagnostics || []);
  return c;
}
function busy(on, text = '') {
  for (const b of ['run', 'check']) $(b).disabled = on || !compilerReady;
  $('run-info').textContent = text;
}
function outcome(r) {
  return r.timedOut ? 'timeout' : r.exception ? 'exception' : 'ok';
}
function render(r) {
  const parts = [escapeHtml(r.stdout)];
  if (r.stderr) parts.push(`<span class="err">${escapeHtml(r.stderr)}</span>`);
  if (r.exception) parts.push(`<span class="err">Program stopped with an error: ${escapeHtml(r.exception)}</span>`);
  if (r.timedOut) parts.push('<span class="err">Stopped: time limit reached (endless loop, or waiting for more keyboard input than given).</span>');
  if (r.outputLimited) parts.push('<span class="err">Stopped: too much output.</span>');
  return parts.filter(Boolean).join('\n') || '(no output)';
}
$('run').addEventListener('click', async () => {
  busy(true, 'Compiling...');
  $('check-results').innerHTML = '';
  const t0 = performance.now();
  const c = await compile();
  if (!c.ok) {
    recorder.event('x', 'compile-error', Math.round(performance.now() - t0));
    $('output').innerHTML = `<span class="err">The program does not compile.\n${escapeHtml(diagText(c.diagnostics))}</span>`;
    busy(false, '');
    return;
  }
  busy(true, 'Running...');
  const r = await runner.run(c.wasm, $('stdin').value, { timeLimitMs: 5000 });
  lastRun = { at: new Date().toISOString(), outcome: outcome(r), ms: r.ms, stdin: $('stdin').value.slice(0, 2000), stdout: r.stdout.slice(0, 4000) };
  recorder.event('x', outcome(r), r.ms);
  $('output').innerHTML = render(r);
  busy(false, `${r.ms} ms`);
});
$('check').addEventListener('click', async () => {
  if (!question.cases.length) { $('check-results').innerHTML = '<p class="small muted">This question has no sample cases.</p>'; return; }
  busy(true, 'Compiling...');
  const c = await compile();
  if (!c.ok) {
    recorder.event('x', 'compile-error', 0);
    $('output').innerHTML = `<span class="err">The program does not compile.\n${escapeHtml(diagText(c.diagnostics))}</span>`;
    busy(false, '');
    return;
  }
  let passed = 0;
  const rows = [];
  for (const [i, k] of question.cases.entries()) {
    busy(true, `Case ${i + 1} of ${question.cases.length}...`);
    const r = await runner.run(c.wasm, k.input, { timeLimitMs: 5000 });
    const ok = !r.timedOut && runner.normalise(r.stdout) === runner.normalise(k.expected);
    passed += ok;
    rows.push(`<div class="case"><b>Sample case ${i + 1}: </b>${ok ? '<span class="badge ok">passed</span>' : '<span class="badge warn">different output</span>'}
      ${ok ? '' : `<div class="cols"><div><span class="small muted">Input</span><pre>${escapeHtml(k.input || '(none)')}</pre></div><div></div>
        <div><span class="small muted">Expected</span><pre>${escapeHtml(k.expected)}</pre></div>
        <div><span class="small muted">Your program</span><pre>${render(r)}</pre></div></div>`}</div>`);
  }
  lastCheck = { at: new Date().toISOString(), passed, total: question.cases.length };
  recorder.event('c', passed, question.cases.length);
  $('check-results').innerHTML = `<p><b>${passed} of ${question.cases.length} sample cases passed.</b>
    <span class="small muted">The teacher also uses other, hidden cases.</span></p>` + rows.join('');
  busy(false, '');
});

// ---------- submission ----------
let publicKey = null;
async function submit() {
  const session = control?.sessions?.find(s => s.id === mode);
  if (!session || !openWindow(session)) { toast('This assessment is not open now.'); return; }
  const code = editor.getValue();
  if (code.trim().length < 20) { toast('Type your program first.'); return; }
  if (!publicKey) publicKey = await labFile('teacher-public-key.pem');
  if (!publicKey || !publicKey.includes('BEGIN PUBLIC KEY')) { toast('The teacher has not published the submission key yet (lab/teacher-public-key.pem).'); return; }
  if (!confirm(`Submit ${question.id} for ${mode} as ${identity.regno} ${identity.name}?`)) return;
  recorder.event('s', fnv(code));
  const summary = summarise(work.events, code);
  const payload = {
    format: FORMAT, kind: 'lab-submission', session: mode, question: question.id,
    regno: identity.regno, name: identity.name, files: codeFiles(),
    createdAt: new Date().toISOString(), timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    userAgent: navigator.userAgent, page: location.origin + location.pathname,
    activity: { format: FORMAT, t0: new Date(work.t0).toISOString(), events: work.events, trimmed: false },
    summary, lastRun, lastCheck,
  };
  let pkg = await encryptPackage(payload, publicKey);
  if (pkg.armour.length > MAX_ARMOUR) {
    payload.activity = { ...payload.activity, events: work.events.filter(e => e[0] !== 'i' && e[0] !== 'r'), trimmed: true };
    pkg = await encryptPackage(payload, publicKey);
  }
  if (pkg.armour.length > MAX_ARMOUR) { toast('The program is too long to submit (more than about 40 000 characters).'); return; }
  const receipt = pkg.sha256.slice(0, 16);
  const fields = { session: mode, question: question.id, receipt };
  if (pkg.armour.length < 5000) fields.package = pkg.armour;
  $('package').value = pkg.armour;
  $('receipt').innerHTML = `Receipt <code>${receipt}</code> (key ${await keyId(publicKey)}). The reply on GitHub shows the same receipt.`;
  $('open-github').href = newIssueUrl('lab-submission.yml', `[Lab] ${mode} ${question.id}`, fields);
  $('my-submissions').href = `${REPO_URL}/issues?q=${encodeURIComponent('is:issue author:@me label:lab-submission')}`;
  $('submit-panel').hidden = false;
  $('submit-panel').scrollIntoView({ behavior: 'smooth' });
  await copyPackage();
}
async function copyPackage() {
  try {
    await navigator.clipboard.writeText($('package').value);
    toast('Package copied. Now press Open GitHub and paste it into the Package box.');
    document.querySelector('.toast').style.background = 'var(--ok)';
  } catch {
    $('package').focus();
    $('package').select();
    toast('Copy did not work automatically: the package is selected, press Ctrl+C.');
  }
}
$('copy-package').addEventListener('click', copyPackage);
$('submit').addEventListener('click', () => submit().catch(e => toast('Could not make the package: ' + e.message)));
$('forget').addEventListener('click', () => {
  if (!confirm('Remove all your saved Java Lab work (every question) from this browser?')) return;
  try {
    for (const k of Object.keys(localStorage)) if (k.startsWith(`lab-work:${identity.regno}:`)) localStorage.removeItem(k);
    localStorage.removeItem('lab-identity');
  } catch { /* storage unavailable */ }
  location.href = location.pathname;
});

// ---------- start ----------
async function selectors() {
  const open = openSessions();
  $('mode').innerHTML = open.map(s => `<option value="${escapeHtml(s.id)}">${escapeHtml(s.id)}${s.title ? ': ' + escapeHtml(s.title) : ''} (open)</option>`).join('') +
    '<option value="practice">Practice (no submission)</option>';
  const wanted = params.get('s');
  mode = open.some(s => s.id === wanted) || wanted === 'practice' ? wanted : (open[0]?.id || 'practice');
  $('mode').value = mode;
  $('mode-help').textContent = mode === 'practice'
    ? 'Practice: every course program, with its sample cases. Nothing is submitted.'
    : 'Assessment: write each question, check it, then Submit. You can submit again until it closes.';
  const ids = questionIds();
  const titles = await Promise.all(ids.map(async id => (catalog.some(p => p.id === id) ? titleOf(id) : ((await labFile(`questions/${id}/README.md`)) || '').match(/^#\s+(.+)$/m)?.[1] || id)));
  $('question').innerHTML = ids.map((id, i) => `<option value="${escapeHtml(id)}">${escapeHtml(id)}: ${escapeHtml(titles[i].replace(/^Experiment [0-9a-c]+: /, ''))}</option>`).join('');
  const q = ids.includes(params.get('q')) ? params.get('q') : ids[0];
  $('question').value = q;
  const go = () => { location.search = new URLSearchParams({ s: $('mode').value, q: $('mode').value === mode ? $('question').value : '' }).toString(); };
  $('mode').addEventListener('change', go);
  $('question').addEventListener('change', go);
  return q;
}

async function main() {
  [control, catalog] = await Promise.all([labFile('control.json', true), fetchJson('course/catalog.json')]);
  banner();
  setInterval(async () => { const c = await labFile('control.json', true); if (c) { control = c; banner(); updateSubmit(); } }, 60000);
  loadIdentity();
  identityShown();
  const qid = await selectors();
  if (!identity || !qid) return;

  const p = catalog.find(x => x.id === qid);
  question = p ? await courseQuestion(p) : await labQuestion(qid);
  if (!question) { $('session-banner').textContent = `Question ${qid} was not found in lab/questions/.`; return; }
  $('statement').innerHTML = (question.note ? `<div class="notice warn small">${escapeHtml(question.note)}</div>` : '') +
    `<div class="prose">${renderMarkdown(question.markdown, question.base)}</div>` +
    question.cases.map((k, i) => `<h3>Sample case ${i + 1}</h3><div class="small muted">Input</div><pre>${escapeHtml(k.input || '(none)')}</pre>
      <div class="small muted">Expected output</div><pre>${escapeHtml(k.expected)}</pre>`).join('');
  $('workspace').hidden = false;

  ace.config.set('basePath', 'assets/vendor/ace/');
  editor = ace.edit('editor', {
    mode: 'ace/mode/java', tabSize: 4, useSoftTabs: true, showPrintMargin: false,
    behavioursEnabled: false, wrapBehavioursEnabled: false, enableAutoIndent: true, fontSize: 14,
  });
  syncTheme();
  try { work = JSON.parse(safeGet(workKey())); } catch { work = null; }
  if (!work || !Array.isArray(work.events)) work = { t0: Date.now(), events: [], code: '', stdin: question.cases[0]?.input || '' };
  recorder = new Recorder(editor, work, { onChange: save, onBlocked: kind => toast(kind === 'paste'
    ? 'Pasting from outside the editor is not allowed. Type the code. (The attempt is recorded.)'
    : 'Dropping text into the editor is not allowed.') });
  if (work.code) recorder.load(work.code); else save();
  $('stdin').value = work.stdin || '';
  $('stdin').addEventListener('input', save);
  stats();
  updateSubmit();
  editor.focus();

  $('compiler-status').textContent = 'Loading the Java compiler...';
  if (!(await runner.available())) {
    $('compiler-status').textContent = 'The in-browser Java compiler is not installed on this website.';
    return;
  }
  const t0 = performance.now();
  runner.warmUp().then(() => {
    compilerReady = true;
    $('compiler-status').textContent = `Java compiler ready (${((performance.now() - t0) / 1000).toFixed(1)} s)`;
    busy(false, '');
  }, e => { $('compiler-status').textContent = 'The Java compiler could not start: ' + e.message; });
}
function updateSubmit() {
  const s = control?.sessions?.find(x => x.id === mode);
  const open = !!(s && openWindow(s));
  $('submit').disabled = !open || !editor;
  $('submit').title = open ? '' : (mode === 'practice' ? 'Practice work is not submitted.' : 'This assessment is closed.');
}

main().catch(e => { $('session-banner').className = 'notice bad'; $('session-banner').textContent = 'Error: ' + e.message; });
