# MD287 Complete Course Outline

**How the course is designed and delivered**

**Course:** Spring Boot Microservices with OpenShift AI and DevOps  
**Course code:** MD287  
**Client:** Bank of America  
**Duration:** 5 instructor-led days  
**Prepared by:** Innovation In Software Corporation  
**Version:** 1.0  
**Date:** 8 September 2026  
**Audience for this document:** Instructors, IIS delivery, TEKsystems coordinators  

This is a **new** outline of the course **as built**. It describes the 13-module / 5-lab progressive design in the repository. It does not replace `OUTLINE.md`.

**Companion:** [`MD287_TEKsystems_Outline_to_Course_Mapping.md`](MD287_TEKsystems_Outline_to_Course_Mapping.md) maps the original TEKsystems topic list onto this design.

---

## 1. Design intent

The course is **not** 13 disconnected modules with 13 labs. It is one banking platform assembled in five layers.

| Design choice | What it means in delivery |
| ------------- | ------------------------- |
| **One capstone, five days** | Account → Transaction → harden → deploy → Risk + OpenShift AI |
| **13 modules, 5 labs** | Lecture and Kahoot stay per module; hands-on is **one lab per day** |
| **Starter folders** | Participants work in each lab `starter/` (this participant repo) |
| **Application developer, not platform admin** | OpenShift AI is a **pre-deployed model HTTP endpoint**; MCP is a worksheet |
| **Governed AI** | Model returns a **score**; Java `PolicyEngine` decides APPROVE / HOLD / DECLINE; human review on HOLD |
| **Classroom stack vs slide stack** | Labs freeze Java **21**, Spring Boot **3.4.5**, Docker Desktop, HMAC JWT. Keycloak, Redis, Jenkins, Copilot seats, MCP runtime, and KServe workbenches stay lecture or demo |

**Capstone name:** AI-Assisted Banking Transaction Risk Platform (three services, each with its own database).

---

## 2. Course identity

| Field | Value |
| ----- | ----- |
| Format | Instructor-led · slides · progressive labs · capstone demonstration |
| Intended audience | Java / Spring Boot developers, backend and microservices engineers, DevOps / platform engineers, technical leads, architects supporting enterprise banking |
| Class size | 12–20 participants (typical) |
| Public participant repo | `https://github.com/Innovation-In-Software/springboot-microservices-with-openshift-ai-and-devops-participant` (`main`) |
| Clone | Participants clone themselves. No GitHub login. Do not pre-copy labs onto the VM. |
| Workstations | TEKsystems Ablaze Desktop VMs (all five days) |
| Cluster | IIS Azure Red Hat OpenShift (ARO), East US 2 — **required Days 4–5** |

### 2.1 Prerequisites

Working Java 17+ (classroom uses **21**), OOP, basic Spring Boot, Maven, REST/HTTP/JSON, basic SQL, Git, Linux/PowerShell comfort, testing awareness. Containers and Kafka are taught; they are not assumed as depth.

### 2.2 Learning outcomes (as designed)

By the end of the week participants can:

- Decompose banking capabilities into bounded microservices and REST APIs
- Build Spring Boot services with validation, exception handling, configuration, and OpenAPI
- Apply database-per-service, Flyway, and event-driven flows with Kafka (Red Hat AMQ Streams in lecture)
- Secure APIs with JWT (OAuth2/OIDC concepts; classroom HMAC tokens)
- Add Resilience4j timeouts and circuit breakers with **safe fallbacks** (never auto-approve money movement)
- Containerize services and **deploy them to OpenShift**, including rollback
- Walk a prepared pipeline (SBOM, scan gate, signature verify)
- Integrate a Spring Boot service with a model endpoint (auth, timeout, fallback, audit)
- Apply deterministic policy + human-review routing
- Explain MCP / agentic AI controls (authorization, HITL, audit) without building an MCP server
- Demonstrate the week’s platform end to end

---

## 3. How the week is structured

