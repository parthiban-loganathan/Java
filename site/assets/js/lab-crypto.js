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

// ---- private key protected by a passphrase: standard PKCS#8 EncryptedPrivateKeyInfo with PBES2
// (PBKDF2-HMAC-SHA256, 600 000 iterations, AES-256-CBC), PEM "ENCRYPTED PRIVATE KEY". OpenSSL
// (openssl pkey -in key.pem) and Python's cryptography package read the same file.
const PBKDF2_ITERATIONS = 600000;
const OID = {
  pbes2: [0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x05, 0x0d],
  pbkdf2: [0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x05, 0x0c],
  hmacSha256: [0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x02, 0x09],
  aes256cbc: [0x60, 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x01, 0x2a],
};

function tlv(tag, ...parts) {
  const body = concat(...parts);
  const n = body.length;
  const len = n < 0x80 ? [n] : n < 0x100 ? [0x81, n] : n < 0x10000 ? [0x82, n >> 8, n & 0xff] : [0x83, n >> 16, (n >> 8) & 0xff, n & 0xff];
  return concat(new Uint8Array([tag, ...len]), body);
}

function concat(...parts) {
  const total = parts.reduce((a, p) => a + p.length, 0);
  const out = new Uint8Array(total);
  let o = 0;
  for (const p of parts) { out.set(p, o); o += p.length; }
  return out;
}

function derInt(v) {
  const bytes = [];
  do { bytes.unshift(v & 0xff); v = Math.floor(v / 256); } while (v > 0);
  if (bytes[0] & 0x80) bytes.unshift(0);
  return tlv(0x02, new Uint8Array(bytes));
}

/** Reads one DER element at offset: {tag, start (of content), end}. */
function derRead(buf, offset) {
  const tag = buf[offset];
  let len = buf[offset + 1];
  let start = offset + 2;
  if (len & 0x80) {
    const n = len & 0x7f;
    len = 0;
    for (let i = 0; i < n; i++) len = len * 256 + buf[start + i];
    start += n;
  }
  if (start + len > buf.length) throw new Error('damaged key file');
  return { tag, start, end: start + len };
}

function derChildren(buf, el) {
  const out = [];
  for (let o = el.start; o < el.end;) { const c = derRead(buf, o); out.push(c); o = c.end; }
  return out;
}

const sameBytes = (buf, el, oid) => el.end - el.start === oid.length && oid.every((b, i) => buf[el.start + i] === b);

async function passphraseKey(passphrase, salt, iterations, usage) {
  const base = await crypto.subtle.importKey('raw', new TextEncoder().encode(passphrase), 'PBKDF2', false, ['deriveKey']);
  return crypto.subtle.deriveKey({ name: 'PBKDF2', hash: 'SHA-256', salt, iterations }, base,
    { name: 'AES-CBC', length: 256 }, false, [usage]);
}

/** PKCS#8 private key (DER) encrypted with a passphrase, as PEM "ENCRYPTED PRIVATE KEY". */
export async function encryptPrivateKey(pkcs8, passphrase) {
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const iv = crypto.getRandomValues(new Uint8Array(16));
  const key = await passphraseKey(passphrase, salt, PBKDF2_ITERATIONS, 'encrypt');
  const ct = new Uint8Array(await crypto.subtle.encrypt({ name: 'AES-CBC', iv }, key, pkcs8));
  const der = tlv(0x30,
    tlv(0x30,
      tlv(0x06, new Uint8Array(OID.pbes2)),
      tlv(0x30,
        tlv(0x30, tlv(0x06, new Uint8Array(OID.pbkdf2)),
          tlv(0x30, tlv(0x04, salt), derInt(PBKDF2_ITERATIONS), tlv(0x30, tlv(0x06, new Uint8Array(OID.hmacSha256)), new Uint8Array([0x05, 0x00])))),
        tlv(0x30, tlv(0x06, new Uint8Array(OID.aes256cbc)), tlv(0x04, iv)))),
    tlv(0x04, ct));
  return derToPem(der, 'ENCRYPTED PRIVATE KEY');
}

