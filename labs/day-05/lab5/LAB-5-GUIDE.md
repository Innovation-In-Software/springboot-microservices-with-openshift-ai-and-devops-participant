# Lab 5 — Risk Assessment Service

**Day:** 5 — OpenShift AI, MCP, and Capstone Completion  
**Capstone:** Service 3 of 3 in the AI-Assisted Banking Transaction Risk Platform  
**Time:** 90–120 minutes  
**Difficulty:** Beginner

**Objective:** Build **Risk Assessment Service**. Consume `TransactionSubmitted`, **call the pre-deployed OpenShift AI model endpoint** (auth, timeout, safe fallback), apply a **deterministic policy**, store an audit row in this service’s own database, complete a **human review**, **deploy to the assigned OpenShift project**, and capture capstone OpenShift evidence. MCP stays a design discussion — you do not run an MCP server today.

Code and tests run on the VM. Sample `TransactionSubmitted` JSON is the required event vocabulary. A live POST from Transaction Service is extra if Kafka can be shared.

---

## What you will finish with

- Risk Assessment Service on port **8083**
- Its **own** database (`risk_db` on host port **5435**)
- A consumer on Kafka topic `transactions.submitted` (group `risk-assessment-service`)
- Model call to the **pre-deployed OpenShift AI** endpoint (same contract as the local stand-in on `:8090`)
- Risk Assessment **deployed** to the assigned OpenShift project (Route evidence)
- Policy outcomes **APPROVE**, **HOLD**, **DECLINE**
- Model timeout or HTTP 500 → **HOLD** `MODEL_UNAVAILABLE` (never auto-approve)
- Human review on HOLD only (`risk.write`)
- Audit fields: model name/version, score, policy version, correlation id
- Tests for policy, fallback, and JWT

---

## Knowledge you need (from Day 5)

| Day 5 idea | How it appears in this lab |
| --- | --- |
| **Model endpoint** | `ModelClient` POST with Bearer API key. Local stand-in `:8090` for coding; **required** call to the pre-deployed OpenShift AI Route (`MD287_MODEL_ROUTE`). |
| **Safe fallback** | Timeout or 5xx → HOLD, not APPROVE |
| **Deterministic policy** | Java `PolicyEngine` owns APPROVE / HOLD / DECLINE |
| **Human review** | HOLD rows stay `PENDING` until a reviewer posts APPROVE or DECLINE |
| **Audit** | Persist `modelName`, `modelVersion`, `modelScore`, `policyVersion`, `correlationId` |
| **MCP** | Walk Exercise 5.3 worksheet (already filled) — auth, HITL, audit. No MCP server. |
| **GitHub Copilot Free** | Already on this VM. Copilot may draft `PolicyEngine` if/else — **review before accept**; tests must prove the table. |
| **runAsUser 100** | Named `USER md287` is not enough on ARO. Keep `runAsNonRoot` and set uid **100**. |

### Policy table (`policy-v1`)

| Condition (first match wins) | Disposition | Reason |
| --- | --- | --- |
| Model not `OK` | HOLD | `MODEL_UNAVAILABLE` |
| Amount ≥ 5000 | HOLD | `HIGH_VALUE` |
| Score ≥ 70 | DECLINE | `HIGH_SCORE` |
| Score < 40 **and** amount < 1000 | APPROVE | `LOW_SCORE_LOW_VALUE` |
| Anything else | HOLD | `REVIEW_BAND` |

### Who can call what

| Token (`tools/issue-jwt.py`) | GET assessments | POST review |
| --- | --- | --- |
| `ops` — includes `risk.read` | 200 | **403** |
| `reviewer` — `risk.read risk.write` | 200 | 200 on HOLD |
| `teller` | **403** | **403** |
| no token | **401** | **401** |

Health stays public.

---

## Environment basics

