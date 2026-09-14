# Lab 0 tools

Participant helper for **Lab 0 — Environment Setup**.

| Script | Purpose |
| ------ | ------- |
| [`check-environment.ps1`](check-environment.ps1) | Verify Java 21, Maven, Git, Python, `oc`, `curl.exe`, Docker, and Lab 1 ports |

From the cloned repo root:

```powershell
powershell -ExecutionPolicy Bypass -File "labs\day-00\lab0\tools\check-environment.ps1"
```

Exit code `0` is PASS (warnings allowed). Exit code `1` is FAIL.

Follow **[LAB-0-GUIDE.md](../LAB-0-GUIDE.md)**. Do not run `oc login` from this folder.
