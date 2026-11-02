// Web Worker that runs ONE compiled student program (see java-runner.js). The page terminates
// this worker when the time limit is reached. Keyboard input: Scanner(System.in) reads the
// global labStdin (java/teavm-patches/.../BrowserStdin.java).
import { load } from '../javac/compiler.wasm-runtime.js';

onmessage = async ({ data }) => {
  globalThis.labStdin = data.stdin || '';
  let out = '', err = '';
  let produced = 0;                    // characters written so far (for the idle check)
  const flush = () => {
    if (out) { postMessage({ type: 'out', text: out }); out = ''; }
    if (err) { postMessage({ type: 'err', text: err }); err = ''; }
  };
  const ticker = setInterval(flush, 50);
  let exception = null;
  try {
    const app = await load(new Int8Array(data.wasm), {
      installImports(imports) {
        imports.teavmConsole.putcharStdout = c => { out += String.fromCharCode(c); produced++; };
        imports.teavmConsole.putcharStderr = c => { err += String.fromCharCode(c); produced++; };
      },
    });
    const r = app.exports.main([]);
    if (r && typeof r.then === 'function') await r;
  } catch (e) {
    exception = String(e && e.message || e);
  }
  // Programs with threads (Experiment 7) keep running after main returns: wait until their
  // output has been quiet for 300 ms (the page's time limit still applies).
  let last = -1;
  while (produced !== last) {
    last = produced;
    await new Promise(r => setTimeout(r, 300));
  }
  clearInterval(ticker);
  flush();
  postMessage({ type: 'done', exception });
};
