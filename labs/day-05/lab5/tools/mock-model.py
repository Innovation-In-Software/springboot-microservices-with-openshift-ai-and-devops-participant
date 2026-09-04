"""Classroom stand-in for an OpenShift AI model endpoint.

POST /v1/score  Authorization: Bearer md287-classroom-model-token

Special amounts (USD):
  13.13  sleep 10s  -> caller times out
  66.66  HTTP 500
Otherwise score = min(99, int(amount)).
"""
from __future__ import annotations

import json
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

API_KEY = "md287-classroom-model-token"
HOST = "0.0.0.0"
PORT = 8090


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt: str, *args) -> None:
        print("[%s] %s" % (self.log_date_time_string(), fmt % args))

    def do_GET(self) -> None:
        if self.path in ("/health", "/v1/health"):
            self._json(200, {"status": "UP", "modelName": "md287-risk-model", "modelVersion": "1.0.0"})
            return
        self._json(404, {"error": "not found"})

    def do_POST(self) -> None:
        if self.path != "/v1/score":
            self._json(404, {"error": "not found"})
            return
        auth = self.headers.get("Authorization", "")
        if auth != f"Bearer {API_KEY}":
            self._json(401, {"error": "unauthorized"})
            return
        length = int(self.headers.get("Content-Length", "0"))
        raw = self.rfile.read(length) if length else b"{}"
        try:
            body = json.loads(raw.decode("utf-8"))
        except json.JSONDecodeError:
            self._json(400, {"error": "malformed json"})
            return
        amount = float(body.get("amount", 0))
        if abs(amount - 13.13) < 0.001:
            time.sleep(10)
        if abs(amount - 66.66) < 0.001:
            self._json(500, {"error": "model backend failed"})
            return
        # Classroom mapping so the sample event files hit APPROVE / HOLD / DECLINE.
        if abs(amount - 25.00) < 0.001:
            score = 12
        elif abs(amount - 80.00) < 0.001:
            score = 80
        elif abs(amount - 250.00) < 0.001:
            score = 55
        elif amount >= 5000:
            score = 10
        else:
            score = max(0, min(99, int(amount)))
        self._json(
            200,
            {
                "score": score,
                "modelName": "md287-risk-model",
                "modelVersion": "1.0.0",
            },
        )

    def _json(self, status: int, payload: dict) -> None:
        data = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)


if __name__ == "__main__":
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    print(f"MD287 mock model listening on {HOST}:{PORT}")
    server.serve_forever()