```text
Day 1  Account Service
         ↓
Day 2  + Transaction Service  (REST validate + Kafka TransactionSubmitted)
         ↓
Day 3  + JWT, Resilience4j, tests, log hygiene
         ↓
Day 4  + Observability, containers, OpenShift deploy, pipeline evidence, rollback
         ↓
Day 5  + Risk Assessment ↔ model endpoint + policy / human review + capstone demo
```

| Day | Theme | Modules | Lab (90–120 min) | Environment |
| --- | ----- | ------- | ---------------- | ----------- |
| 1 | Architecture, API Design, and Spring Boot | 1–3 | Lab 1 — Account Service | VM only |
| 2 | Data and Event-Driven Communication | 4–5 | Lab 2 — Transaction Service | VM only |
| 3 | Resilience, Security, and Testing | 6–8 | Lab 3 — Secure and Resilient Services | VM only |
| 4 | Observability, Containers, OpenShift, and CI/CD | 9–11 | Lab 4 — Deploy the Capstone Services | VM **+ ARO** |
| 5 | OpenShift AI, MCP, and Capstone Completion | 12–13 | Lab 5 — Risk Assessment Service | VM **+ ARO** |

Each day: module slides → checkpoint exercises → Kahoot (per module) → the day’s lab. Daily knowledge check (5–10 questions) on Days 1–4. Day 5: short check + capstone demonstration.

---

## 4. Module catalog (13)

Consolidated from a larger topic bank (728 catalog topics retained for deck expansion). Delivery teaches the 13 modules below.

| Module | Title | Day | Hands-on checkpoint |
| ------ | ----- | --- | ------------------- |
| 1 | Microservices Fundamentals & Cloud-Native Principles | 1 | Exercise 1.1 Analyze a monolithic banking application |
| 2 | Microservices Design & REST APIs | 1 | Exercise 1.2 Design Account Service APIs and boundaries |
| 3 | Spring Boot Microservices Development | 1 | Exercise 1.3 Scaffold Account endpoints and validation |
| 4 | Microservices Data & Persistence | 2 | Exercise 2.1 Map data ownership Account vs Transaction |
| 5 | Integration & Event-Driven Messaging | 2 | Exercises 2.2–2.3 Event flow; idempotency and DLQ |
| 6 | Resilience & Performance | 3 | Exercise 3.1 Timeout and circuit breaker |
| 7 | Microservices Security | 3 | Exercise 3.2 JWT roles/scopes |
| 8 | Testing Spring Boot Microservices | 3 | Exercise 3.3 Security and resilience tests |
| 9 | Observability | 4 | Exercise 4.1 Health probes and correlation logging |
| 10 | Containers & Red Hat OpenShift | 4 | Exercise 4.2 Containerize Account Service |
| 11 | DevOps & CI/CD | 4 | Exercise 4.3 Pipeline stage ownership map |
| 12 | Copilot, OpenShift AI & MCP | 5 | Exercises 5.1–5.3 Model→policy; policy rules; MCP controls |
| 13 | Capstone Integration & Course Close | 5 | Capstone demonstration scenarios |

Kahoot: **15 questions per module** (Modules 1–13).

---

## 5. Day 1 — Architecture, API Design, and Spring Boot

**Outcome:** A functional, documented Spring Boot **Account Service** (capstone service 1 of 3).

### 5.1 Lecture focus

- Microservices vs monolith, trade-offs, Twelve-Factor and cloud-native
- Bounded contexts, REST resources, HTTP semantics, idempotency, OpenAPI
- Spring Boot lifecycle, starters, DI, layers, profiles, validation, errors, Actuator
- GitHub Copilot **demonstration** (review-before-accept); not a required lab seat

### 5.2 Lab 1 — Account Service

Path: `labs/day-01/lab1/` · Port **8081** · Postgres host **5433** (`account_db`)

Participants:

- Run the starter with Flyway + PostgreSQL
- Implement create, retrieve, update, and status transitions **activate / freeze / close** (no physical delete)
- Add validation, exception JSON, OpenAPI, Actuator health
- Pass unit tests; logs use synthetic identifiers only

---

## 6. Day 2 — Data and Event-Driven Communication

**Outcome:** Two-service workflow — Account + **Transaction Service** — REST plus `TransactionSubmitted` events.

