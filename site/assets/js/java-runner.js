// Compile and run Java in the browser with the open-source teavm-javac (OpenJDK javac + TeaVM,
// WebAssembly). Nothing is sent to a server: the student's code is compiled and executed on
// their own device.
//
// - One long-lived worker holds the compiler (loading it takes a few seconds, compiling ~1 s).
// - Every run uses a fresh worker, so an endless loop is stopped by terminating that worker.

const JAVAC = new URL('../javac/', import.meta.url);
let compiler = null;
let seq = 0;
const pending = new Map();

/** True when the compiler files are deployed with the site. */
export async function available() {
  try {
    const r = await fetch(new URL('compiler.wasm', JAVAC), { method: 'HEAD' });
    return r.ok;
  } catch {
    return false;
  }
}

function startCompiler() {
  if (compiler) return compiler;
  const worker = new Worker(new URL('./javac-worker.js', import.meta.url), { type: 'module' });
  const ready = new Promise((resolve, reject) => {
    worker.onmessage = ({ data }) => {
      if (data.type === 'ready') { resolve(); return; }
      if (data.type === 'init-error') { reject(new Error(data.message)); return; }
      const p = pending.get(data.id);
      if (p) { pending.delete(data.id); p(data); }
    };
    worker.onerror = e => reject(new Error(e.message || 'compiler worker failed'));
  });
  compiler = { worker, ready };
  return compiler;
}

/** Loads the compiler in the background (call early to hide the start-up time). */
export function warmUp() {
  return startCompiler().ready;
}

/**
 * Compiles source files {"Main.java": text}. Result: {ok, diagnostics[], wasm (ArrayBuffer),
 * mainClass}. Diagnostics: {severity, fileName, lineNumber, columnNumber, message, type}.
 */
export async function compile(files) {
  const c = startCompiler();
  await c.ready;
  const id = ++seq;
  return new Promise(resolve => {
    pending.set(id, resolve);
    c.worker.postMessage({ id, files });
  });
}

/**
 * Runs a compiled program with the given keyboard input. `sql`: set-up script of the embedded
 * database (CREATE DATABASE, CREATE TABLE ...) for programs that use JDBC. Result: {stdout,
 * stderr, exception, timedOut, outputLimited, ms}.
 */
export function run(wasm, stdin, { timeLimitMs = 5000, maxOutput = 65536, sql = '' } = {}) {
  return new Promise(resolve => {
    const t0 = performance.now();
    const worker = new Worker(new URL('./java-exec-worker.js', import.meta.url), { type: 'module' });
    let stdout = '', stderr = '', done = false;
    const finish = extra => {
      if (done) return;
      done = true;
      clearTimeout(timer);
      worker.terminate();
      resolve({ stdout, stderr, exception: null, timedOut: false, outputLimited: false,
                ms: Math.round(performance.now() - t0), ...extra });
    };
    const timer = setTimeout(() => finish({ timedOut: true }), timeLimitMs);
    worker.onmessage = ({ data }) => {
      if (data.type === 'out') stdout += data.text;
      else if (data.type === 'err') stderr += data.text;
      else if (data.type === 'done') finish({ exception: data.exception });
      if (stdout.length + stderr.length > maxOutput) {
        stdout = stdout.slice(0, maxOutput);
        finish({ outputLimited: true });
      }
    };
    worker.onerror = e => finish({ exception: e.message || 'the program could not be started' });
    worker.postMessage({ wasm, stdin, sql });     // copied, so the same program can run again
  });
}

/** Same comparison as the JDK grader: line-end spaces and leading/trailing blank lines ignored. */
export function normalise(s) {
  return s.replace(/\r\n/g, '\n').split('\n').map(l => l.trimEnd()).join('\n').trim();
}

/** File name a public class needs (javac requires class X to be in X.java). */
export function fileNameFor(code) {
  const m = code.match(/public\s+(?:final\s+|abstract\s+)*class\s+(\w+)/);
  return (m ? m[1] : 'Main') + '.java';
}
