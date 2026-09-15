# Pipeline stage ownership map (Exercise 4.3)

**Module 7** (DevOps and CI/CD) · Day 4 · **Checkpoint C**

Fill the **Who typically owns it** column. The instructor will review it. Do **not** copy from a `solution/` folder.

| Stage | What happens | Who typically owns it | Fail means |
| --- | --- | --- | --- |
| checkout | Clone the reviewed Git revision | | Wrong commit would be built |
| compile | `mvn package` | | Code does not build |
| test | `mvn test` | | Behaviour contract broken |
| analysis (optional) | Static analysis | | Quality bar not met |
| scan | CVE scan of image or dependencies | | **CRITICAL findings block promote** |
| image | Build and push immutable tag | | No artifact to deploy |
| SBOM | Software Bill of Materials attached | | Cannot prove what shipped |
| sign | Cosign (or equivalent) signature | | Unsigned image must not run |
| deploy | Roll out to the namespace | | Cluster reject / probe fail |
| smoke | Readiness + a synthetic API check | | Version is live but not healthy |
| approval | Human promote to a higher env | | No production push without review |

Keep this file. You will confirm scan / SBOM / sign in Lab 4 Step 6.
