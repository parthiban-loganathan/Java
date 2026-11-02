// Shared page chrome: navigation, theme switch, small helpers.
export const REPO_URL = 'https://github.com/parthiban-loganathan/Java';
export const CODESPACES_URL = 'https://codespaces.new/parthiban-loganathan/Java';

const NAV = [
  ['index.html', 'Home'],
  ['learn.html', 'Learn Java'],
  ['catchment.html', 'Catchment simulator'],
  ['starter/index.html', 'Starter template'],
  ['projects.html', 'Projects'],
  ['docs.html', 'Guides'],
];

export function chrome(active) {
  const base = document.documentElement.dataset.base || '';
  const header = document.createElement('header');
  header.className = 'site-header';
  header.innerHTML = `
    <div class="bar">
      <a class="brand" href="${base}index.html" aria-label="Lab home">
        <svg width="30" height="30" viewBox="0 0 32 32" aria-hidden="true"><rect width="32" height="32" rx="7" fill="#0b6e8a"/>
          <path d="M5 22 L12 12 L17 18 L21 13 L27 22 Z" fill="#e2f1f5"/><path d="M5 25 Q16 21 27 25" stroke="#7fd0e6" stroke-width="2" fill="none"/></svg>
        <span>Civil Engineering Innovation Lab<small>VIT Vellore &middot; open source</small></span>
      </a>
      <button class="menu-btn" aria-expanded="false" aria-controls="site-nav">Menu</button>
      <nav class="nav" id="site-nav">${NAV.map(([href, label]) =>
        `<a href="${base}${href}" class="${href === active ? 'active' : ''}">${label}</a>`).join('')}
        <a href="${REPO_URL}" rel="noopener">GitHub</a>
        <button class="theme-btn" title="Switch light / dark theme" aria-label="Switch theme">&#9680;</button>
      </nav>
    </div>`;
  document.body.prepend(header);
  const footer = document.createElement('footer');
  footer.className = 'site-footer';
  footer.innerHTML = `<div class="inner">
    <span>Code: MIT licence &middot; Text: CC BY 4.0 &middot; Data: see <a href="${base}docs.html#06-data-sources">data sources</a></span>
    <span>Built with Java (TeaVM), Three.js, Chart.js, Leaflet &middot; hosted on GitHub Pages</span></div>`;
  document.body.append(footer);

  const nav = header.querySelector('.nav');
  const menu = header.querySelector('.menu-btn');
  menu.addEventListener('click', () => {
    const open = nav.classList.toggle('open');
    menu.setAttribute('aria-expanded', String(open));
  });
  const saved = safeGet('lab-theme');
  if (saved) document.documentElement.dataset.theme = saved;
  header.querySelector('.theme-btn').addEventListener('click', () => {
    const dark = getComputedStyle(document.documentElement).colorScheme.includes('dark');
    const next = dark ? 'light' : 'dark';
    document.documentElement.dataset.theme = next;
    safeSet('lab-theme', next);
    document.dispatchEvent(new CustomEvent('themechange'));
  });
}

export function safeGet(k) { try { return localStorage.getItem(k); } catch { return null; } }
export function safeSet(k, v) { try { localStorage.setItem(k, v); } catch { /* storage unavailable */ } }

export function cssVar(name) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

export async function fetchText(url) {
  const r = await fetch(url);
  if (!r.ok) throw new Error(`${url}: HTTP ${r.status}`);
  return r.text();
}

export async function fetchJson(url) {
  return JSON.parse(await fetchText(url));
}

export function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

/** Markdown to HTML with marked (loaded as a global by the page), links made relative to baseUrl. */
export function renderMarkdown(md, baseUrl = '') {
  const html = window.marked.parse(md, { gfm: true });
  const tmp = document.createElement('div');
  tmp.innerHTML = html;
  tmp.querySelectorAll('a[href]').forEach(a => {
    const h = a.getAttribute('href');
    if (/^(https?:|#|mailto:)/.test(h)) return;
    a.setAttribute('href', REPO_URL + '/blob/main/' + baseUrl + h);
    a.setAttribute('rel', 'noopener');
  });
  tmp.querySelectorAll('pre code').forEach(el => {
    if (window.hljs) window.hljs.highlightElement(el);
  });
  return tmp.innerHTML;
}

export function fmt(x, d = 2) {
  return Number.isFinite(x) ? x.toLocaleString('en-IN', { minimumFractionDigits: d, maximumFractionDigits: d }) : '-';
}
