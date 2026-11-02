// Activity log of the Lab editor: what the student typed, deleted, tried to paste, when they
// left the page, and every run and check. The teacher receives it inside the encrypted
// submission and can replay the typing (teacher.html, tools/lab/activity.py).
//
// Event formats (t = milliseconds since the log was started):
//   ['i', t, row, col, text]           text inserted (consecutive keystrokes are merged)
//   ['r', t, row1, col1, row2, col2]   text removed
//   ['p', t, length, first200]         paste from outside the editor: BLOCKED
//   ['P', t, length]                   paste of text copied inside this editor: allowed
//   ['B', t, length]                   one insertion of 20+ visible characters that was not a paste
//   ['d', t]                           drag-and-drop into the editor: BLOCKED
//   ['a', t] / ['f', t]                page left (other tab / window) / page back in focus
//   ['L', t, length, fnv]              page (re)loaded with the saved code
//   ['x', t, outcome, ms]              run: ok | compile-error | exception | timeout
//   ['c', t, passed, total]            check against the sample cases
//   ['s', t, fnv]                      submission package made
// Replaying i and r from an empty document gives the code (activity.py checks this).

export const FORMAT = 1;
const MERGE_MS = 2000;
const BULK = 20;

/** FNV-1a 32-bit hash of the UTF-8 bytes, 8 hex digits (same in activity.py). */
export function fnv(text) {
  let h = 0x811c9dc5;
  for (const b of new TextEncoder().encode(text)) {
    h ^= b;
    h = Math.imul(h, 0x01000193) >>> 0;
  }
  return h.toString(16).padStart(8, '0');
}

/** Applies the i and r events to an empty document. */
export function replay(events, upTo = Infinity) {
  let lines = [''];
  for (const e of events) {
    if (e[1] > upTo) break;
    if (e[0] === 'i') {
      const [, , row, col, text] = e;
      const line = lines[row] ?? '';
      const parts = text.split('\n');
      const before = line.slice(0, col), after = line.slice(col);
      if (parts.length === 1) lines[row] = before + text + after;
      else lines.splice(row, 1, before + parts[0], ...parts.slice(1, -1), parts[parts.length - 1] + after);
    } else if (e[0] === 'r') {
      const [, , r1, c1, r2, c2] = e;
      lines.splice(r1, r2 - r1 + 1, (lines[r1] ?? '').slice(0, c1) + (lines[r2] ?? '').slice(c2));
    }
  }
  return lines.join('\n');
}

/** Numbers the teacher looks at first (activity.py computes the same). */
export function summarise(events, code) {
  const s = { typedChars: 0, insertedChars: 0, removals: 0, pasteBlocked: 0, pasteBlockedChars: 0,
    internalPastes: 0, internalPasteChars: 0, bulkInserts: 0, bulkChars: 0, dropBlocked: 0,
    awayCount: 0, awayMs: 0, activeMs: 0, loads: 0, runs: 0, compileErrors: 0, checks: 0,
    bestPassed: 0, bestTotal: 0, firstEditMs: null, lastEditMs: null, finalChars: code.length,
    replayMatches: replay(events) === code };
  let lastEdit = null, awaySince = null;
  for (const e of events) {
    const [type, t] = e;
    if (type === 'i' || type === 'r') {
      if (lastEdit !== null && t - lastEdit < 60000) s.activeMs += t - lastEdit;
      lastEdit = t;
      if (s.firstEditMs === null) s.firstEditMs = t;
      s.lastEditMs = t;
      if (type === 'i') s.insertedChars += e[4].length; else s.removals++;
    } else if (type === 'p') { s.pasteBlocked++; s.pasteBlockedChars += e[2]; }
    else if (type === 'P') { s.internalPastes++; s.internalPasteChars += e[2]; }
    else if (type === 'B') { s.bulkInserts++; s.bulkChars += e[2]; }
    else if (type === 'd') s.dropBlocked++;
    else if (type === 'a') { if (awaySince === null) { awaySince = t; s.awayCount++; } }
    else if (type === 'f') { if (awaySince !== null) { s.awayMs += t - awaySince; awaySince = null; } }
    else if (type === 'L') s.loads++;
    else if (type === 'x') { s.runs++; if (e[2] === 'compile-error') s.compileErrors++; }
    else if (type === 'c') {
      s.checks++;
      if (e[2] > s.bestPassed || s.bestTotal === 0) { s.bestPassed = e[2]; s.bestTotal = e[3]; }
    }
  }
  s.typedChars = s.insertedChars - s.internalPasteChars - s.bulkChars;
  return s;
}

