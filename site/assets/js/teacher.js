// Teacher tools: key pair creation, opening a submission, replaying the typing. All local.
import { chrome, escapeHtml, LAB } from './common.js';
import { generateKeyPair, decryptPackage } from './lab-crypto.js';
import { replay, summarise } from './lab-activity.js';

chrome('');
const $ = id => document.getElementById(id);
const dark = () => getComputedStyle(document.documentElement).colorScheme.includes('dark');
const syncTheme = () => { $('hl-light').disabled = dark(); $('hl-dark').disabled = !dark(); };
syncTheme();
document.addEventListener('themechange', syncTheme);

const blobUrl = text => URL.createObjectURL(new Blob([text], { type: 'application/x-pem-file' }));

$('gen').addEventListener('click', async () => {
  $('gen').disabled = true;
  $('gen').textContent = 'Creating (a few seconds)...';
  const k = await generateKeyPair();
  $('kid').textContent = k.keyId;
  $('dl-private').href = blobUrl(k.privatePem);
  $('dl-public').href = blobUrl(k.publicPem);
  $('public-pem').value = k.publicPem;
  $('create-public').href = `https://github.com/${LAB.repo}/new/${LAB.branch}?filename=lab/teacher-public-key.pem&value=${encodeURIComponent(k.publicPem)}`;
  $('keys').hidden = false;
  $('gen').textContent = 'Key pair created';
});

const readFile = input => (input.files[0] ? input.files[0].text() : Promise.resolve(null));

async function packageText(value) {
  const v = value.trim();
  if (/^\d+$/.test(v)) {
    const r = await fetch(`https://api.github.com/repos/${LAB.repo}/issues/${v}/comments?per_page=100`);
    if (!r.ok) throw new Error(`GitHub answered ${r.status} for issue ${v}`);
    const bot = (await r.json()).filter(c => c.user?.login === 'github-actions[bot]' && c.body.includes('BEGIN LAB SUBMISSION'));
    if (bot.length) return bot[bot.length - 1].body;
    const i = await (await fetch(`https://api.github.com/repos/${LAB.repo}/issues/${v}`)).json();
    return i.body || '';
  }
  return v;
}

$('open').addEventListener('click', async () => {
  $('open-msg').textContent = '';
  try {
    const json = await readFile($('file-json'));
    let sub;
    if (json) sub = JSON.parse(json);
    else {
      const key = await readFile($('file-key'));
      if (!key) throw new Error('Choose submission.json, or the private key file and a package.');
      sub = (await decryptPackage(await packageText($('pkg').value), key)).data;
    }
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
  const flags = (meta.problems || []).slice();
  if (!s.replayMatches && !sub.activity?.trimmed) flags.push('the submitted code is not what the typing log produces');
  if (s.pasteBlocked) flags.push(`${s.pasteBlocked} paste attempt(s), ${s.pasteBlockedChars} characters`);
  if (s.bulkInserts) flags.push(`${s.bulkInserts} large insertion(s) without typing (${s.bulkChars} characters)`);
  $('flags').innerHTML = flags.length ? `<div class="notice warn small">${flags.map(escapeHtml).join('<br>')}</div>` : '';
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
    const step = Math.max(200, end / 600);          // about 1 minute for the whole session
    timer = setInterval(() => {
      slider.value = String(Math.min(end, Number(slider.value) + step));
      draw();
      if (Number(slider.value) >= end) { clearInterval(timer); timer = null; $('play').textContent = 'Play'; }
    }, 100);
  };
  $('view').hidden = false;
  $('view').scrollIntoView({ behavior: 'smooth' });
}
