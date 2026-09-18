#!/usr/bin/env python3
"""Push a local Docker image to the OpenShift integrated registry.

Used when Docker Desktop's Windows credential helper returns HTTP 403, or when
the cluster registry cert is not in the VM trust store.

Stdlib only. Example:
  python labs/tools/push_image.py md287/account-service:1.0.0 ^
    default-route-openshift-image-registry.apps.aro-md287.centralus.aroapp.io ^
    md287-student12/account-service:1.0.0
"""
from __future__ import annotations

import base64
import gzip
import hashlib
import json
import os
import re
import ssl
import subprocess
import sys
import tarfile
import tempfile
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Optional

SSL = ssl._create_unverified_context()
UA = "md287-push-image/1.2"


class _NoRedirect(urllib.request.HTTPRedirectHandler):
    """urllib strips Authorization on 302. We follow redirects ourselves."""

    def http_error_302(self, req, fp, code, msg, headers):  # noqa: ANN001
        raise urllib.error.HTTPError(req.full_url, code, msg, headers, fp)

    http_error_301 = http_error_303 = http_error_307 = http_error_308 = http_error_302


def die(msg: str, code: int = 1) -> None:
    print("ERROR: " + msg, file=sys.stderr)
    raise SystemExit(code)


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def gzip_file(src: Path, dest: Path) -> None:
    with src.open("rb") as raw, dest.open("wb") as out:
        with gzip.GzipFile(fileobj=out, mode="wb", compresslevel=6, mtime=0) as gz:
            while True:
                chunk = raw.read(1024 * 1024)
                if not chunk:
                    break
                gz.write(chunk)


def parse_www_authenticate(header: str) -> dict:
    result = {"scheme": ""}
    if not header:
        return result
    parts = header.strip().split(None, 1)
    result["scheme"] = parts[0]
    rest = parts[1] if len(parts) > 1 else ""
    for match in re.finditer(r'([a-zA-Z0-9_-]+)=("(?:\\.|[^"\\])*"|[^,\s]+)', rest):
        key, value = match.group(1), match.group(2)
        if value.startswith('"') and value.endswith('"'):
            value = value[1:-1].replace('\\"', '"')
        result[key] = value
    return result


def error_body(err: urllib.error.HTTPError) -> str:
    try:
        return err.read().decode("utf-8", "replace")[:2000]
    except Exception:
        return ""


def html_unavailable(text: str) -> bool:
    lowered = text.lower()
    return "application is not available" in lowered or "the host doesn't exist" in lowered