/**
 * Records an Ace editor. `log` = {t0, events} (restored from storage or new); `onChange` is
 * called after every recorded event (to save); `onBlocked(kind)` when a paste or drop is refused.
 */
export class Recorder {
  constructor(editor, log, { onChange = () => {}, onBlocked = () => {} } = {}) {
    this.editor = editor;
    this.log = log;
    this.onChange = onChange;
    this.silent = false;
    this.clip = null;                 // last text copied or cut inside this editor
    this.command = null;
    this.pasting = false;
    this.lastInsertAt = -Infinity;
    this.lastRemoveAt = -Infinity;

    const session = editor.session;
    session.setNewLineMode('unix');
    session.on('change', d => { if (!this.silent) this.change(d); });
    editor.on('copy', o => { this.clip = o && typeof o === 'object' ? o.text : o; });
    editor.on('paste', e => {
      const text = e.text || '';
      if (text && text === this.clip) {
        this.push(['P', this.now(), text.length]);
        this.pasting = true;
        setTimeout(() => { this.pasting = false; }, 0);
      } else {
        this.push(['p', this.now(), text.length, text.slice(0, 200)]);
        e.text = '';
        onBlocked('paste');
      }
    });
    editor.commands.on('exec', e => { this.command = e.command && e.command.name; });
    editor.commands.on('afterExec', () => { this.command = null; });
    const box = editor.container;
    const block = e => {
      e.preventDefault();
      e.stopPropagation();
      if (e.type === 'drop') { this.push(['d', this.now()]); onBlocked('drop'); }
    };
    for (const type of ['dragenter', 'dragover', 'drop']) box.addEventListener(type, block, true);
    const away = () => this.push(['a', this.now()]);
    const back = () => this.push(['f', this.now()]);
    window.addEventListener('blur', away);
    window.addEventListener('focus', back);
    document.addEventListener('visibilitychange', () => (document.hidden ? away() : back()));
  }

  now() { return Date.now() - this.log.t0; }

  push(event) {
    this.log.events.push(event);
    this.onChange();
  }

  change(d) {
    const t = this.now();
    const text = d.lines.join('\n');
    const ev = this.log.events;
    const last = ev[ev.length - 1];
    if (d.action === 'insert') {
      const visible = text.replace(/\s/g, '').length;
      const undo = this.command === 'undo' || this.command === 'redo';
      if (visible >= BULK && !this.pasting && !undo) ev.push(['B', t, text.length]);
      if (last && last[0] === 'i' && t - this.lastInsertAt < MERGE_MS && !text.includes('\n') &&
          !last[4].includes('\n') && last[2] === d.start.row && last[3] + last[4].length === d.start.column) {
        last[4] += text;
      } else {
        ev.push(['i', t, d.start.row, d.start.column, text]);
      }
      this.lastInsertAt = t;
    } else {
      const sameLine = last && last[0] === 'r' && t - this.lastRemoveAt < MERGE_MS &&
        d.start.row === d.end.row && last[2] === last[4] && last[2] === d.start.row;
      if (sameLine && last[3] === d.end.column) {
        last[3] = d.start.column;                         // Backspace again: range grows to the left
      } else if (sameLine && last[3] === d.start.column) {
        last[5] += d.end.column - d.start.column;         // Delete again: range grows to the right
      } else {
        ev.push(['r', t, d.start.row, d.start.column, d.end.row, d.end.column]);
      }
      this.lastRemoveAt = t;
    }
    this.onChange();
  }

  /** Puts saved code into the editor without recording it as typing. */
  load(code) {
    this.silent = true;
    this.editor.session.setValue(code);
    this.silent = false;
    this.push(['L', this.now(), code.length, fnv(code)]);
  }

  event(type, ...values) {
    this.push([type, this.now(), ...values]);
  }
}

/** Reasons for the teacher to look closer (same rules as tools/lab/activity.py flags()). */
export function flags(s, trimmed = false) {
  const f = [];
  if (trimmed) f.push('typing log trimmed (very long work)');
  else if (!s.replayMatches) f.push('code differs from the typing log');
  if (s.pasteBlocked) f.push(`${s.pasteBlocked} paste attempt(s) blocked`);
  if (s.bulkInserts) f.push(`${s.bulkInserts} large insertion(s) without typing`);
  if (s.dropBlocked) f.push(`${s.dropBlocked} drop attempt(s) blocked`);
  if (!trimmed && s.finalChars > 40 && s.typedChars < 0.8 * s.finalChars) f.push(`typed ${s.typedChars} characters for ${s.finalChars} in the code`);
  if (s.awayMs > 5 * 60000) f.push(`away from the page ${Math.round(s.awayMs / 60000)} min`);
  return f;
}
