# Lab 4 — Deploy the Capstone Services

**Day:** 4 — Observability, Containers, OpenShift, and CI/CD  
**Capstone:** Package and operate services 1 and 2 of the AI-Assisted Banking Transaction Risk Platform  
**Time:** 90–120 minutes  
**Difficulty:** Beginner

**Objective:** Containerize Account and Transaction services (non-root images), run them with Docker Compose, **deploy them to the pre-provisioned OpenShift project**, walk a prepared pipeline (SBOM, signature, vulnerability gate), then roll forward a version and **roll it back on OpenShift**.

This is an **ops lab**. You do **not** copy the Lab 3 Java trees. Images are built from `labs/day-03/lab3/starter/`.

---

## What you will finish with

- Multi-stage Containerfiles that run as user `md287`, not root
- Account on **8081** and Transaction on **8082** from Docker images (Compose, then OpenShift)
- Liveness and readiness probes hitting Actuator
- ConfigMaps for URLs, Secrets for passwords and the classroom JWT **applied on OpenShift**
- Resource requests and limits on the OpenShift Deployments
- Working **Routes** on the classroom cluster
- Logs that still print `correlationId=` and never print `Authorization`
- A completed prepared pipeline: scan gate, SBOM, signature verify
- A version bump (`1.0.0` → `1.0.1`) and **`oc rollout undo`** back to `1.0.0`

Lab 5 will add Risk Assessment and OpenShift AI. Do not add a third Java service today.

---

## Knowledge you need (from Day 4)

| Day 4 idea | How it appears in this lab |
| --- | --- |
| **Liveness vs readiness vs startup** | HTTP GET `/actuator/health/liveness` and `/actuator/health/readiness`. Startup probe gives Spring time to boot. |
| **Correlation ID** | Header `X-Correlation-Id` still lands in logs as `correlationId=`. That is the classroom trace handle. |
| **Metrics** | Compose exposes `health,info,metrics`. Call `/actuator/metrics` **with a JWT**. Health stays public. |
| **Non-root image** | `USER md287` in the Containerfile. OpenShift also sets `runAsNonRoot: true`. |
| **Config vs secret** | JDBC URL in a ConfigMap. DB password and JWT secret in a Secret. |
| **Immutable tags** | Promote `1.0.0` / `1.0.1`, not `:latest`. |
| **Pipeline gates** | CRITICAL CVE → fail. SBOM is the ingredients list. Unsigned images must not run. |
| **Rollback** | Compose tag swap to practice, then **`oc rollout undo` on the assigned project** (required). |

### What you are not installing today

Postgres, Kafka, and the image registry are **platform** concerns. Compose starts Postgres and Kafka on the VM so you can prove probes before you push. On the classroom ARO cluster those backing services are **pre-provisioned** in **your assigned project** (service names `account-db`, `transaction-db`, `kafka` on **19092**).

**`oc login` is required.** The course outline deploys to pre-provisioned OpenShift namespaces. If `oc whoami` fails, stop and get the instructor — do not skip the cluster steps.

Jenkins is **awareness only** (Module 11). Do not install Jenkins. GitOps is conceptual.

---

## Environment basics