**Demonstration environment:** Windows 10/11 · PowerShell in VS Code (Ctrl+`)

| Task | How |
| --- | --- |
| Work folder | `labs/day-05/lab5/starter/risk-assessment-service` |
| HTTP | **`curl.exe`**, not `curl` |
| Tokens | `python labs\day-05\lab5\tools\issue-jwt.py ops` (and `reviewer`) |
| Stop older labs first | Labs 1–4 may still bind **9092** |
| GitHub Copilot | **Copilot Free** is already on this VM. Use it to draft `PolicyEngine`; tests must prove the table. **Review before accept**. |

**You need:**

- Java 21, Maven 3.9+, Docker Desktop, Python 3, **`oc` login** to `md287-<your-username>`
- Open the **Day 5** starter (`labs/day-05/lab5/starter/risk-assessment-service`), not yesterday's trees
- HTTP calls: **`curl.exe`** (not `curl` — PowerShell aliases `curl`)
- Tokens from `labs/day-05/lab5/tools/issue-jwt.py` (`ops` / `reviewer`). Lab 3 ops has **no** `risk.read`
- Classroom **OpenShift AI model Route** must be UP (`MD287_MODEL_ROUTE`); local mock is `:8090`

| Process | Host port |
| --- | --- |
| Risk Assessment Service | 8083 |
| Risk Postgres | 5435 |
| Kafka | 9092 |
| Mock model (OpenShift AI stand-in) | 8090 |

---

## Steps from the training slides

### Step 1 — Start local backing services and prove the pre-deployed model

The outline calls a **pre-deployed OpenShift AI** model (application integration: auth, timeout, fallback). IIS places that endpoint in your project as Service `md287-risk-model` (port **8090**) plus a Route. Workbenches / KServe internals stay lecture-only.

**Do this:**

1. Confirm the classroom model Route (HTTP, **no trailing slash**). After `oc project md287-<your-username>`:

```powershell
oc whoami
oc project md287-<your-username>
$env:MD287_MODEL_ROUTE = "http://md287-risk-model-$(oc project -q).apps.aro-md287.centralus.aroapp.io"
curl.exe -s "$env:MD287_MODEL_ROUTE/v1/health"
```

**Expected result:** JSON includes `"status":"UP"` and a model name/version. If this fails, **stop** — the model must be pre-deployed. Do not invent scores.

2. For coding on the VM, start Compose (local Kafka, `risk_db`, and a **same-contract** mock on **8090**):

```powershell
cd labs\day-05\lab5\starter
docker compose up -d
curl.exe -s http://localhost:8090/v1/health
```

If port 9092 is busy, `docker compose down` in Lab 2–4 folders first. Ports **5435** and **8090** are Lab 5 on the VM.

**Why this matters:** You treat the model as an untrusted remote: timeout, `Authorization: Bearer` API key, no raw key in logs. The local mock matches the OpenShift AI Route so PolicyEngine tests can run before you deploy.

---

### Step 2 — Confirm the starter still refuses to assess

```powershell
cd risk-assessment-service
mvn test
```

Policy tests fail until Step 3. That is expected.

---

### Step 3 — Implement `PolicyEngine` (Exercise 5.2)

Open `policy/PolicyEngine.java`. Replace the TODO with the table above. Use the injected `Md287Properties.Policy` thresholds — do not hard-code magic numbers.

GitHub Copilot (Free, already on this VM) may draft the if/else. **Review before accept.** Reject any suggestion that auto-APPROVEs when the model is down. Tests must prove every row of the table.

```powershell
mvn -Dtest=PolicyEngineTest test
```

**Expected result:** APPROVE, HIGH_SCORE, HIGH_VALUE, MODEL_UNAVAILABLE, and REVIEW_BAND cases pass.

**Why this matters:** The model may suggest a number. **Policy** decides money movement. A high-value payment with a low score still goes to a human.

---

### Step 4 — Safe model fallback

`ModelClient` already sets connect/read timeouts. Confirm the `catch` blocks return `ModelScore.timeout()` / `unavailable()` and **never** invent a score. If Copilot suggests `return ModelScore.ok(...)` in a catch block, reject it.

`AssessmentService` must call `policyEngine.decide` with that result so a timeout becomes HOLD.

```powershell
mvn -Dtest=AssessmentServiceTest test
```

**Expected result:** `modelTimeoutStoresHold` passes. Duplicate `eventId` does not call the model again.

**Why this matters:** Same rule as Lab 3: failure is not “assume it is fine.”

---

### Step 5 — Protect the API (scopes)

In `SecurityConfig`, GET `/api/v1/assessments/{transactionId}` needs `SCOPE_risk.read`. POST `/*/review` needs `SCOPE_risk.write`. Actuator health/info stay public.

```powershell
mvn -Dtest=AssessmentSecurityTest test
mvn test
```

**Expected result:** 401 without a token, 403 with `risk.read` on review, all tests green.

---

### Step 6 — Run the service and publish sample events

```powershell
mvn spring-boot:run
```

New terminal, from `labs/day-05/lab5`:

```powershell
$OPS = python tools\issue-jwt.py ops
$REVIEWER = python tools\issue-jwt.py reviewer

.\tools\publish-event.ps1 -File tools\events\approve.json
.\tools\publish-event.ps1 -File tools\events\hold.json
.\tools\publish-event.ps1 -File tools\events\decline.json
.\tools\publish-event.ps1 -File tools\events\timeout.json
.\tools\publish-event.ps1 -File tools\events\high-value.json

curl.exe -s -H "Authorization: Bearer $OPS" http://localhost:8083/api/v1/assessments/TXN-A1B2C3D4
curl.exe -s -H "Authorization: Bearer $OPS" "http://localhost:8083/api/v1/assessments?disposition=HOLD"
```

**Expected result:**

| File | Disposition | Reason |
| --- | --- | --- |
| approve.json | APPROVE | `LOW_SCORE_LOW_VALUE` |
| hold.json | HOLD | `REVIEW_BAND` |
| decline.json | DECLINE | `HIGH_SCORE` |
| timeout.json | HOLD | `MODEL_UNAVAILABLE` |
| high-value.json | HOLD | `HIGH_VALUE` |

Publish `approve.json` a second time. Logs show duplicate ignored. One row in the database.

Review the HOLD from `hold.json`:

```powershell
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Authorization: Bearer $REVIEWER" `
  -H "Content-Type: application/json" `
  --data-binary "{`"decision`":`"APPROVE`",`"reason`":`"false positive on synthetic lab traffic`"}" `
  http://localhost:8083/api/v1/assessments/TXN-B2C3D4E5/review
```

**Expected result:** **200**, `reviewStatus=COMPLETED`, `disposition=APPROVE`. Same POST with `$OPS` is **403**. Reviewing an APPROVE row is **409** `REVIEW_NOT_ALLOWED`.

Logs: `transactionId=`, `correlationId=lab5-demo-...`. No `Authorization`, no API key.

---

### Step 7 — Integrate with Account + Transaction events

Sample JSON files use the **`TransactionSubmitted`** vocabulary (event id, correlation id, amount). That is required.

To take a live event from Transaction Service (same topic):

1. Stop Lab 5 Kafka: from `starter`, `docker compose down`
2. Keep Lab 3 or Lab 4 Account **8081**, Transaction **8082**, Kafka **9092**
3. Start only Lab 5 extras: `docker compose up -d risk-db mock-model`
4. POST a transaction with a Lab 3 **ops** token (`accounts.read transactions.read transactions.write`)
5. GET the assessment with a **Lab 5** ops token (`tools\issue-jwt.py ops`). Lab 3 ops does **not** include `risk.read`

Risk Assessment uses consumer group `risk-assessment-service`, so it receives a **copy** of `TransactionSubmitted`. GET `/api/v1/assessments/{transactionId}`.

If you cannot share Kafka, the sample JSON files still satisfy the event vocabulary. Do not invent a successful assessment.

---

### Step 8 — Deploy Risk Assessment to OpenShift; MCP is the worksheet (Exercise 5.3)

**Do this:**

1. Read `openshift/risk-assessment.yaml`. Tick:

- [ ] ConfigMap holds model URL and topic — not the API key
- [ ] Secret holds DB password, JWT secret, model API key
- [ ] Probes hit Actuator; `runAsNonRoot: true`
- [ ] `MD287_MODEL_BASE_URL` is `http://md287-risk-model:8090` (in-cluster OpenShift AI stand-in)

2. Build, push, apply (required — capstone needs OpenShift evidence):

```powershell
cd labs\day-05\lab5
docker build -f starter\risk-assessment-service\Containerfile -t md287/risk-assessment-service:1.0.0 starter\risk-assessment-service
$PROJECT = oc project -q
oc apply -n $PROJECT -f starter\openshift\risk-assessment.yaml
$env:MD287_REGISTRY = "default-route-openshift-image-registry.apps.aro-md287.centralus.aroapp.io"
powershell -File tools\push-risk-image.ps1
oc -n $PROJECT set image deploy/risk-assessment-service risk-assessment-service=image-registry.openshift-image-registry.svc:5000/$PROJECT/risk-assessment-service:1.0.0
oc -n $PROJECT rollout status deploy/risk-assessment-service
$RISK = oc -n $PROJECT get route risk-assessment-service -o jsonpath="{.spec.host}"
curl.exe -s https://$RISK/actuator/health/readiness
```

**Expected result:** Pod Ready. Route readiness **200**. This is the OpenShift evidence for the capstone demo.

3. Fill `mcp-controls.md` in this starter folder (Exercise 5.3): if an MCP tool called `get_assessment` / `submit_review`, list auth, HITL, and audit controls. Compare with `../solution/mcp-controls.md` when the instructor says to. You do **not** run an MCP server.

---

## Success criteria

- [ ] `mvn test` passes
- [ ] Sample events produce APPROVE / HOLD / DECLINE / MODEL_UNAVAILABLE / HIGH_VALUE
- [ ] Duplicate event does not create a second row
- [ ] Reviewer can complete HOLD; ops cannot
- [ ] Logs stay synthetic and secret-free
- [ ] Pre-deployed OpenShift AI Route `/v1/health` is **UP** (`MD287_MODEL_ROUTE`)
- [ ] Risk Assessment deployed; Route readiness **200**
- [ ] MCP worksheet filled

---

## Capstone demo script (teams)

1. Health on 8083  
2. APPROVE path (`approve.json`)  
3. Duplicate event  
4. Timeout → HOLD (never APPROVE)  
5. Human review  
6. OpenShift evidence: Risk Route readiness **200** (and Lab 4 Account Route / `rollout undo`)  
7. Rollback/recovery: Lab 4 `oc rollout undo`

---

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| Mock model connection refused | `docker compose ps`; `curl.exe http://localhost:8090/v1/health` |
| Pre-deployed model down | `curl.exe $env:MD287_MODEL_ROUTE/v1/health`; instructor must pre-deploy `md287-risk-model` |
| ImagePullBackOff on Risk | `tools\push-risk-image.ps1` then `oc set image` |
| Pod `CreateContainerConfigError` / `runAsNonRoot` + `non-numeric user (md287)` | OpenShift cannot prove a named `USER md287` is non-root. Keep `runAsNonRoot: true` and set `runAsUser: 100` (the uid `docker run --entrypoint id` printed). |
| `oc whoami` failed | Required. Get login from [LAB-ACCESS.md](../../../LAB-ACCESS.md). |
| Publish-event hangs | Kafka not healthy; wait for `kafka-init` to exit 0 |
| Transaction stays RECEIVED; `oc exec` shows no consumer groups | Single-broker Kafka needs `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1` (and transaction log RF=1). Without `__consumer_offsets`, producers succeed but consumers never join. |
| 401 with a token | Lab 5 `issue-jwt.py` secret must match `application.yml` |
| 403 on GET | Use `ops` or `reviewer`, not `teller` |
| Always HOLD | Policy TODO not implemented; or model not `OK` |
| All sample events APPROVE with `modelScore` 0 | Java `SimpleClientHttpRequestFactory` POSTs `Transfer-Encoding: chunked`. The classroom Python mock only read `Content-Length`, so the body was `{}` and the score was 0. Use `JdkClientHttpRequestFactory` (Content-Length) and a mock that also reads chunked bodies. |
| Second approve.json creates another row | Idempotency on `eventId` missing |

```powershell
Ctrl+C
cd labs\day-05\lab5\starter
docker compose down
```

---

## What you built in the capstone

```text
Day 1  Account Service
Day 2  + Transaction Service
Day 3  + JWT, Resilience4j, tests
Day 4  + containers, OpenShift, CI/CD
Day 5  + Risk Assessment and OpenShift AI  ← you are here
```
