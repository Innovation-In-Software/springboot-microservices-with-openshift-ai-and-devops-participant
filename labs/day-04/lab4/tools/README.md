# Shared tools for Lab 4

Use these from `labs\day-04\lab4` as in [LAB-4-GUIDE.md](../LAB-4-GUIDE.md). Do **not** copy from a `solution/` folder.

| Script / file | Purpose |
| --- | --- |
| `push-images.ps1` | Tag and push `md287/account-service:1.0.0` and `md287/transaction-service:1.0.0` to the assigned OpenShift project (Lab 4 Step 5). Isolated docker login, then skopeo / `oc image mirror` / Python registry push. |
| `generate-sbom.ps1` | Syft if installed; otherwise copies the classroom CycloneDX sample |
| `sample-sbom-account-service.json` | Fallback SBOM for review |
| `scan-gate.ps1` | Exit 1 if any CRITICAL finding exists |
| `sample-scan-pass.json` / `sample-scan-fail.json` | Gate demos |
| `verify-signature.ps1` | Classroom Cosign stand-in |
| `run-pipeline-locally.ps1` | Walk checkout → smoke without Tekton |
| `requests/` | Synthetic JSON bodies for the Compose smoke path |

JWT helper stays in Lab 3: `labs/day-03/lab3/tools/issue-jwt.py`.
