# Lab 5 — Risk Assessment Service

**Day:** 5 — OpenShift AI, MCP, and Capstone Completion  
**Capstone:** Service 3 of 3 in the AI-Assisted Banking Transaction Risk Platform  
**Time:** 90–120 minutes  
**Difficulty:** Beginner

**Objective:** Build **Risk Assessment Service**. Consume `TransactionSubmitted`, call a model endpoint (auth, timeout, safe fallback), apply a **deterministic policy**, store an audit row in this service’s own database, and complete a **human review**. MCP stays a design discussion — you do not run an MCP server today.

This lab can run **standalone** (publish sample Kafka messages). An optional extra uses Lab 3 Account + Transaction so the event comes from a real POST.

---

## What you will finish with

- Risk Assessment Service on port **8083**
- Its **own** database (`risk_db` on host port **5435**)
- A consumer on Kafka topic `transactions.submitted` (group `risk-assessment-service`)
- Model call to a classroom stand-in for OpenShift AI (`http://localhost:8090/v1/score`)
- Policy outcomes **APPROVE**, **HOLD**, **DECLINE**
- Model timeout or HTTP 500 → **HOLD** `MODEL_UNAVAILABLE` (never auto-approve)
- Human review on HOLD only (`risk.write`)
- Audit fields: model name/version, score, policy version, correlation id
- Tests for policy, fallback, and JWT

---

## Knowledge you need (from Day 5)

| Day 5 idea | How it appears in this lab |
| --- | --- |
| **Model endpoint** | `ModelClient` POST with Bearer API key, 2s connect / 3s read timeout |
| **Safe fallback** | Timeout or 5xx → HOLD, not APPROVE |
| **Deterministic policy** | Java `PolicyEngine` owns APPROVE / HOLD / DECLINE |
| **Human review** | HOLD rows stay `PENDING` until a reviewer posts APPROVE or DECLINE |
| **Audit** | Persist `modelName`, `modelVersion`, `modelScore`, `policyVersion`, `correlationId` |
| **MCP** | Exercise 5.3 worksheet only — auth, HITL, audit if a tool wrapped this API |

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

**You need:** Java 21, Maven 3.9+, Docker Desktop, Python 3.

| Process | Host port |
| --- | --- |
| Risk Assessment Service | 8083 |
| Risk Postgres | 5435 |
| Kafka | 9092 |
| Mock model (OpenShift AI stand-in) | 8090 |

---

## Steps from the training slides

### Step 1 — Start backing services and the mock model

From `labs/day-05/lab5/starter`:

```powershell
docker compose up -d
curl.exe -s http://localhost:8090/v1/health
```

**Expected result:** Compose starts `risk-db`, `kafka`, `kafka-init`, `mock-model`. Health JSON includes `"status":"UP"`.

**Why this matters:** The mock model is the classroom stand-in for a **pre-deployed OpenShift AI** route. You still treat it as an untrusted remote: timeout, auth header, no score in logs beyond the integer.

If port 9092 is busy, `docker compose down` in Lab 2–4 folders first. Ports **5435** and **8090** are Lab 5 only.

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

```powershell
mvn -Dtest=PolicyEngineTest test
```

**Expected result:** APPROVE, HIGH_SCORE, HIGH_VALUE, MODEL_UNAVAILABLE, and REVIEW_BAND cases pass.

**Why this matters:** The model may suggest a number. **Policy** decides money movement. A high-value payment with a low score still goes to a human.

---

### Step 4 — Safe model fallback

`ModelClient` already sets connect/read timeouts. Confirm the `catch` blocks return `ModelScore.timeout()` / `unavailable()` and **never** invent a score.

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

### Step 7 — Optional: live event from Transaction Service

Lab 5 Compose starts its **own** Kafka on **9092**. That collides with Lab 3. For this optional path:

1. Stop Lab 5 Kafka: from `starter`, `docker compose down`
2. Keep Lab 3 running (Account **8081**, Transaction **8082**, Kafka **9092**)
3. Start only Lab 5 extras: `docker compose up -d risk-db mock-model`
4. POST a transaction with a Lab 3 **ops** token (`accounts.read transactions.read transactions.write`)
5. GET the assessment with a **Lab 5** ops token (`tools\issue-jwt.py ops`). Lab 3 ops does **not** include `risk.read`

Risk Assessment uses consumer group `risk-assessment-service`, so it receives a **copy** of `TransactionSubmitted`. GET `/api/v1/assessments/{transactionId}`.

If you cannot share Kafka, stay on the sample JSON files. Do not invent a successful assessment.

---

### Step 8 — OpenShift YAML and MCP (Exercise 5.3)

Read `openshift/risk-assessment.yaml` in this starter folder. Tick:

- [ ] ConfigMap holds model URL and topic — not the API key
- [ ] Secret holds DB password, JWT secret, model API key
- [ ] Probes hit Actuator; `runAsNonRoot: true`

If `oc whoami` works, apply into the assigned project. If not, YAML review counts.

Fill `mcp-controls.md` in this starter folder (Exercise 5.3): if an MCP tool called `get_assessment` / `submit_review`, list auth, HITL, and audit controls.

---

## Success criteria

- [ ] `mvn test` passes
- [ ] Sample events produce APPROVE / HOLD / DECLINE / MODEL_UNAVAILABLE / HIGH_VALUE
- [ ] Duplicate event does not create a second row
- [ ] Reviewer can complete HOLD; ops cannot
- [ ] Logs stay synthetic and secret-free
- [ ] OpenShift YAML reviewed (or applied)
- [ ] MCP worksheet filled

---

## Capstone demo script (teams)

1. Health on 8083  
2. APPROVE path (`approve.json`)  
3. Duplicate event  
4. Timeout → HOLD (never APPROVE)  
5. Human review  
6. Point at OpenShift YAML (probes, secret vs config)  
7. Say how you would roll back the image (Lab 4 `rollout undo`)

---

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| Mock model connection refused | `docker compose ps`; `curl.exe http://localhost:8090/v1/health` |
| Publish-event hangs | Kafka not healthy; wait for `kafka-init` to exit 0 |
| 401 with a token | Lab 5 `issue-jwt.py` secret must match `application.yml` |
| 403 on GET | Use `ops` or `reviewer`, not `teller` |
| Always HOLD | Policy TODO not implemented; or model not `OK` |
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
