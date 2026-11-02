#!/usr/bin/env python3
"""Encrypted lab submission packages: the same format as site/assets/js/lab-crypto.js.

    binary = b"LAB1" | key id (8) | wrapped key length (uint16 BE) | RSA-OAEP-SHA256(AES key) | IV (12) | AES-256-GCM(gzip(JSON)) + tag
    armour = -----BEGIN LAB SUBMISSION----- / base64 / -----END LAB SUBMISSION-----

Command line (teacher):
    python3 tools/lab/labcrypto.py keygen DIR          new key pair: DIR/teacher-public-key.pem, DIR/lab-private-key.pem
    python3 tools/lab/labcrypto.py decrypt KEY.pem FILE  print the decrypted JSON of a package (FILE may be a whole issue text)
Needs the "cryptography" package (Apache-2.0/BSD): pip install cryptography
"""
import base64
import gzip
import hashlib
import json
import os
import re
import sys

try:  # only decrypt, encrypt and keygen need it; reading package headers works without it
    from cryptography.hazmat.primitives import hashes, serialization
    from cryptography.hazmat.primitives.asymmetric import padding, rsa
    from cryptography.hazmat.primitives.ciphers.aead import AESGCM
except ImportError:  # pragma: no cover
    hashes = serialization = padding = rsa = AESGCM = None

MAGIC = b"LAB1"
BEGIN = "-----BEGIN LAB SUBMISSION-----"
END = "-----END LAB SUBMISSION-----"


def _oaep():
    return padding.OAEP(mgf=padding.MGF1(algorithm=hashes.SHA256()), algorithm=hashes.SHA256(), label=None)
MAX_BYTES = 64 * 1024


class PackageError(ValueError):
    pass


def key_id_of_public(public_key):
    der = public_key.public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)
    return hashlib.sha256(der).hexdigest()[:16]


def key_id(public_pem):
    """Key id from a PEM public key (SubjectPublicKeyInfo); needs only the standard library."""
    text = public_pem.decode() if isinstance(public_pem, bytes) else public_pem
    m = re.search(r"-----BEGIN PUBLIC KEY-----(.*?)-----END PUBLIC KEY-----", text, re.S)
    if not m:
        raise PackageError("not a PEM public key")
    return hashlib.sha256(base64.b64decode(re.sub(r"\s+", "", m.group(1)))).hexdigest()[:16]


def find_packages(text):
    """All armoured blocks in a text, as binary packages (invalid blocks are skipped)."""
    out = []
    for m in re.finditer(re.escape(BEGIN) + r"(.*?)" + re.escape(END), text, re.S):
        try:
            out.append(unarmour_block(m.group(1)))
        except PackageError:
            pass
    return out


def unarmour_block(body):
    b64 = re.sub(r"\s+", "", body)
    if not re.fullmatch(r"[A-Za-z0-9+/]+={0,2}", b64 or "-"):
        raise PackageError("package contains characters that are not base64")
    if len(b64) * 3 // 4 > MAX_BYTES:
        raise PackageError("package too large")
    try:
        binary = base64.b64decode(b64, validate=True)
    except ValueError as e:
        raise PackageError("package is not valid base64: %s" % e)
    check_structure(binary)
    return binary


def unarmour(text):
    i, j = text.find(BEGIN), text.find(END)
    if i < 0 or j < i:
        raise PackageError("no LAB SUBMISSION block found")
    return unarmour_block(text[i + len(BEGIN):j])


def check_structure(binary):
    if len(binary) < 4 + 8 + 2 + 12 + 16 or binary[:4] != MAGIC:
        raise PackageError("not a lab submission package")
    n = int.from_bytes(binary[12:14], "big")
    if n not in (256, 384, 512) or len(binary) < 14 + n + 12 + 16:
        raise PackageError("damaged package (incomplete copy?)")


def package_key_id(binary):
    return binary[4:12].hex()


def sha256(binary):
    return hashlib.sha256(binary).hexdigest()


def armour(binary):
    b64 = base64.b64encode(binary).decode()
    return BEGIN + "\n" + "\n".join(b64[i:i + 76] for i in range(0, len(b64), 76)) + "\n" + END


def load_private_key(pem):
    return serialization.load_pem_private_key(pem.encode() if isinstance(pem, str) else pem, password=None)


def decrypt(binary, private_key):
    """Returns the submitted JSON object. Raises PackageError when the key or the data is wrong."""
    check_structure(binary)
    n = int.from_bytes(binary[12:14], "big")
    wrapped, iv, ct = binary[14:14 + n], binary[14 + n:26 + n], binary[26 + n:]
    if package_key_id(binary) != key_id_of_public(private_key.public_key()):
        raise PackageError("package was encrypted for key %s, not for this private key" % package_key_id(binary))
    try:
        aes = private_key.decrypt(wrapped, _oaep())
        plain = AESGCM(aes).decrypt(iv, ct, None)
        data = json.loads(gzip.decompress(plain).decode("utf-8"))
    except Exception as e:  # wrong key, altered bytes, not gzip or not JSON
        raise PackageError("package cannot be decrypted (%s)" % type(e).__name__)
    if not isinstance(data, dict):
        raise PackageError("package content is not an object")
    return data


def encrypt(obj, public_pem):
    """Same as the browser (used by the tests)."""
    pub = serialization.load_pem_public_key(public_pem.encode() if isinstance(public_pem, str) else public_pem)
    aes = AESGCM.generate_key(bit_length=256)
    iv = os.urandom(12)
    wrapped = pub.encrypt(aes, _oaep())
    ct = AESGCM(aes).encrypt(iv, gzip.compress(json.dumps(obj).encode("utf-8")), None)
    return MAGIC + bytes.fromhex(key_id_of_public(pub)) + len(wrapped).to_bytes(2, "big") + wrapped + iv + ct


def keygen(directory):
    os.makedirs(directory, exist_ok=True)
    key = rsa.generate_private_key(public_exponent=65537, key_size=4096)
    pub = key.public_key().public_bytes(serialization.Encoding.PEM, serialization.PublicFormat.SubjectPublicKeyInfo)
    priv = key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption())
    with open(os.path.join(directory, "teacher-public-key.pem"), "wb") as f:
        f.write(pub)
    path = os.path.join(directory, "lab-private-key.pem")
    with open(os.open(path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600), "wb") as f:
        f.write(priv)
    print("key id", key_id(pub))
    print("public key :", os.path.join(directory, "teacher-public-key.pem"), "(commit as lab/teacher-public-key.pem)")
    print("private key:", path, "(keep secret: GitHub secret LAB_PRIVATE_KEY of the gradebook)")


if __name__ == "__main__":
    if len(sys.argv) == 3 and sys.argv[1] == "keygen":
        keygen(sys.argv[2])
    elif len(sys.argv) == 4 and sys.argv[1] == "decrypt":
        with open(sys.argv[2]) as f:
            key = load_private_key(f.read())
        with open(sys.argv[3]) as f:
            print(json.dumps(decrypt(unarmour(f.read()), key), indent=1, ensure_ascii=False))
    else:
        print(__doc__)
        sys.exit(2)