**Demonstration environment:** Windows 10/11 · PowerShell in VS Code (Ctrl+`)

| Task | How |
| --- | --- |
| Work folder | `labs/day-04/lab4/starter/` |
| HTTP | **`curl.exe`**, not `curl` |
| Tokens | `python labs\day-03\lab3\tools\issue-jwt.py teller` (and `ops`) |
| Stop older labs first | Lab 1–3 Compose stacks use the same host ports |

**You need:** Docker Desktop, Python 3 for JWTs, **OpenShift `oc`**, and classroom `oc login` to your assigned project. Java 21 only if you open the Lab 3 source.

| Cluster item | Value |
| --- | --- |
| API | `https://api.aro-md287.centralus.aroapp.io:6443/` |
| Your project | `md287-<your-username>` |
| Password | See [LAB-ACCESS.md](../../../LAB-ACCESS.md) |
| Image registry | `default-route-openshift-image-registry.apps.aro-md287.centralus.aroapp.io` |

**Port map (same as Labs 1–3):**

| Process | Host port |
| --- | --- |
| Account Service | 8081 |
| Transaction Service | 8082 |
| Account Postgres | 5433 |
| Transaction Postgres | 5434 |
| Kafka | 9092 |

If those ports are busy:

```powershell
docker ps
```

Stop the older stacks (`docker compose down` in each Lab 1–3 service folder) before you start Lab 4 Compose.

---

## Steps from the training slides

### Step 1 — Confirm the Lab 3 images you will ship

You package **working** Lab 3 services. You do not rebuild JWT or Kafka logic here.

**Do this:**

1. Open `labs/day-03/lab3/starter/account-service` and `.../transaction-service` in the editor if you want to skim `application.yml`. Confirm Actuator probes are already on:

```yaml
management.endpoint.health.probes.enabled: true
```

2. Confirm `.dockerignore` exists in each Lab 3 starter module (`target/`, tests, IDE files). That keeps image layers small.

**Expected result:** you know the build **context** is your Lab 3 starter, and the Containerfile lives in Lab 4.

**Why this matters:** Twelve-factor **build** is separate from **run**. The JAR you ship should be the one you already tested.

---

### Step 2 — Finish the Containerfiles (non-root)

Open:

- `starter/account-service/Containerfile`
- `starter/transaction-service/Containerfile`

Replace the TODOs with a multi-stage Alpine JRE image: group/user `md287`, `chown`, `USER`, `EXPOSE`.

Account exposes **8081**. Transaction exposes **8082**.

**Do this:**

```powershell
cd labs\day-04\lab4\starter

docker build `
  -f account-service\Containerfile `
  -t md287/account-service:1.0.0 `
  ..\..\..\day-03\lab3\starter\account-service
```

The first build downloads Maven plugins inside Docker. Give it several minutes.

Then Transaction:

```powershell
docker build `
  -f transaction-service\Containerfile `
  -t md287/transaction-service:1.0.0 `
  ..\..\..\day-03\lab3\starter\transaction-service
```

Confirm the image is not root:

```powershell
docker run --rm --entrypoint id md287/account-service:1.0.0
```

**Expected result:** `uid=100` (or similar) `md287`, **not** `uid=0(root)`. `docker images md287/account-service` shows tag `1.0.0`.

**Why this matters:** A container that runs as root is one break-out away from host power. OpenShift will often **refuse** a root image when `runAsNonRoot: true` is set.

If you get stuck, compare with a classmate’s Containerfile or raise a hand. Do not wait for a `solution/` folder — this pack does not include one.

---

### Step 3 — Exercise 4.2 recap (layered image)

**Do this:**

```powershell
docker history md287/account-service:1.0.0
```

**Expected result:** a Maven build stage is **not** in the final history (multi-stage left only the JRE + JAR). You should see `EXPOSE` and `USER`.

**Why this matters:** The compiler and Maven cache must not ship to production.

---

### Step 4 — Run the stack and prove probes, metrics, and correlation

From `labs/day-04/lab4/starter`:

```powershell
docker compose up -d --build
```

Wait until both apps listen (first boot runs Flyway). Then:

```powershell
curl.exe -s http://localhost:8081/actuator/health/liveness
curl.exe -s http://localhost:8081/actuator/health/readiness
curl.exe -s http://localhost:8082/actuator/health/readiness
curl.exe -s http://localhost:8081/actuator/info
```

Issue tokens and create a synthetic account + transaction (replace `ACC-...` after create):

```powershell
cd ..\..\..\day-03\lab3
$TELLER = python tools\issue-jwt.py teller
$OPS = python tools\issue-jwt.py ops

curl.exe -s -H "Authorization: Bearer $TELLER" http://localhost:8081/actuator/metrics

curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Authorization: Bearer $TELLER" `
  -H "Content-Type: application/json" `
  --data-binary "@starter\account-service\requests\create-valid.json" `
  http://localhost:8081/api/v1/accounts
```

Activate with `POST /api/v1/accounts/ACC-YOUR-ID/activate` and the teller token. Put that `accountId` into `labs/day-04/lab4/tools/requests/create-transaction.json`. Then:

```powershell
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Authorization: Bearer $OPS" `
  -H "X-Correlation-Id: lab4-demo" `
  -H "Content-Type: application/json" `
  --data-binary "@..\..\day-04\lab4\tools\requests\create-transaction.json" `
  http://localhost:8082/api/v1/transactions
```

Logs:

```powershell
docker logs md287-lab4-account --tail 40
docker logs md287-lab4-transaction --tail 40
```

**Expected result:**

- Health liveness/readiness **200** without a JWT
- `/actuator/info` shows `"version":"1.0.0"`
- `/actuator/metrics` with a teller token lists meter names (health stays public; metrics stay behind JWT — same Lab 3 security rules)
- Create account **201**, activate **200**, create transaction **201** `RECEIVED` (then GET becomes `SUBMITTED` if Kafka is healthy)
- Logs contain `correlationId=lab4-demo` (or the id Spring generated)
- Logs do **not** contain `Authorization` or the JWT payload
- If Account is down, POST transaction is still **503** — never a fake ACTIVE account (Lab 3 rule still holds)

**Why this matters:** Operators probe health without a token. Business APIs still require JWT. Correlation is how you follow one customer action across two containers.

---

### Step 5 — Log in, apply manifests, and push images (OpenShift — required)

The outline deploys to **pre-provisioned** projects. You do **not** create a namespace.

**Do this:**

1. In `starter/openshift/10-account.yaml` and `20-transaction.yaml`, replace the probe and resource TODOs. Use liveness `/actuator/health/liveness`, readiness `/actuator/health/readiness`, and small CPU/memory requests and limits.

2. Log in and select **your** project (API URL is the classroom cluster; username and password are in [LAB-ACCESS.md](../../../LAB-ACCESS.md)):

```powershell
oc login https://api.aro-md287.centralus.aroapp.io:6443/ --username <your-username> --password <password>
oc whoami
oc project md287-<your-username>
```

**Expected result:** `oc whoami` prints your participant account. `oc project` shows only `md287-<your-username>`. If login fails, **stop** — get the instructor. YAML review alone does not complete this lab.

3. Confirm backing services exist:

```powershell
oc get svc account-db transaction-db kafka
```

**Expected result:** all three Services are listed. Kafka is used at **`kafka:19092`** (see ConfigMap).

4. Apply ConfigMap, Secret, Deployments, Services, Routes. **Skip** `00-namespace.yaml`.

```powershell
cd labs\day-04\lab4\starter
$PROJECT = oc project -q
oc apply -n $PROJECT -f openshift\01-configmap.yaml
oc apply -n $PROJECT -f openshift\02-secret.yaml
oc apply -n $PROJECT -f openshift\10-account.yaml
oc apply -n $PROJECT -f openshift\20-transaction.yaml
oc -n $PROJECT get deploy,svc,route,cm,secret
```

5. Push the images you built in Step 2 into this project, then point the Deployments at the internal registry:

```powershell
cd labs\day-04\lab4
$env:MD287_REGISTRY = "default-route-openshift-image-registry.apps.aro-md287.centralus.aroapp.io"
powershell -File tools\push-images.ps1
```

Participants have **edit** on their project only, so they cannot always read the Route in `openshift-image-registry`. Setting `$env:MD287_REGISTRY` is the reliable path. The script also falls back to that hostname.

```powershell
oc -n $PROJECT set image deploy/account-service account-service=image-registry.openshift-image-registry.svc:5000/$PROJECT/account-service:1.0.0
oc -n $PROJECT set image deploy/transaction-service transaction-service=image-registry.openshift-image-registry.svc:5000/$PROJECT/transaction-service:1.0.0
oc -n $PROJECT rollout status deploy/account-service
oc -n $PROJECT rollout status deploy/transaction-service
oc -n $PROJECT get pods,route
```

6. Call the **Account Route** (not localhost):

```powershell
$HOST = oc -n $PROJECT get route account-service -o jsonpath="{.spec.host}"
curl.exe -s https://$HOST/actuator/health/readiness
```

Trust the classroom cluster certificate if `curl` warns; or use `curl.exe -k` only in this lab.

**Expected result:** Deployments list probes, requests/limits, `runAsNonRoot: true`. Secret holds `MD287_JWT_SECRET`. ConfigMap holds JDBC and Kafka URLs — not the password. Pods become Ready. Readiness on the Route returns **200**.

**Why this matters:** ConfigMaps are not for passwords. Probes stop sending traffic to a pod that is not ready. The outline requires a real deploy, not a YAML-only review.

---

### Step 6 — Prepared pipeline: SBOM, signature, scan gate

Fill **Exercise 4.3** first: `starter/pipeline/OWNERSHIP.md` (who owns scan / SBOM / sign / promote).

Walk the prepared pipeline (this is the required CI/CD evidence — SBOM, signature verify, vulnerability gate). Jenkins is awareness only; do not install it.

```powershell
cd labs\day-04\lab4
powershell -File tools\run-pipeline-locally.ps1
```

If the cluster has OpenShift Pipelines, also apply the classroom Pipeline (stub stages that match the same ownership map):

```powershell
$PROJECT = oc project -q
oc apply -n $PROJECT -f starter\pipeline\pipeline.yaml
oc create -n $PROJECT -f starter\pipeline\pipelinerun.yaml
oc get pipelinerun -n $PROJECT
```

If the Pipelines operator is missing, `oc apply` will error. The **local script still satisfies** the outline’s prepared pipeline (scan / SBOM / sign). Record that in Exercise 4.3.

Open `tools/sample-sbom-account-service.json`. Confirm it lists `spring-boot-starter-web` **3.4.5** and Temurin 21.

**Expected result:** scan **PASS** on `sample-scan-pass.json`, scan **FAIL** on `sample-scan-fail.json` (CRITICAL). Signature script prints the Cosign verify command. SBOM file exists under `tools/`.

**Why this matters:** A green deploy with a CRITICAL CVE is not a success. An SBOM is how you answer “what did we actually ship?” after a new CVE drops on Friday.

---

### Step 7 — Deploy a new version and roll it back (OpenShift — required)

You will change **only the visible version** (`INFO_APP_VERSION` / image tag). You are practicing the **mechanic**, not rewriting Java.

Practice the tag swap on Compose first (optional warm-up), then **rollback on OpenShift**.

**OpenShift (required):**

```powershell
$PROJECT = oc project -q
oc -n $PROJECT set env deploy/account-service INFO_APP_VERSION=1.0.1
oc -n $PROJECT rollout status deploy/account-service
$HOST = oc -n $PROJECT get route account-service -o jsonpath="{.spec.host}"
curl.exe -s https://$HOST/actuator/info
oc -n $PROJECT rollout undo deploy/account-service
oc -n $PROJECT rollout status deploy/account-service
oc -n $PROJECT rollout history deploy/account-service
curl.exe -s https://$HOST/actuator/info
```

**Expected:** undo restores the previous ReplicaSet. `oc rollout history` shows more than one revision. `/actuator/info` returns to `"version":"1.0.0"`.

**Why this matters:** Banks need a rehearsed rollback. “Redeploy yesterday’s tag” is faster than debugging a bad Friday release in production.

---

## Success criteria

- [ ] Account and Transaction images build from Lab 3 starter context
- [ ] Container process is **not** root (`docker run --rm --entrypoint id ...`)
- [ ] Compose stack: liveness, readiness, `/actuator/metrics`, `/actuator/info`
- [ ] JWT still required on business APIs; health stays public
- [ ] Correlation id appears in logs; no `Authorization` text
- [ ] `oc whoami` works; manifests applied in the **assigned** project (not `00-namespace.yaml`)
- [ ] Images pushed; pods Ready; Account **Route** readiness **200**
- [ ] Pipeline: CRITICAL scan fails the gate; SBOM reviewed; signature command reviewed
- [ ] `oc rollout undo` restored `1.0.0` (history shows more than one revision)
- [ ] Ownership map for Exercise 4.3 filled

---

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| Port already allocated | `docker compose down` in Lab 1–3 folders; `docker ps` |
| Image build COPY fails | Build **context** must be the Lab 3 starter module, `-f` is the Lab 4 Containerfile |
| `id` still shows root | `USER md287` missing; rebuild without cache `docker build --no-cache ...` |
| Readiness never 200 | Postgres/Kafka not healthy; `docker compose ps` and `docker logs md287-lab4-account` |
| Transaction stays RECEIVED | Kafka topics: `kafka-init` must complete; wait and GET again |
| 401 with a token | Same classroom secret as Lab 3 (`md287-lab-only-hmac-secret-32bytes!`, at least 32 bytes for HS256); re-run `issue-jwt.py` |
| 503 on POST transaction | Account container not ready — Lab 3 safe fallback, **do not** fake ACTIVE |
| `oc whoami` failed | Required. Get login from [LAB-ACCESS.md](../../../LAB-ACCESS.md). Do not skip OpenShift. |
| `oc apply` Unauthorized | Wrong project or missing `edit`. Stay in the assigned project. |
| ImagePullBackOff | Run `tools\push-images.ps1`, then `oc set image` to the internal pullspec |
| Pod `CreateContainerConfigError` / `runAsNonRoot` + `non-numeric user (md287)` | OpenShift cannot prove a named `USER md287` is non-root. Keep `runAsNonRoot: true` and set `runAsUser: 100` (the uid `docker run --entrypoint id` printed). |
| Registry Route missing | `$env:MD287_REGISTRY = "default-route-openshift-image-registry.apps.aro-md287.centralus.aroapp.io"` then re-run `push-images.ps1` |
| Pipelines CRDs missing | Use `tools/run-pipeline-locally.ps1` (that is the prepared pipeline) |

```powershell
cd labs\day-04\lab4\starter
docker compose down
```

---

## Optional stretch

- Run `docker history` on Transaction Service and compare layer count with Account Service.
- Sketch GitOps (Argo CD watches Git; you do not install it here).
- Jenkins is an **awareness** alternative to Tekton — same stages, different YAML.

Do **not** add OpenShift AI or Risk Assessment here.

---

## What you built in the capstone

```text
Day 1  Account Service
Day 2  + Transaction Service
Day 3  + JWT, Resilience4j, tests
Day 4  + containers, OpenShift, CI/CD  ← you are here
Day 5  + Risk Assessment Service and OpenShift AI
```
