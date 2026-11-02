// Teacher tools: key pair, unlocking the private key, marking a whole session in the browser,
// opening and replaying one submission. Nothing leaves this computer except requests to GitHub
// for the (encrypted) submissions and the lab status.
import { chrome, escapeHtml, fetchJson, fetchText, LAB } from './common.js';
import { generateKeyPair, decryptPackage, importPrivateKey, unarmour, BEGIN } from './lab-crypto.js';
import { replay, summarise, flags } from './lab-activity.js';
import * as runner from './java-runner.js';
import { zip } from './zip.js';

chrome('');
const $ = id => document.getElementById(id);
const dark = () => getComputedStyle(document.documentElement).colorScheme.includes('dark');
const syncTheme = () => { $('hl-light').disabled = dark(); $('hl-dark').disabled = !dark(); };
syncTheme();
document.addEventListener('themechange', syncTheme);
const blobUrl = (text, type = 'application/x-pem-file') => URL.createObjectURL(new Blob([text], { type }));
const MARKER = '<!-- lab-intake v1 ';
const BOT = 'github-actions[bot]';

// ------------------------------------------------------------------ 1. key pair
$('gen').addEventListener('click', async () => {
  const p1 = $('pass1').value, p2 = $('pass2').value;
  if (p1.length < 12) { $('gen-msg').textContent = 'The passphrase needs at least 12 characters.'; return; }
  if (p1 !== p2) { $('gen-msg').textContent = 'The two passphrases differ.'; return; }
  $('gen').disabled = true;
  $('gen-msg').textContent = 'Creating (a few seconds)...';
  const k = await generateKeyPair(p1);
  $('kid').textContent = k.keyId;
  $('dl-private').href = blobUrl(k.privatePem);
  $('dl-public').href = blobUrl(k.publicPem);
  $('public-pem').value = k.publicPem;
  $('create-public').href = `https://github.com/${LAB.repo}/new/${LAB.branch}?filename=lab/teacher-public-key.pem&value=${encodeURIComponent(k.publicPem)}`;
  $('keys').hidden = false;
  $('gen-msg').textContent = 'Key pair created.';
  $('pass1').value = $('pass2').value = '';
});

// ------------------------------------------------------------------ 2. unlock
let privateKey = null;
async function unlock() {
  if (privateKey) return privateKey;
  const file = $('file-key').files[0];
  if (!file) throw new Error('choose your private key file in part 2');
  privateKey = await importPrivateKey(await file.text(), $('passphrase').value);
  $('passphrase').value = '';
  $('unlock-msg').textContent = 'Key unlocked for this page.';
  return privateKey;
}
$('unlock').addEventListener('click', () => unlock().catch(e => { $('unlock-msg').textContent = e.message; }));
$('file-key').addEventListener('change', () => { privateKey = null; $('unlock-msg').textContent = ''; });

