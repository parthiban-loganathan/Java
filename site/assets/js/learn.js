import { chrome, fetchJson, fetchText, renderMarkdown, escapeHtml, CODESPACES_URL } from './common.js';

chrome('learn.html');

const $ = id => document.getElementById(id);
let catalog = [];
let current = null;
let javaModule = null;
let sourceFiles = {};

function syncHighlightTheme() {
  const dark = getComputedStyle(document.documentElement).colorScheme.includes('dark');
  $('hl-light').disabled = dark;
  $('hl-dark').disabled = !dark;
}
syncHighlightTheme();
document.addEventListener('themechange', syncHighlightTheme);
$('codespaces-link').href = CODESPACES_URL;

async function loadJava() {
  if (!javaModule) javaModule = await import('../java/civiltech.js');
  return javaModule;
}

function buildToc() {
  const groups = [['lesson', 'Lessons: Java from zero'], ['experiment', 'Lab experiments (BACSE102)']];
  $('toc').innerHTML = groups.map(([kind, title]) => `<h3>${title}</h3>` + catalog.filter(p => p.kind === kind)
    .map(p => `<a href="#${p.id}" data-id="${p.id}"><span class="id">${p.id}</span>${escapeHtml(p.title.replace(/^Experiment [0-9a-c]+: /, ''))}</a>`)
    .join('')).join('');
}

async function show(id) {
  const p = catalog.find(x => x.id === id) || catalog[0];
  current = p;
  document.querySelectorAll('#toc a').forEach(a => a.classList.toggle('active', a.dataset.id === p.id));
  document.title = `${p.id} ${p.title.replace(/^Experiment [0-9a-c]+: /, '')}`;
  $('lab-link').href = `lab.html?s=practice&q=${encodeURIComponent(p.id)}`;
  const base = `course/${p.path}/`;
  const md = await fetchText(base + 'README.md');
  const badges = [
    `<span class="badge">${p.kind === 'lesson' ? 'Lesson' : 'Experiment'} ${p.id}</span>`,
    p.browser ? '<span class="badge ok">Runs in your browser</span>' : '<span class="badge warn">JDK only</span>',
  ].join(' ');
  $('content').innerHTML = `<div>${badges}</div><div class="prose">${renderMarkdown(md, base)}</div>`;

  sourceFiles = {};
  for (const f of p.sources) sourceFiles[f] = await fetchText(base + f);
  $('file-tabs').innerHTML = p.sources.map((f, i) => `<button data-file="${f}" class="${i === 0 ? 'active' : ''}">${f}</button>`).join('');
  $('file-tabs').querySelectorAll('button').forEach(b => b.addEventListener('click', () => showFile(b.dataset.file)));
  showFile(p.sources[0]);

  $('case-select').innerHTML = p.cases.map((c, i) => `<option value="${c}">Test case ${i + 1}</option>`).join('');
  $('case-select').hidden = p.cases.length < 2;
  await loadCase();
  $('run-note').hidden = !p.note;
  $('run-note').textContent = p.note;
  $('run').disabled = !p.browser;
  $('run').title = p.browser ? '' : 'This program needs the JDK (see the note).';
  $('stdout').textContent = p.browser ? 'Press Run.' : 'Not available in the browser for this program.';
  $('match').innerHTML = '';
  $('run-status').textContent = '';
  $('workbench').hidden = false;
  window.scrollTo({ top: 0 });
}

function showFile(name) {
  $('file-tabs').querySelectorAll('button').forEach(b => b.classList.toggle('active', b.dataset.file === name));
  const el = $('code');
  el.textContent = sourceFiles[name];
  el.removeAttribute('data-highlighted');
  if (window.hljs) window.hljs.highlightElement(el);
  const blob = new Blob([sourceFiles[name]], { type: 'text/x-java' });
  const a = $('download-code');
  if (a.href.startsWith('blob:')) URL.revokeObjectURL(a.href);
  a.href = URL.createObjectURL(blob);
  a.download = name;
}

async function loadCase() {
  const p = current;
  const suffix = $('case-select').value || '';
  const base = `course/${p.path}/`;
  let input = '';
  try { input = await fetchText(`${base}input${suffix}.txt`); } catch { input = ''; }
  $('stdin').value = input;
  $('stdin').dataset.original = input;
  $('expected').textContent = await fetchText(`${base}expected${suffix}.txt`);
  $('match').innerHTML = '';
}

const norm = s => s.replace(/\r\n/g, '\n').split('\n').map(l => l.trimEnd()).join('\n').trim();

async function run() {
  const p = current;
  $('run').disabled = true;
  $('run-status').innerHTML = '<span class="spinner"></span> loading Java runtime...';
  try {
    const java = await loadJava();
    $('run-status').innerHTML = '<span class="spinner"></span> running...';
    const t0 = performance.now();
    const [out, finished] = await new Promise(res => java.runProgram(p.id, $('stdin').value, (o, f) => res([o, f])));
    $('stdout').textContent = out || '(no output)';
    $('run-status').textContent = `finished in ${(performance.now() - t0).toFixed(0)} ms` + (finished ? '' : ' (stopped after 10 s)');
    const sameInput = $('stdin').value === $('stdin').dataset.original;
    if (sameInput) {
      const ok = norm(out) === norm($('expected').textContent);
      $('match').innerHTML = ok ? '<span class="badge ok">matches the JDK output</span>'
                                : '<span class="badge warn">differs from the JDK output (see note)</span>';
    } else {
      $('match').innerHTML = '<span class="badge">your own input</span>';
    }
  } catch (e) {
    $('stdout').textContent = 'Could not run: ' + e.message;
    $('run-status').textContent = '';
  } finally {
    $('run').disabled = !p.browser;
  }
}

$('run').addEventListener('click', run);
$('reset-input').addEventListener('click', () => { $('stdin').value = $('stdin').dataset.original || ''; });
$('case-select').addEventListener('change', loadCase);
$('copy-code').addEventListener('click', async () => {
  const name = $('file-tabs').querySelector('button.active')?.dataset.file;
  try {
    await navigator.clipboard.writeText(sourceFiles[name]);
    $('copy-code').textContent = 'Copied';
  } catch {
    $('copy-code').textContent = 'Copy failed';
  }
  setTimeout(() => { $('copy-code').textContent = 'Copy'; }, 1500);
});
window.addEventListener('hashchange', () => show(location.hash.slice(1)));

catalog = await fetchJson('course/catalog.json');
buildToc();
show(location.hash.slice(1) || 'L01');