class Registry:
    def __init__(self, host: str, user: str, token: str, repo: str) -> None:
        self.base = "https://" + host.rstrip("/")
        self.user = user
        self.token = token
        self.repo = repo
        self.auth: Optional[str] = None
        self._opener = urllib.request.build_opener(
            urllib.request.HTTPSHandler(context=SSL),
            _NoRedirect(),
        )

    def _abs(self, url: str) -> str:
        if url.startswith("/"):
            url = self.base + url
        parsed = urllib.parse.urlparse(url)
        scheme = parsed.scheme or "https"
        if scheme == "http":
            scheme = "https"
        return urllib.parse.urlunparse(
            (scheme, parsed.netloc, parsed.path, parsed.params, parsed.query, parsed.fragment)
        )

    def _registry_host(self) -> str:
        return (urllib.parse.urlparse(self.base).hostname or "").lower()

    def _headers(self, extra: Optional[dict] = None) -> dict:
        headers = {"User-Agent": UA}
        if self.auth:
            headers["Authorization"] = self.auth
        if extra:
            headers.update(extra)
        return headers

    def _die_http(self, err: urllib.error.HTTPError, url: str) -> None:
        body = error_body(err)
        if html_unavailable(body):
            die(
                "Registry host {0} is not serving (OpenShift router default page). "
                "Confirm MD287_REGISTRY is default-route-openshift-image-registry.apps."
                "aro-md287.centralus.aroapp.io and that IIS exposed that Route. "
                "The Account/Transaction Route HTML is expected until images are pushed "
                "and pods are Ready — that is not the registry.".format(self._registry_host())
            )
        if "authentication information is not given" in body.lower() or err.reason.lower().find(
            "authentication information is not given"
        ) >= 0:
            die(
                "Registry HTTP {0} at {1}: Authentication information is not given. "
                "oc whoami must be studentNN (not student.VLAB). Re-run after git pull.".format(
                    err.code, url
                )
            )
        snippet = body.replace("\n", " ").strip()[:400]
        extra = (" body=" + snippet) if snippet else ""
        die("Registry HTTP {0} {1} at {2}{3}".format(err.code, err.reason, url, extra))

    def _oauth_token_url(self) -> Optional[str]:
        host = self._registry_host()
        marker = ".apps."
        if marker not in host:
            return None
        suffix = host.split(marker, 1)[1]
        return "https://oauth-openshift.apps.{0}/openshift/token".format(suffix)

    def _fetch_bearer(self, challenge: dict) -> Optional[str]:
        realm = challenge.get("realm") or self._oauth_token_url()
        if not realm:
            return None
        service = challenge.get("service") or "docker-registry"
        scope = challenge.get("scope") or "repository:{0}:pull,push".format(self.repo)
        if "push" not in scope:
            scope = "repository:{0}:pull,push".format(self.repo)
        parsed = urllib.parse.urlparse(realm)
        query = dict(urllib.parse.parse_qsl(parsed.query))
        query["service"] = service
        query["scope"] = scope
        url = urllib.parse.urlunparse(
            (
                parsed.scheme or "https",
                parsed.netloc,
                parsed.path,
                parsed.params,
                urllib.parse.urlencode(query),
                parsed.fragment,
            )
        )
        users = []
        for candidate in (self.user, "unused"):
            if candidate and candidate not in users:
                users.append(candidate)
        for username in users:
            basic = base64.b64encode((username + ":" + self.token).encode("utf-8")).decode("ascii")
            req = urllib.request.Request(
                url,
                method="GET",
                headers={
                    "Authorization": "Basic " + basic,
                    "Accept": "application/json",
                    "User-Agent": UA,
                },
            )
            try:
                with urllib.request.urlopen(req, context=SSL, timeout=60) as resp:
                    payload = json.loads(resp.read().decode("utf-8"))
            except Exception as exc:
                print("  token exchange as {0} failed: {1}".format(username, exc))
                continue
            token = payload.get("token") or payload.get("access_token")
            if token:
                print("  obtained registry token (user {0})".format(username))
                return token
        return None

    def _set_basic(self, username: str) -> None:
        raw = base64.b64encode((username + ":" + self.token).encode("utf-8")).decode("ascii")
        self.auth = "Basic " + raw

    def _authenticate(self, err: urllib.error.HTTPError) -> None:
        www = err.headers.get("WWW-Authenticate") or err.headers.get("Www-Authenticate") or ""
        challenge = parse_www_authenticate(www)
        scheme = (challenge.get("scheme") or "").lower()
        if scheme == "bearer" or challenge.get("realm") or self._oauth_token_url():
            token = self._fetch_bearer(challenge)
            if token:
                self.auth = "Bearer " + token
                return
        self._set_basic(self.user)

    def request(
        self,
        method: str,
        url: str,
        data: Optional[bytes] = None,
        headers: Optional[dict] = None,
        timeout: int = 600,
        allow_auth: bool = True,
    ):
        url = self._abs(url)
        hops = 0
        auth_tries = 0
        while hops < 12:
            req = urllib.request.Request(
                url, data=data, method=method, headers=self._headers(headers)
            )
            try:
                return self._opener.open(req, timeout=timeout)
            except urllib.error.HTTPError as err:
                if err.code in (301, 302, 303, 307, 308):
                    loc = err.headers.get("Location")
                    if not loc:
                        self._die_http(err, url)
                    nxt = self._abs(urllib.parse.urljoin(url, loc))
                    nxt_host = (urllib.parse.urlparse(nxt).hostname or "").lower()
                    if nxt_host and nxt_host != self._registry_host():
                        if allow_auth and auth_tries < 3:
                            auth_tries += 1
                            self._authenticate(err)
                            hops += 1
                            continue
                        self._die_http(err, url)
                    url = nxt
                    hops += 1
                    continue
                if err.code == 401 and allow_auth and auth_tries < 3:
                    auth_tries += 1
                    self._authenticate(err)
                    hops += 1
                    continue
                reason = (err.reason or "").lower()
                if (
                    err.code == 400
                    and allow_auth
                    and auth_tries < 3
                    and "authentication information is not given" in reason
                ):
                    auth_tries += 1
                    if not self.auth:
                        self._set_basic(self.user)
                    self._authenticate(err)
                    hops += 1
                    continue
                raise
        die("Too many redirects contacting {0}".format(url))

    def login(self) -> None:
        probe = self.base + "/v2/"
        try:
            resp = self.request("GET", probe, timeout=30)
        except urllib.error.HTTPError as err:
            body = error_body(err)
            if html_unavailable(body):
                self._die_http(err, probe)
            for username in (self.user, "unused"):
                if not username:
                    continue
                self._set_basic(username)
                try:
                    resp = self.request("GET", probe, timeout=30, allow_auth=False)
                    print("  registry login Basic ({0})".format(username))
                    self._assert_registry_api(resp, probe)
                    return
                except urllib.error.HTTPError as retry:
                    if retry.code not in (401, 400):
                        self._die_http(retry, probe)
            self.auth = "Bearer " + self.token
            try:
                resp = self.request("GET", probe, timeout=30, allow_auth=True)
            except urllib.error.HTTPError as retry:
                self._die_http(retry, probe)
        self._assert_registry_api(resp, probe)

    def _assert_registry_api(self, resp, url: str) -> None:
        api = resp.headers.get("Docker-Distribution-API-Version", "")
        ctype = (resp.headers.get("Content-Type") or "").lower()
        peek = resp.read(800).decode("utf-8", "replace")
        if html_unavailable(peek) or "text/html" in ctype:
            die(
                "GET {0} returned the OpenShift router page, not the image registry. "
                "Set MD287_REGISTRY to default-route-openshift-image-registry.apps."
                "aro-md287.centralus.aroapp.io (not the Account Route).".format(url)
            )
        if api or peek.strip() in ("", "{}", "null"):
            return
        if not api and peek.strip().startswith("<"):
            die("GET {0} returned HTML, not registry/2.0. Wrong host.".format(url))

    def blob_exists(self, repo: str, digest: str) -> bool:
        url = "{0}/v2/{1}/blobs/sha256:{2}".format(self.base, repo, digest)
        try:
            self.request("HEAD", url, timeout=60)
            return True
        except urllib.error.HTTPError as err:
            if err.code == 404:
                return False
            if err.code in (400, 405):
                try:
                    self.request(
                        "GET",
                        url,
                        headers={"Range": "bytes=0-0"},
                        timeout=60,
                    )
                    return True
                except urllib.error.HTTPError as retry:
                    if retry.code == 404:
                        return False
                    self._die_http(retry, url)
            self._die_http(err, url)
            return False

    def upload_blob(self, repo: str, digest: str, path: Path) -> None:
        if self.blob_exists(repo, digest):
            print("  blob sha256:{0} already on registry".format(digest[:12]))
            return
        start_url = "{0}/v2/{1}/blobs/uploads/".format(self.base, repo)
        try:
            start = self.request("POST", start_url, data=b"", timeout=60)
        except urllib.error.HTTPError as err:
            self._die_http(err, start_url)
        location = start.headers.get("Location")
        if not location:
            die("Registry upload did not return a Location header")
        location = self._abs(urllib.parse.urljoin(self.base + "/", location))
        sep = "&" if "?" in location else "?"
        put_url = location + sep + "digest=sha256:" + digest
        size = path.stat().st_size
        print("  uploading sha256:{0} ({1} bytes)".format(digest[:12], size))
        with path.open("rb") as handle:
            body = handle.read()
        try:
            self.request(
                "PUT",
                put_url,
                data=body,
                headers={
                    "Content-Type": "application/octet-stream",
                    "Content-Length": str(len(body)),
                },
            )
        except urllib.error.HTTPError as err:
            self._die_http(err, put_url)

    def put_manifest(self, repo: str, reference: str, manifest: bytes) -> None:
        url = "{0}/v2/{1}/manifests/{2}".format(self.base, repo, reference)
        try:
            self.request(
                "PUT",
                url,
                data=manifest,
                headers={"Content-Type": "application/vnd.docker.distribution.manifest.v2+json"},
            )
        except urllib.error.HTTPError as err:
            self._die_http(err, url)


