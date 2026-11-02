// Web Worker that holds the in-browser Java compiler (teavm-javac). See java-runner.js.
import { load } from '../javac/compiler.wasm-runtime.js';

const base = new URL('../javac/', import.meta.url);
let compiler = null;
let diagnostics = [];

async function binary(name) {
  const r = await fetch(new URL(name, base));
  if (!r.ok) throw new Error(`could not load ${name} (HTTP ${r.status})`);
  return new Int8Array(await r.arrayBuffer());
}

async function init() {
  const [module, sdk, classlib] = await Promise.all([
    load(new URL('compiler.wasm', base).href, { stackDeobfuscator: { enabled: false } }),
    binary('compile-classlib-teavm.bin'),
    binary('runtime-classlib-teavm.bin'),
  ]);
  compiler = module.exports.createCompiler();
  compiler.setSdk(sdk);
  compiler.setTeaVMClasslib(classlib);
  compiler.onDiagnostic(d => {
    diagnostics.push({
      type: d.type, severity: d.severity, fileName: d.fileName, lineNumber: d.lineNumber,
      columnNumber: d.type === 'javac' ? d.columnNumber : undefined, message: d.message,
    });
  });
}

const initialised = init().then(
  () => postMessage({ type: 'ready' }),
  e => postMessage({ type: 'init-error', message: String(e && e.message || e) }));

onmessage = async ({ data }) => {
  await initialised;
  const { id, files } = data;
  diagnostics = [];
  try {
    compiler.clearSourceFiles();
    compiler.clearOutputFiles();
    for (const [name, text] of Object.entries(files)) compiler.addSourceFile(name, text);
    if (!compiler.compile()) {
      postMessage({ id, ok: false, stage: 'javac', diagnostics });
      return;
    }
    const mains = compiler.detectMainClasses();
    if (!mains || mains.length === 0) {
      diagnostics.push({ type: 'javac', severity: 'error', fileName: '', lineNumber: 0,
        message: 'No class has a method public static void main(String[] args).' });
      postMessage({ id, ok: false, stage: 'main', diagnostics });
      return;
    }
    const mainClass = mains.includes('Main') ? 'Main' : mains[0];
    if (!compiler.generateWebAssembly({ outputName: 'app', mainClass })) {
      postMessage({ id, ok: false, stage: 'teavm', diagnostics });
      return;
    }
    const bytes = compiler.getWebAssemblyOutputFile('app.wasm');
    const wasm = bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength);
    postMessage({ id, ok: true, diagnostics, wasm, mainClass }, [wasm]);
  } catch (e) {
    postMessage({ id, ok: false, stage: 'internal', diagnostics:
      [...diagnostics, { type: 'internal', severity: 'error', message: String(e && e.message || e) }] });
  }
};