// ------------------------------------------------------------------ helpers shared with the robots
function field(body, label) {
  const lines = (body || '').split(/\r?\n/);
  const start = lines.findIndex(l => l.trim() === `### ${label}`);
  if (start < 0) return '';
  const out = [];
  for (let i = start + 1; i < lines.length && !lines[i].startsWith('### '); i++) out.push(lines[i]);
  const v = out.join('\n').trim().replace(/^```\w*\s*|\s*```$/g, '').trim();
  return v === '_No response_' ? '' : v;
}
const safeId = s => (s || '').replace(/[^A-Za-z0-9_-]/g, '').slice(0, 32);
const windowEnd = w => Math.min(...[w.closed_at, w.closes_at].filter(Boolean).map(Date.parse), Infinity);
function decide(control, sessionId, question, createdMs) {
  const s = (control.sessions || []).find(x => x.id === sessionId);
  if (!s) return [false, `unknown session ${sessionId}`];
  if (!(s.questions || []).includes(question)) return [false, `question ${question} is not part of session ${s.id}`];
  for (const w of s.windows || []) if (Date.parse(w.opened_at) <= createdMs && createdMs < windowEnd(w)) return [true, 'inside the window'];
  return [false, `session ${s.id} was not open then`];
}
const ist = iso => new Date(Date.parse(iso) + 5.5 * 3600e3).toISOString().slice(0, 19).replace('T', ' ');
const norm = s => s.replace(/\r\n/g, '\n').split('\n').map(l => l.trimEnd()).join('\n').trim();
function tokens(code) {
  return code.replace(/\/\/[^\n]*|\/\*[\s\S]*?\*\//g, ' ').replace(/"(?:\\.|[^"\\])*"/g, '"S"')
    .match(/[A-Za-z_]\w*|\d+(?:\.\d+)?|\S/g) || [];
}
function fingerprint(code) {
  const t = tokens(code), set = new Set();
  for (let i = 0; i + 5 <= Math.max(t.length, 5); i++) set.add(t.slice(i, i + 5).join('\u0001'));
  return set;
}
function jaccard(a, b) {
  let inter = 0;
  for (const x of a) if (b.has(x)) inter++;
  return a.size + b.size - inter ? inter / (a.size + b.size - inter) : 0;
}
const csvCell = v => { const s = v === null || v === undefined ? '' : String(v); return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s; };

// ------------------------------------------------------------------ 3. session export
async function gh(path, token) {
  const headers = { Accept: 'application/vnd.github+json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  const r = await fetch(`https://api.github.com/repos/${LAB.repo}${path}`, { headers });
  if (r.status === 403 || r.status === 429) throw new Error('GitHub limits requests without a token (60 per hour): wait, or enter a token');
  if (!r.ok) throw new Error(`GitHub answered ${r.status} for ${path}`);
  return r.json();
}
async function ghAll(path, token) {
  const out = [];
  for (let page = 1; ; page++) {
    const items = await gh(`${path}${path.includes('?') ? '&' : '?'}per_page=100&page=${page}`, token);
    out.push(...items);
    if (items.length < 100) return out;
  }
}

let catalog = null;
async function questionCases(id, hidden) {
  if (hidden[id] && hidden[id].cases.length) return hidden[id];
  catalog = catalog || await fetchJson('course/catalog.json');
  const p = catalog.find(x => x.id === id);
  const cases = [];
  let sql = '';
  if (p) {
    const base = `course/${p.path}/`;
    for (const suf of p.cases) {
      cases.push({ input: await fetchText(`${base}input${suf}.txt`).catch(() => ''), expected: await fetchText(`${base}expected${suf}.txt`) });
    }
    if (p.setup) sql = await fetchText(base + 'setup.sql');
  } else {
    const raw = path => fetch(`${LAB.raw}lab/questions/${id}/${path}?t=${Date.now()}`).then(r => (r.ok ? r.text() : null));
    for (let n = 1; n <= 20; n++) {
      const suf = n === 1 ? '' : `-${n}`;
      const exp = await raw(`expected${suf}.txt`);
      if (exp === null) break;
      cases.push({ input: (await raw(`input${suf}.txt`)) || '', expected: exp });
    }
    sql = (await raw('setup.sql')) || '';
  }
  return { cases, sql, source: 'public sample cases' };
}
async function hiddenTests() {
  const tests = {};
  for (const f of $('tests').files) {
    const parts = (f.webkitRelativePath || f.name).split('/');
    if (parts.length < 2) continue;
    const q = parts[parts.length - 2], name = parts[parts.length - 1];
    const t = tests[q] || (tests[q] = { files: {}, cases: [], sql: '', source: 'hidden tests' });
    t.files[name] = await f.text();
  }
  for (const t of Object.values(tests)) {
    for (let n = 1; t.files[`expected-${n}.txt`] !== undefined; n++) {
      t.cases.push({ input: t.files[`input-${n}.txt`] || '', expected: t.files[`expected-${n}.txt`] });
    }
    t.sql = t.files['setup.sql'] || '';
  }
  return tests;
}

$('export').addEventListener('click', () => exportSession().catch(e => log('Stopped: ' + e.message)));
function log(text) { $('export-log').textContent += text + '\n'; $('export-log').scrollTop = 1e9; }

async function exportSession() {
  $('export-log').textContent = '';
  $('export-table').innerHTML = '';
  const session = safeId($('session').value.trim());
  if (!session) throw new Error('enter the session name');
  const key = await unlock();
  const token = $('token').value.trim();
  const control = await fetch(`${LAB.raw}lab/control.json?t=${Date.now()}`).then(r => r.json());
  const s = (control.sessions || []).find(x => x.id === session);
  if (!s) throw new Error(`session ${session} is not in lab/control.json`);
  const since = new Date(Math.min(...s.windows.map(w => Date.parse(w.opened_at)))).toISOString();
  log(`Session ${session}: questions ${s.questions.join(' ')}, ${s.windows.length} window(s).`);
  const issues = (await ghAll(`/issues?labels=lab-submission&state=all&since=${since}`, token))
    .filter(i => !i.pull_request && safeId(field(i.body, 'Session')) === session);
  const comments = await ghAll(`/issues/comments?since=${since}`, token);
  const botCopy = {};
  for (const c of comments) {
    if (c.user?.login === BOT && (c.body || '').startsWith(MARKER) && c.body.includes(BEGIN)) {
      botCopy[Number(c.issue_url.split('/').pop())] = c.body;
    }
  }
  log(`${issues.length} submission issue(s) found.`);
  const hidden = await hiddenTests();
  const attempts = [];
  for (const i of issues) {
    const labels = (i.labels || []).map(l => l.name);
    if (labels.includes('lab-ignore')) continue;
    const a = { issue: i.number, url: i.html_url, github: i.user.login, created: i.created_at, problems: [] };
    let text = botCopy[i.number];
    if (!text) { text = i.body || ''; a.problems.push('no bot copy (issue may have been edited)'); }
    try {
      unarmour(text);
      const { data } = await decryptPackage(text, key);
      a.data = data;
      a.regno = safeId(String(data.regno || '').toUpperCase()) || 'UNKNOWN';
      a.name = String(data.name || '').replace(/[|<>\r\n\t]/g, ' ').slice(0, 80);
      a.question = safeId(String(data.question));
      if (safeId(String(data.session)) !== session) a.problems.push(`package is for session ${data.session}`);
      let [ok, reason] = decide(control, session, a.question, Date.parse(i.created_at));
      if (!ok && labels.includes('lab-accept-late')) { ok = true; reason = 'accepted by the teacher (lab-accept-late)'; }
      a.accepted = ok;
      a.reason = reason;
    } catch (e) {
      a.accepted = false;
      a.reason = e.message;
    }
    attempts.push(a);
  }
  const latest = new Map();
  for (const a of attempts) {
    if (!a.accepted) continue;
    const k = `${a.question}|${a.regno}`;
    if (!latest.has(k) || a.created > latest.get(k).created) latest.set(k, a);
  }
  log(`${attempts.filter(a => a.accepted).length} accepted, ${attempts.filter(a => !a.accepted).length} not accepted; marking ${latest.size}.`);
  if (!(await runner.available())) throw new Error('the in-browser compiler is not installed on this website');
  await runner.warmUp();
  const testsOf = {};
  let n = 0;
  for (const a of latest.values()) {
    n++;
    const t = testsOf[a.question] || (testsOf[a.question] = await questionCases(a.question, hidden));
    const files = a.data.files || {};
    a.code = Object.values(files).join('\n');
    a.result = { compiles: false, passed: 0, total: t.cases.length, source: t.source };
    const comp = await runner.compile(files);
    if (comp.ok) {
      a.result.compiles = true;
      for (const c of t.cases) {
        const r = await runner.run(comp.wasm, c.input, { timeLimitMs: 10000, sql: t.sql });
        if (!r.timedOut && norm(r.stdout) === norm(c.expected)) a.result.passed++;
      }
    }
    const act = a.data.activity || {};
    a.summary = summarise(act.events || [], Object.values(files)[0] || '');
    a.flags = [...flags(a.summary, !!act.trimmed), ...a.problems];
    a.fp = fingerprint(a.code);
    log(`${n}/${latest.size} ${a.question} ${a.regno}: ${a.result.compiles ? `${a.result.passed}/${a.result.total}` : 'does not compile'}`);
  }
  const marked = [...latest.values()].sort((x, y) => (x.regno + x.question).localeCompare(y.regno + y.question));
  for (const a of marked) {
    a.similar = marked.filter(b => b !== a && b.question === a.question && jaccard(a.fp, b.fp) >= 0.8).map(b => b.regno);
  }
  const header = ['regno', 'name', 'github', 'question', 'issue', 'submitted_ist', 'compiles', 'tests_passed', 'tests_total',
    'score_percent', 'tests_used', 'typed_chars', 'code_chars', 'active_min', 'paste_blocked', 'bulk_inserts', 'times_away',
    'away_min', 'runs', 'checks', 'typing_log_matches_code', 'similar_to', 'flags'];
  const rows = marked.map(a => [a.regno, a.name, a.github, a.question, a.issue, ist(a.created), a.result.compiles ? 'yes' : 'no',
    a.result.passed, a.result.total, a.result.total ? (100 * a.result.passed / a.result.total).toFixed(1) : '', a.result.source,
    a.summary.typedChars, a.summary.finalChars, (a.summary.activeMs / 60000).toFixed(1), a.summary.pasteBlocked,
    a.summary.bulkInserts, a.summary.awayCount, (a.summary.awayMs / 60000).toFixed(1), a.summary.runs, a.summary.checks,
    a.summary.replayMatches ? 'yes' : 'no', a.similar.join(' '), a.flags.join('; ')]);
  const csv = [header, ...rows].map(r => r.map(csvCell).join(',')).join('\n') + '\n';
  const rejected = attempts.filter(a => !a.accepted).map(a => [a.issue, a.github, a.regno || '', ist(a.created), a.reason]);
  const files = [{ name: `${session}/marks.csv`, data: csv },
    { name: `${session}/not-accepted.csv`, data: [['issue', 'github', 'regno', 'created_ist', 'reason'], ...rejected].map(r => r.map(csvCell).join(',')).join('\n') + '\n' }];
  for (const a of attempts) {
    if (!a.data) continue;
    const dir = `${session}/${a.question}/${a.regno}/${a.issue}/`;
    for (const [name, text] of Object.entries(a.data.files || {})) {
      if (/^[A-Za-z_$][\w$]{0,63}\.java$/.test(name)) files.push({ name: dir + name, data: text });
    }
    files.push({ name: dir + 'submission.json', data: JSON.stringify({ meta: { issue: a.issue, url: a.url, github: a.github, created_at: a.created, accepted: a.accepted, reason: a.reason, problems: a.problems }, ...a.data }) });
  }
  $('dl-csv').href = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
  $('dl-csv').download = `${session}-marks.csv`;
  $('dl-zip').href = URL.createObjectURL(zip(files));
  $('dl-zip').download = `${session}-lab-work.zip`;
  $('dl-csv').hidden = $('dl-zip').hidden = false;
  $('export-table').innerHTML = `<tr>${['Register no.', 'Name', 'Question', 'Tests', 'Typed / code', 'Active min', 'Similar to', 'Flags'].map(h => `<th>${h}</th>`).join('')}</tr>` +
    marked.map(a => `<tr><td>${escapeHtml(a.regno)}</td><td>${escapeHtml(a.name)}</td><td>${escapeHtml(a.question)}</td>
      <td>${a.result.compiles ? `${a.result.passed}/${a.result.total}` : 'no compile'}</td><td>${a.summary.typedChars} / ${a.summary.finalChars}</td>
      <td>${(a.summary.activeMs / 60000).toFixed(1)}</td><td>${escapeHtml(a.similar.join(' '))}</td><td>${escapeHtml(a.flags.join('; '))}</td></tr>`).join('');
  log('Done. Download marks.csv and the ZIP.');
}

// ------------------------------------------------------------------ 4. one submission
async function packageText(value) {
  const v = value.trim();
  if (/^\d+$/.test(v)) {
    const r = await fetch(`https://api.github.com/repos/${LAB.repo}/issues/${v}/comments?per_page=100`);
    if (!r.ok) throw new Error(`GitHub answered ${r.status} for issue ${v}`);
    const bot = (await r.json()).filter(c => c.user?.login === BOT && c.body.includes(BEGIN));
    if (bot.length) return bot[bot.length - 1].body;
    const i = await (await fetch(`https://api.github.com/repos/${LAB.repo}/issues/${v}`)).json();
    return i.body || '';
  }
  return v;
}

$('open').addEventListener('click', async () => {
  $('open-msg').textContent = '';
  try {
    const json = $('file-json').files[0];
    const sub = json ? JSON.parse(await json.text()) : (await decryptPackage(await packageText($('pkg').value), await unlock())).data;
    show(sub);
  } catch (e) {
    $('open-msg').textContent = 'Could not open: ' + e.message;
  }
});

let timer = null;
function show(sub) {
  const code = Object.values(sub.files || {})[0] || '';
  const events = sub.activity?.events || [];
  const s = summarise(events, code);
  const meta = sub.meta || {};
  $('who').textContent = `${sub.regno} ${sub.name}: ${sub.session} ${sub.question}` + (meta.issue ? ` (issue #${meta.issue})` : '');
  const min = ms => (ms / 60000).toFixed(1);
  $('summary').innerHTML = [
    ['Typed characters', s.typedChars], ['Characters in code', s.finalChars], ['Active typing', `${min(s.activeMs)} min`],
    ['Pastes blocked', s.pasteBlocked], ['Large insertions', s.bulkInserts], ['Left the page', `${s.awayCount} times, ${min(s.awayMs)} min`],
    ['Runs', `${s.runs} (${s.compileErrors} not compiling)`], ['Best check', s.bestTotal ? `${s.bestPassed}/${s.bestTotal}` : '-'],
    ['Page loads', s.loads], ['Typing log gives the code', s.replayMatches ? 'yes' : (sub.activity?.trimmed ? 'log trimmed' : 'NO')],
    ['Created (student clock)', sub.createdAt], ['Browser', (sub.userAgent || '').slice(0, 80)],
  ].map(([k, v]) => `<span>${k}: <b>${escapeHtml(String(v))}</b></span>`).join('');
  const f = [...(meta.problems || []), ...flags(s, !!sub.activity?.trimmed)];
  $('flags').innerHTML = f.length ? `<div class="notice warn small">${f.map(escapeHtml).join('<br>')}</div>` : '';
  $('final-code').textContent = code;
  if (window.hljs) { delete $('final-code').dataset.highlighted; window.hljs.highlightElement($('final-code')); }

  const end = events.length ? events[events.length - 1][1] : 0;
  const color = { i: 'var(--ok)', r: 'var(--ok)', p: 'var(--bad)', B: 'var(--bad)', d: 'var(--bad)', a: 'var(--warn)', x: 'var(--accent)', c: 'var(--accent)' };
  $('bar').innerHTML = events.filter(e => color[e[0]]).map(e =>
    `<span style="left:${(100 * e[1] / Math.max(1, end)).toFixed(2)}%;background:${color[e[0]]}"></span>`).join('');
  const label = e => {
    const t = new Date(e[1]).toISOString().slice(11, 19);
    const what = { i: () => `typed ${JSON.stringify(e[4]).slice(0, 60)}`, r: () => `deleted at line ${e[2] + 1}`,
      p: () => `PASTE BLOCKED (${e[2]} chars): ${JSON.stringify(e[3]).slice(0, 80)}`, P: () => `pasted own copied text (${e[2]} chars)`,
      B: () => `LARGE INSERTION (${e[2]} chars)`, d: () => 'DROP BLOCKED', a: () => 'left the page', f: () => 'came back',
      L: () => `page loaded (${e[2]} chars)`, x: () => `run: ${e[2]} (${e[3]} ms)`, c: () => `check: ${e[2]}/${e[3]} passed`,
      s: () => 'submission made' }[e[0]];
    return `${t}  ${what ? what() : e[0]}`;
  };
  $('events').textContent = events.map(label).join('\n');
  const slider = $('slider');
  slider.max = String(end);
  const draw = () => {
    const t = Number(slider.value);
    $('replay-code').textContent = replay(events, t);
    $('clock').textContent = `${new Date(t).toISOString().slice(11, 19)} / ${new Date(end).toISOString().slice(11, 19)}`;
  };
  slider.value = String(end);
  slider.oninput = draw;
  draw();
  $('play').onclick = () => {
    if (timer) { clearInterval(timer); timer = null; $('play').textContent = 'Play'; return; }
    if (Number(slider.value) >= end) slider.value = '0';
    $('play').textContent = 'Pause';
    const step = Math.max(200, end / 600);
    timer = setInterval(() => {
      slider.value = String(Math.min(end, Number(slider.value) + step));
      draw();
      if (Number(slider.value) >= end) { clearInterval(timer); timer = null; $('play').textContent = 'Play'; }
    }, 100);
  };
  $('view').hidden = false;
  $('view').scrollIntoView({ behavior: 'smooth' });
}