def docker_save(image: str, dest: Path) -> None:
    print("Saving {0} ...".format(image))
    subprocess.check_call(["docker", "save", "-o", str(dest), image])


def push_saved_image(archive: Path, registry: Registry, repo: str, reference: str) -> None:
    work = Path(tempfile.mkdtemp(prefix="md287-push-"))
    with tarfile.open(archive, "r") as tar:
        tar.extractall(work)
    manifests = json.loads((work / "manifest.json").read_text(encoding="utf-8"))
    if not manifests:
        die("docker save archive had an empty manifest.json")
    item = manifests[0]
    config_rel = item["Config"]
    config_path = work / config_rel
    config_digest = sha256_file(config_path)
    registry.upload_blob(repo, config_digest, config_path)

    layers = []
    for layer_rel in item["Layers"]:
        raw = work / layer_rel
        gz_path = raw.with_suffix(raw.suffix + ".gz")
        gzip_file(raw, gz_path)
        digest = sha256_file(gz_path)
        registry.upload_blob(repo, digest, gz_path)
        layers.append(
            {
                "mediaType": "application/vnd.docker.image.rootfs.diff.tar.gzip",
                "size": gz_path.stat().st_size,
                "digest": "sha256:" + digest,
            }
        )

    manifest = {
        "schemaVersion": 2,
        "mediaType": "application/vnd.docker.distribution.manifest.v2+json",
        "config": {
            "mediaType": "application/vnd.docker.container.image.v1+json",
            "size": config_path.stat().st_size,
            "digest": "sha256:" + config_digest,
        },
        "layers": layers,
    }
    body = json.dumps(manifest, separators=(",", ":")).encode("utf-8")
    print("Putting manifest {0}:{1}".format(repo, reference))
    registry.put_manifest(repo, reference, body)


def main(argv: list[str]) -> int:
    if len(argv) != 4:
        die(
            "Usage: push_image.py <local-image:tag> <registry-host> <project/name:tag>\n"
            "Set OC_USER and OC_TOKEN, or run from the PowerShell helper which does."
        )
    local, host, remote = argv[1], argv[2], argv[3]
    user = os.environ.get("OC_USER", "").strip()
    token = os.environ.get("OC_TOKEN", "").strip()
    if not user or not token:
        die("OC_USER and OC_TOKEN must be set (the helper script sets them from oc whoami).")
    if ":" not in remote:
        die("Remote image must look like project/name:tag")
    repo, reference = remote.rsplit(":", 1)
    registry = Registry(host, user, token, repo)
    print("Registry {0}  repo {1}  tag {2}".format(host, repo, reference))
    registry.login()
    archive = Path(tempfile.mkdtemp(prefix="md287-save-")) / "image.tar"
    docker_save(local, archive)
    push_saved_image(archive, registry, repo, reference)
    print("Pushed {0}/{1}:{2}".format(host, repo, reference))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