/** PKCS#8 DER of a private key PEM; an ENCRYPTED PRIVATE KEY needs its passphrase. */
export async function privateKeyDer(pem, passphrase = '') {
  if (/-----BEGIN PRIVATE KEY-----/.test(pem)) return pemToDer(pem, 'PRIVATE KEY');
  const der = pemToDer(pem, 'ENCRYPTED PRIVATE KEY');
  const [alg, data] = derChildren(der, derRead(der, 0));
  const [algOid, params] = derChildren(der, alg);
  if (!sameBytes(der, algOid, OID.pbes2)) throw new Error('unsupported key encryption (expected PBES2)');
  const [kdf, enc] = derChildren(der, params);
  const [kdfOid, kdfParams] = derChildren(der, kdf);
  if (!sameBytes(der, kdfOid, OID.pbkdf2)) throw new Error('unsupported key derivation (expected PBKDF2)');
  const kp = derChildren(der, kdfParams);
  const salt = der.slice(kp[0].start, kp[0].end);
  let iterations = 0;
  for (let i = kp[1].start; i < kp[1].end; i++) iterations = iterations * 256 + der[i];
  const prf = kp.find(e => e.tag === 0x30);
  if (prf && !sameBytes(der, derChildren(der, prf)[0], OID.hmacSha256)) throw new Error('unsupported key derivation hash (expected SHA-256)');
  const [encOid, ivEl] = derChildren(der, enc);
  if (!sameBytes(der, encOid, OID.aes256cbc)) throw new Error('unsupported key cipher (expected AES-256-CBC)');
  if (!passphrase) throw new Error('this private key is protected: enter its passphrase');
  const key = await passphraseKey(passphrase, salt, iterations, 'decrypt');
  try {
    return new Uint8Array(await crypto.subtle.decrypt({ name: 'AES-CBC', iv: der.slice(ivEl.start, ivEl.end) }, key,
      der.slice(data.start, data.end)));
  } catch {
    throw new Error('wrong passphrase for this private key');
  }
}

/**
 * New teacher key pair: {publicPem, privatePem, keyId}. With a passphrase the private key is
 * saved encrypted (ENCRYPTED PRIVATE KEY); without one it is plain PKCS#8 (PRIVATE KEY).
 */
export async function generateKeyPair(passphrase = '') {
  const pair = await crypto.subtle.generateKey(
    { ...RSA, modulusLength: 4096, publicExponent: new Uint8Array([1, 0, 1]) }, true, ['encrypt', 'decrypt']);
  const publicPem = derToPem(await crypto.subtle.exportKey('spki', pair.publicKey), 'PUBLIC KEY');
  const pkcs8 = new Uint8Array(await crypto.subtle.exportKey('pkcs8', pair.privateKey));
  const privatePem = passphrase ? await encryptPrivateKey(pkcs8, passphrase) : derToPem(pkcs8, 'PRIVATE KEY');
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

/** Private key (PEM, plain or passphrase-protected) as a Web Crypto key for decrypting packages. */
export async function importPrivateKey(privatePem, passphrase = '') {
  return crypto.subtle.importKey('pkcs8', await privateKeyDer(privatePem, passphrase), RSA, false, ['decrypt']);
}

/**
 * Decrypts an armoured package. `key`: the private key as PEM text (with `passphrase` when it is
 * protected) or a key from importPrivateKey. Result: {data, sha256, keyId}.
 */
export async function decryptPackage(text, key, passphrase = '') {
  const bin = unarmour(text);
  const n = (bin[12] << 8) | bin[13];
  const wrapped = bin.subarray(14, 14 + n);
  const iv = bin.subarray(14 + n, 26 + n);
  const ct = bin.subarray(26 + n);
  const rsaKey = typeof key === 'string' ? await importPrivateKey(key, passphrase) : key;
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
