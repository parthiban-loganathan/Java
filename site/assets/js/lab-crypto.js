// Encrypted submission packages for the Lab page (Web Crypto, no library).
//
// The student's page encrypts with the teacher's PUBLIC key, so the code can be posted in a
// public GitHub issue and only the holder of the PRIVATE key can read it.
//   payload  = gzip(UTF-8 JSON)
//   key      = random AES-256-GCM key, wrapped with RSA-OAEP (SHA-256, 4096-bit key)
//   binary   = "LAB1" | key id (8 bytes) | wrapped key length (uint16 BE) | wrapped key | IV (12) | ciphertext+tag
//   armour   = -----BEGIN LAB SUBMISSION----- / base64, 76 per line / -----END LAB SUBMISSION-----
// key id = first 8 bytes of SHA-256(public key, SubjectPublicKeyInfo DER), shown as 16 hex digits.
// tools/lab/labcrypto.py reads and writes the same format. Works in browsers and Node 20+.

const MAGIC = [0x4c, 0x41, 0x42, 0x31];           // "LAB1"
export const BEGIN = '-----BEGIN LAB SUBMISSION-----';
export const END = '-----END LAB SUBMISSION-----';
const RSA = { name: 'RSA-OAEP', hash: 'SHA-256' };

function pemToDer(pem, label) {
  const m = pem.match(new RegExp(`-----BEGIN ${label}-----([\\s\\S]+?)-----END ${label}-----`));
  if (!m) throw new Error(`not a PEM ${label}`);
  return fromBase64(m[1]);
}

function derToPem(der, label) {
  const b = toBase64(new Uint8Array(der));
  return `-----BEGIN ${label}-----\n${b.match(/.{1,64}/g).join('\n')}\n-----END ${label}-----\n`;
}

export function toBase64(bytes) {
  let s = '';
  for (let i = 0; i < bytes.length; i += 0x8000) s += String.fromCharCode(...bytes.subarray(i, i + 0x8000));
  return btoa(s);
}

export function fromBase64(text) {
  const s = atob(text.replace(/[^A-Za-z0-9+/=]/g, ''));
  const out = new Uint8Array(s.length);
  for (let i = 0; i < s.length; i++) out[i] = s.charCodeAt(i);
  return out;
}

export function hex(bytes) {
  return [...bytes].map(b => b.toString(16).padStart(2, '0')).join('');
}

export async function sha256(bytes) {
  return new Uint8Array(await crypto.subtle.digest('SHA-256', bytes));
}

async function streamBytes(bytes, transform) {
  const stream = new Blob([bytes]).stream().pipeThrough(transform);
  return new Uint8Array(await new Response(stream).arrayBuffer());
}

/** 16 hex digits identifying a public key (PEM). */
export async function keyId(publicPem) {
  return hex((await sha256(pemToDer(publicPem, 'PUBLIC KEY'))).subarray(0, 8));
}

/** New teacher key pair as PEM texts {publicPem, privatePem, keyId}. */
export async function generateKeyPair() {
  const pair = await crypto.subtle.generateKey(
    { ...RSA, modulusLength: 4096, publicExponent: new Uint8Array([1, 0, 1]) }, true, ['encrypt', 'decrypt']);
  const publicPem = derToPem(await crypto.subtle.exportKey('spki', pair.publicKey), 'PUBLIC KEY');
  const privatePem = derToPem(await crypto.subtle.exportKey('pkcs8', pair.privateKey), 'PRIVATE KEY');
  return { publicPem, privatePem, keyId: await keyId(publicPem) };
}

/** Encrypts a JSON-serialisable object. Result: {armour, sha256 (hex of the binary), bytes}. */
export async function encryptPackage(obj, publicPem) {
  const spki = pemToDer(publicPem, 'PUBLIC KEY');
  const id = (await sha256(spki)).subarray(0, 8);
  const rsaKey = await crypto.subtle.importKey('spki', spki, RSA, false, ['encrypt']);
  const aesKey = await crypto.subtle.generateKey({ name: 'AES-GCM', length: 256 }, true, ['encrypt']);
  const raw = new Uint8Array(await crypto.subtle.exportKey('raw', aesKey));
  const wrapped = new Uint8Array(await crypto.subtle.encrypt(RSA, rsaKey, raw));
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const plain = await streamBytes(new TextEncoder().encode(JSON.stringify(obj)), new CompressionStream('gzip'));
  const ct = new Uint8Array(await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, aesKey, plain));
  const bin = new Uint8Array(4 + 8 + 2 + wrapped.length + 12 + ct.length);
  let o = 0;
  bin.set(MAGIC, o); o += 4;
  bin.set(id, o); o += 8;
  bin[o++] = wrapped.length >> 8; bin[o++] = wrapped.length & 0xff;
  bin.set(wrapped, o); o += wrapped.length;
  bin.set(iv, o); o += 12;
  bin.set(ct, o);
  const armour = `${BEGIN}\n${toBase64(bin).match(/.{1,76}/g).join('\n')}\n${END}`;
  return { armour, sha256: hex(await sha256(bin)), bytes: bin.length };
}

/** Binary package from text that contains an armoured block (e.g. a whole issue comment). */
export function unarmour(text) {
  const i = text.indexOf(BEGIN), j = text.indexOf(END);
  if (i < 0 || j < i) throw new Error('no LAB SUBMISSION block found');
  const bin = fromBase64(text.slice(i + BEGIN.length, j));
  if (bin.length < 4 + 8 + 2 + 12 + 16 || MAGIC.some((b, k) => bin[k] !== b)) throw new Error('not a lab submission package');
  return bin;
}

/** Key id written in a package (hex), without decrypting it. */
export function packageKeyId(bin) {
  return hex(bin.subarray(4, 12));
}

/** Decrypts an armoured package with the teacher's private key (PEM). Result: {data, sha256, keyId}. */
export async function decryptPackage(text, privatePem) {
  const bin = unarmour(text);
  const n = (bin[12] << 8) | bin[13];
  const wrapped = bin.subarray(14, 14 + n);
  const iv = bin.subarray(14 + n, 26 + n);
  const ct = bin.subarray(26 + n);
  const rsaKey = await crypto.subtle.importKey('pkcs8', pemToDer(privatePem, 'PRIVATE KEY'), RSA, false, ['decrypt']);
  let raw;
  try {
    raw = await crypto.subtle.decrypt(RSA, rsaKey, wrapped);
  } catch {
    throw new Error(`this package was encrypted for key ${packageKeyId(bin)}, not for the private key given`);
  }
  const aesKey = await crypto.subtle.importKey('raw', raw, 'AES-GCM', false, ['decrypt']);
  const plain = new Uint8Array(await crypto.subtle.decrypt({ name: 'AES-GCM', iv }, aesKey, ct));
  const json = new TextDecoder().decode(await streamBytes(plain, new DecompressionStream('gzip')));
  return { data: JSON.parse(json), sha256: hex(await sha256(bin)), keyId: packageKeyId(bin) };
}
