#!/usr/bin/env python3
"""
Turn an RSA private key (PEM) into a private JWK and a public JWKS, using only openssl and the
standard library.

  jwk.py KEY_PEM KID JWK_OUT JWKS_OUT
"""

import base64
import json
import re
import subprocess
import sys

FIELDS = {
    "modulus": "n",
    "publicExponent": "e",
    "privateExponent": "d",
    "prime1": "p",
    "prime2": "q",
    "exponent1": "dp",
    "exponent2": "dq",
    "coefficient": "qi",
}


def b64url_int(value):
    raw = value.to_bytes((value.bit_length() + 7) // 8, "big")
    return base64.urlsafe_b64encode(raw).rstrip(b"=").decode()


def parse_openssl_text(text):
    """Parse `openssl rsa -text` output into {field: int}."""
    values, current, hex_parts = {}, None, []

    def flush():
        if current and hex_parts:
            values[current] = int("".join(hex_parts).replace(":", ""), 16)

    for line in text.splitlines():
        m = re.match(r"^(\w+):\s*(.*)$", line)
        if m and m.group(1) in FIELDS:
            flush()
            current, hex_parts = m.group(1), []
            rest = m.group(2).strip()
            if current == "publicExponent":
                values[current] = int(rest.split()[0])
                current = None
            elif rest:
                hex_parts.append(rest)
        elif m:
            flush()
            current, hex_parts = None, []
        elif current and line.startswith(" "):
            hex_parts.append(line.strip())
    flush()
    return values


def main(key_pem, kid, jwk_out, jwks_out):
    text = subprocess.run(
        ["openssl", "rsa", "-in", key_pem, "-noout", "-text"],
        check=True, capture_output=True, text=True,
    ).stdout
    values = parse_openssl_text(text)
    missing = [k for k in FIELDS if k not in values]
    if missing:
        sys.exit(f"Could not read {missing} from {key_pem}")

    meta = {"kid": kid, "alg": "PS256", "use": "sig"}
    jwk = {"kty": "RSA", **{FIELDS[k]: b64url_int(v) for k, v in values.items()}, **meta}
    public = {k: jwk[k] for k in ("kty", "n", "e")}
    public.update(meta)

    with open(jwk_out, "w") as f:
        json.dump(jwk, f)
    with open(jwks_out, "w") as f:
        json.dump({"keys": [public]}, f, indent=2)


if __name__ == "__main__":
    if len(sys.argv) != 5:
        sys.exit(__doc__)
    main(*sys.argv[1:])