### 6.1 Lecture focus

- Database-per-service, Spring Data JPA, Flyway, local transactions
- Why to avoid distributed transactions; Saga and transactional outbox (**conceptual**)
- Sync REST vs async messaging; Kafka producers/consumers, correlation, idempotency, DLQ
- Event vocabulary: `TransactionSubmitted`, `TransactionApproved`, `TransactionHeldForReview`, `TransactionDeclined` (`RISK_ASSESSED` is internal)

### 6.2 Lab 2 — Transaction Service

Path: `labs/day-02/lab2/` · Port **8082** · Postgres **5434** · Kafka **9092**

Participants:

- Own `transaction_db` (no shared tables with Account)
- GET Account Service; only **ACTIVE** accounts may transact
- Publish/consume `TransactionSubmitted`; correlation ID; `processed_events` idempotency
- Confirm poison messages on `transactions.submitted.DLT`

---

## 7. Day 3 — Resilience, Security, and Testing

**Outcome:** Secured, resilient, tested Account and Transaction APIs; safe fallbacks; log hygiene.

### 7.1 Lecture focus

- Failure modes; timeouts, retries, circuit breaker; bulkhead and rate limiting (**conceptual**)
- OAuth2 / OIDC, JWT, roles and scopes; Spring Security resource server
- Keycloak / client IdP **overview** (labs use `issue-jwt.py`)
- Data classification, minimization, log redaction
- Unit tests + **one** guided integration test (`AccountPersistenceTest`)

### 7.2 Lab 3 — Secure and Resilient Services

Path: `labs/day-03/lab3/`

Participants:

- JWT on both APIs (401 without token, 403 wrong scope)
- Transaction Service forwards the caller JWT to Account Service
- Resilience4j timeout + circuit breaker; fallback is **503**, never a fake ACTIVE account
- Unit tests + integration test; no `Authorization` text in logs

---

## 8. Day 4 — Observability, Containers, OpenShift, and CI/CD

**Outcome:** Account and Transaction are observable, containerized, **deployed to the assigned OpenShift project**, pipeline evidence captured, rollback demonstrated.

### 8.1 Lecture focus

- Structured logging, correlation IDs, Actuator, Micrometer; tracing demo (OpenTelemetry)
- Secure Containerfiles, non-root, registries, immutable tags
- OpenShift projects, Deployments, Services, Routes, ConfigMaps, Secrets, probes, scale, rollout/rollback
- OpenShift Pipelines (Tekton) stages; Jenkins as **awareness**; GitOps **conceptual**

### 8.2 Lab 4 — Deploy the Capstone Services

Path: `labs/day-04/lab4/` · Images built from your completed Lab 3 **starter** context

**Required on ARO** (`oc login`). Skip `00-namespace.yaml`. Apply with `oc apply -n <project>`.

Participants:

- Multi-stage non-root images `md287/account-service:1.0.0` and `md287/transaction-service:1.0.0`
- Compose probes on the VM, then push via `tools/push-images.ps1`
- ConfigMaps vs Secrets; probes; resource requests/limits; Account **Route readiness 200**
- Prepared pipeline: `tools/run-pipeline-locally.ps1` (scan PASS/FAIL, SBOM, signature command)
- `oc set env` version bump then **`oc rollout undo`**

---

## 9. Day 5 — OpenShift AI, MCP, and Capstone Completion

**Outcome:** **Risk Assessment Service** integrated with a pre-deployed model endpoint; policy + human review; MCP awareness; capstone demo.

### 9.1 Lecture focus

- Calling a model: auth, validation, timeouts, safe fallback, audit fields
- Workbenches, model registry, KServe — **lecture/demo only**
- Deterministic policy vs model score; human-review routing
- Copilot governance reminder
- Agentic AI and MCP (clients, servers, tools, auth, HITL, audit) — **Exercise 5.3 worksheet, no MCP server**

### 9.2 Lab 5 — Risk Assessment Service

Path: `labs/day-05/lab5/` · Port **8083** · Postgres **5435** · local mock **8090**

**Required on ARO:** instructor `MD287_MODEL_ROUTE` health; deploy Risk; Route evidence.

