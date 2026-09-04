"""Issue HS256 lab JWTs for Lab 5. Same secret as Labs 3–4, plus risk scopes."""
from __future__ import annotations

import argparse
import base64
import hashlib
import hmac
import json
import time

SECRET = "md287-lab-only-hmac-secret-32bytes!"
ISSUER = "md287-lab"

TOKENS = {
    "teller": ("lab-teller", "accounts.read accounts.write"),
    "ops": ("lab-ops", "accounts.read transactions.read transactions.write risk.read"),
    "readonly": ("lab-reader", "accounts.read transactions.read risk.read"),
    "reviewer": ("lab-reviewer", "risk.read risk.write"),
}


def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def issue(subject: str, scope: str, hours: int = 12) -> str:
    header = b64url(json.dumps({"alg": "HS256", "typ": "JWT"}, separators=(",", ":")).encode())
    now = int(time.time())
    payload = {
        "iss": ISSUER,
        "sub": subject,
        "iat": now,
        "exp": now + hours * 3600,
        "scope": scope,
    }
    payload_b64 = b64url(json.dumps(payload, separators=(",", ":")).encode())
    signing_input = f"{header}.{payload_b64}".encode()
    signature = b64url(hmac.new(SECRET.encode(), signing_input, hashlib.sha256).digest())
    return f"{header}.{payload_b64}.{signature}"


def main() -> None:
    parser = argparse.ArgumentParser(description="Issue MD287 Lab 5 JWTs")
    parser.add_argument("role", choices=sorted(TOKENS), help="teller, ops, readonly, or reviewer")
    parser.add_argument("--hours", type=int, default=12)
    args = parser.parse_args()
    subject, scope = TOKENS[args.role]
    print(issue(subject, scope, args.hours))


if __name__ == "__main__":
    main()
