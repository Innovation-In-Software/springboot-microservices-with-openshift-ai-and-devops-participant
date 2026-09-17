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

import gzip
import hashlib
import json
import os
import ssl
import subprocess
import sys
import tarfile
import tempfile
import urllib.error
import urllib.request
from pathlib import Path
from typing import Optional

SSL = ssl._create_unverified_context()


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


class Registry:
    def __init__(self, host: str, user: str, token: str) -> None:
        self.base = "https://" + host.rstrip("/")
        self.user = user
        self.token = token

    def _headers(self, extra: Optional[dict] = None) -> dict:
        headers = {
            "Authorization": "Bearer " + self.token,
            "User-Agent": "md287-push-image/1.0",
        }
        if extra:
            headers.update(extra)
        return headers

    def call(
        self,
        method: str,
        url: str,
        data: Optional[bytes] = None,
        headers: Optional[dict] = None,
        timeout: int = 600,
    ):
        req = urllib.request.Request(url, data=data, method=method, headers=self._headers(headers))
        try:
            return urllib.request.urlopen(req, context=SSL, timeout=timeout)
        except urllib.error.HTTPError as err:
            if err.code != 401:
                raise
            # Some ARO seats accept Basic with the kube token as the password.
            basic = (self.user + ":" + self.token).encode("utf-8")
            import base64

            retry_headers = self._headers(headers)
            retry_headers["Authorization"] = "Basic " + base64.b64encode(basic).decode("ascii")
            req = urllib.request.Request(url, data=data, method=method, headers=retry_headers)
            return urllib.request.urlopen(req, context=SSL, timeout=timeout)

    def ensure_v2(self) -> None:
        try:
            self.call("GET", self.base + "/v2/")
        except urllib.error.HTTPError as err:
            if err.code in (200, 401):
                return
            die("Registry GET /v2/ failed HTTP {0}: {1}".format(err.code, err.reason))

    def blob_exists(self, repo: str, digest: str) -> bool:
        url = "{0}/v2/{1}/blobs/sha256:{2}".format(self.base, repo, digest)
        try:
            self.call("HEAD", url)
            return True
        except urllib.error.HTTPError as err:
            if err.code == 404:
                return False
            raise

    def upload_blob(self, repo: str, digest: str, path: Path) -> None:
        if self.blob_exists(repo, digest):
            print("  blob sha256:{0} already on registry".format(digest[:12]))
            return
        start = self.call("POST", "{0}/v2/{1}/blobs/uploads/".format(self.base, repo), data=b"")
        location = start.headers.get("Location")
        if not location:
            die("Registry upload did not return a Location header")
        if location.startswith("/"):
            location = self.base + location
        elif not location.startswith("http"):
            location = self.base + "/" + location.lstrip("/")
        sep = "&" if "?" in location else "?"
        put_url = location + sep + "digest=sha256:" + digest
        size = path.stat().st_size
        print("  uploading sha256:{0} ({1} bytes)".format(digest[:12], size))
        with path.open("rb") as handle:
            body = handle.read()
        self.call(
            "PUT",
            put_url,
            data=body,
            headers={"Content-Type": "application/octet-stream", "Content-Length": str(len(body))},
        )

    def put_manifest(self, repo: str, reference: str, manifest: bytes) -> None:
        url = "{0}/v2/{1}/manifests/{2}".format(self.base, repo, reference)
        self.call(
            "PUT",
            url,
            data=manifest,
            headers={"Content-Type": "application/vnd.docker.distribution.manifest.v2+json"},
        )


def docker_save(image: str, dest: Path) -> None:
    print("Saving {0} ...".format(image))
    subprocess.check_call(["docker", "save", "-o", str(dest), image])


def push_saved_image(archive: Path, registry: Registry, repo: str, reference: str) -> None:
    work = Path(tempfile.mkdtemp(prefix="md287-push-"))
    try:
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
    finally:
        # Best-effort cleanup; image tars are large.
        pass


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
    registry = Registry(host, user, token)
    print("Registry {0}  repo {1}  tag {2}".format(host, repo, reference))
    registry.ensure_v2()
    archive = Path(tempfile.mkdtemp(prefix="md287-save-")) / "image.tar"
    docker_save(local, archive)
    push_saved_image(archive, registry, repo, reference)
    print("Pushed {0}/{1}:{2}".format(host, repo, reference))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