Participants:

- Consume `TransactionSubmitted`; call model with Bearer API key
- Timeout / 5xx → HOLD `MODEL_UNAVAILABLE` (never auto-approve)
- `PolicyEngine` APPROVE / HOLD / DECLINE; human review on HOLD (`risk.write`)
- Persist model name/version, score, policy version, correlation id
- Push `md287/risk-assessment-service:1.0.0` via `tools/push-risk-image.ps1`

### 9.3 Required capstone demonstration

1. End-to-end successful transaction  
2. Duplicate-event handling  
3. Model failure / timeout (safe fallback)  
4. Human-review routing  
5. OpenShift deployment evidence  
6. Rollback or recovery  

---

## 10. Capstone components (required)

| Component | Where it is built |
| --------- | ----------------- |
| Account Service + own DB | Lab 1, hardened Lab 3, deployed Lab 4 |
| Transaction Service + own DB + Kafka | Lab 2, hardened Lab 3, deployed Lab 4 |
| Risk Assessment Service + own DB | Lab 5 |
| Sync REST + async events | Labs 2 and 5 |
| JWT, resilience, tests, log hygiene | Lab 3 |
| OpenShift deploy, probes, rollback | Labs 4–5 |
| Model endpoint + policy + human review + audit | Lab 5 |

**Not in the required capstone:** Customer Service, Notification Service, Redis, API gateway, Grafana dashboard, blue-green/canary, participant-built MCP server, Keycloak login UI.

---

## 11. Assessment (as designed)

| Cadence | Format |
| ------- | ------ |
| After each module | Kahoot, 15 questions |
| Days 1–4 | Daily knowledge check, 5–10 questions, 15–20 min |
| Labs 1–5 | Functional completion against the lab success criteria |
| Day 5 | Short check + team capstone demonstration |

Proposed weights (client-facing spec): labs 30% · knowledge checks 10% · security/testing/quality 15% · capstone implementation 25% · AI-governance correctness 10% · demonstration 10%. Proposed pass: **70%** overall, no category below **60%** (subject to client approval).

---

## 12. Runtime used in the labs (frozen)

| Item | Lab value |
| ---- | --------- |
| JDK | **21** |
| Spring Boot | **3.4.5** |
| Maven | 3.9.x |
| PostgreSQL image | `postgres:16-alpine` |
| Kafka image | `apache/kafka:3.8.1` |
| Container runtime | Docker Desktop (Compose v2) |
| JWT | HMAC classroom secret via `issue-jwt.py` |
| OpenShift | ARO; `oc` 4.x; default `*.aroapp.io` |
| Model | HTTP stand-in `md287-risk-model:8090` (same contract as `mock-model.py`) |

Full environment: `docs/SYSTEM-REQUIREMENTS.md`.

---

## 13. Repository layout (as designed)

```text
MD287/
├── README.md · OUTLINE.md
├── curriculum/day-01 … day-05     ← DAY-CURRICULUM, slides_outline, diagrams
├── labs/day-01 … day-05           ← one lab per day: LAB-N-GUIDE, starter
├── instructor/                    ← module-notes, teach-guides
├── decks/                         ← pptx · pdf · marp
├── kahoot/                        ← 13 module quizzes
├── docs/                          ← client / instructor documents
└── scripts/
```

Parallel IDs: Day N curriculum folder = Day N lab folder = modules listed in §3.

---

## 14. What is lecture-only (by design)

| Topic | Why it is not a lab install |
| ----- | --------------------------- |
| Keycloak / Azure AD | HMAC JWT is the classroom IdP stand-in |
| Redis | Not used by the three-service capstone |
| Jenkins / GitHub Actions | Awareness; pipeline evidence is local script (+ optional Tekton if operator exists) |
| OpenShift AI workbenches / KServe admin | Application integration only |
| MCP server | Exercise 5.3 |
| GitHub Copilot seats | Instructor demo; labs do not require Copilot |
| Podman | Labs use Docker Desktop |
| Custom domain | Default ARO apps domain |

---

© 2026 Innovation In Software Corporation
