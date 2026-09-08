# MD287 System Requirements

**Document title:** Lab execution system requirements  
**Course:** Spring Boot Microservices with OpenShift AI and DevOps  
**Client:** Bank of America  
**Prepared by:** Innovation In Software Corporation  
**Version:** 2.0  
**Date:** 8 September 2026  
**Status:** Ready for TEKsystems (VM) and IIS (ARO)  
**Source of truth:** Lab guides `labs/day-01` … `labs/day-05` and `OUTLINE.md` Lab 1–5 bullets  
**Runtime:** Spring Boot **3.4.5** · Java **21**  
**Wording:** **participants** (not students)

**Shareable Word copy:** [`MD287_System_Requirements.docx`](MD287_System_Requirements.docx)  
**Flowchart:** [`lab-environment-flow.svg`](lab-environment-flow.svg)

```powershell
git clone https://github.com/Innovation-In-Software/springboot-microservices-with-openshift-ai-and-devops-participant.git MD287
```

Participants clone this **public** repo themselves. No GitHub login. Do **not** pre-copy labs onto the VM.

---

## 1. Purpose and scope

This document specifies the environments needed to **execute Labs 1–5** of the AI-Assisted Banking Transaction Risk Platform (Account → Transaction → Risk Assessment).

It covers:

- The **participant workstation** (TEKsystems Ablaze Desktop VM) used every day
- The **Azure Red Hat OpenShift (ARO)** classroom cluster (IIS) required on Days 4–5
- Software versions, Docker images, host ports, network, classroom secrets, and acceptance checks
- What is **not** required for the labs (lecture-only stack items)

It does **not** cover: slide delivery laptops, Kahoot, GitHub Copilot licensing, or production Bank of America systems.

---

## 2. Audience and ownership

| Audience | How they use this document |
| -------- | -------------------------- |
| **TEKsystems** | Confirm the Ablaze VM image already meets Part A. **Keep the image as provided.** |
| **IIS platform** | Provision Part B (ARO) **before Day 4**. Tear down after class. |
| **Instructors** | Issue `oc login` and `MD287_MODEL_ROUTE` on Day 4. Run the smoke tests. |
| **Participants** | Clone the public repo on Day 1. Work in each lab `starter/` folder. |

| Piece | Owner | Required? | Role |
| ----- | ----- | --------- | ---- |
| Participant VM | **TEKsystems** (Ablaze Desktop) | **Yes — all five days** | Code, Compose, Maven, JWT scripts, `docker build`, `oc` client |
| ARO OpenShift | **IIS** | **Yes — Days 4 and 5** | One project per participant, backing services, model Route, image-registry Route |
| Course repo | Participants | **Yes** | Public clone on Day 1 |

Days 1–3: VM only (ARO idle). Days 4–5: **same VM plus** the assigned ARO project. Compose on the VM proves probes before push; it does **not** replace OpenShift deploy. Labs 4 and 5 **do not complete** without `oc login`.

---

## 3. Delivery architecture

![TEKsystems VM every day; IIS ARO required on Days 4–5](lab-environment-flow.svg)

```mermaid
flowchart TB
  TEK[TEKsystems VM — required all days]
  IIS[IIS ARO — required Days 4–5]
  P[Participant logs on and clones the public repo]
  TEK --> P
  L1[Lab 1 Account — VM :8081 :5433]
  L2[Lab 2 Transaction — VM :8082 Kafka :9092]
  L3[Lab 3 JWT — VM issue-jwt.py]
  P --> L1 --> L2 --> L3
  L4[Lab 4: docker build on VM then oc apply + push + rollout undo]
  L3 --> L4
  IIS --> L4
  L5[Lab 5: code on VM then model Route + deploy Risk]
  L4 --> L5
  IIS --> L5
```

Three Java services, each with its **own** database:

| Service | Lab introduced | VM host ports | OpenShift objects |
| ------- | -------------- | ------------- | ----------------- |
| Account Service | 1 (hardened in 3, deployed in 4) | App **8081**, Postgres **5433** | `account-service` Deployment / Service / Route · `account-db` |
| Transaction Service | 2 (hardened in 3, deployed in 4) | App **8082**, Postgres **5434**, Kafka **9092** | `transaction-service` · `transaction-db` · `kafka:19092` |
| Risk Assessment Service | 5 | App **8083**, Postgres **5435**, mock model **8090** | `risk-assessment-service` · `risk-db` · `md287-risk-model:8090` |

---

## 4. Participant VM (TEKsystems) — required, keep as-is

Do **not** re-image for this delivery. The Ablaze Desktop gold image is the workstation.

### 4.1 Hardware

| ID | Item | Requirement | Notes from Ablaze review |
| -- | ---- | ----------- | ------------------------ |
| HW-1 | CPU | 4 logical processors (8 preferred) | Image: 4 CPUs to Docker |
| HW-2 | RAM | 8 GB minimum; **16 GB preferred** | Docker engine ~6–8 GB |
| HW-3 | Free disk | 30 GB (40 GB preferred) | Images + `~/.m2` + three service builds |
| HW-4 | Docker Desktop resources | 4 CPUs and 6–8 GB RAM (WSL2) | Measured: 4 CPU / ~7.8 GiB |

### 4.2 Operating system and shell

| ID | Item | Requirement |
| -- | ---- | ----------- |
| OS-1 | OS | Windows 10 (21H2+) or Windows 11, 64-bit. Ablaze guest measured: Windows 10 `10.0.19045`. |
| OS-2 | WSL2 | Enabled (Docker Desktop backend) |
| OS-3 | Shell | Windows PowerShell in **VS Code** (Ctrl+`). HTTP uses **`curl.exe`**, not `curl` (PowerShell aliases `curl`). |
| OS-4 | Editor | VS Code **1.134+** with Extension Pack for Java, Spring Boot, Docker, YAML |
| OS-5 | Not required on the VM | Cursor, Spring Boot CLI, GitHub account, IntelliJ (may be present; labs use VS Code) |

### 4.3 Software

| ID | Software | Required version | Ablaze measured | Labs |
| -- | -------- | ---------------- | --------------- | ---- |
| SW-1 | JDK (OpenJDK / Temurin) | **21 only** on PATH (`JAVA_HOME` → 21) | OpenJDK 21 at `C:\Program Files\OpenJDK\jdk-21` | 1–5 |
| SW-2 | Apache Maven | **3.9.x** (3.9.12 OK); must use Java 21 | 3.9.12, `C:\maven` | 1–5 |
| SW-3 | Git | 2.40+ | 2.52.0.windows.1 | Clone |
| SW-4 | Docker Desktop | 4.x, Compose v2, **engine running at logon** | v4.88.1, Compose v5.4.0, WSL2 | 1–5 |
| SW-5 | Python | **3.12+** on PATH (not the Microsoft Store stub) | Required for `issue-jwt.py` | 3–5 |
| SW-6 | `curl.exe` | Windows inbox | Present | 1–5 |
| SW-7 | OpenShift CLI `oc` | **4.x**, match the ARO cluster | Client 4.22.10 | **4–5 required** |

PATH rule: `java -version` must print **21**. A second JDK 17 on PATH causes support tickets — JDK 21 must be first.

Docker rule: `docker info` must print a **Server Version** without the participant launching Docker by hand (wait up to two minutes after logon). Windows Firewall must **allow Docker Desktop Backend**.

### 4.4 Docker images to pre-pull on the VM

| ID | Image | Why |
| -- | ----- | --- |
| IMG-1 | `postgres:16-alpine` | Account, Transaction, and Risk databases |
| IMG-2 | `apache/kafka:3.8.1` | Labs 2–5 Compose Kafka (KRaft) |
| IMG-3 | `python:3.12-alpine` | Lab 5 mock model; Lab 3 JWT fallback if host Python is down |
| IMG-4 | `maven:3.9.9-eclipse-temurin-21-alpine` | Lab 4/5 Containerfile build stage |
| IMG-5 | `eclipse-temurin:21-jre-alpine` | Lab 4/5 Containerfile runtime stage |

Participants **build** (not pre-load) these tags, then push them to ARO:

| Local tag | Lab | Push script |
| --------- | --- | ----------- |
| `md287/account-service:1.0.0` | 4 | `labs/day-04/lab4/tools/push-images.ps1` |
| `md287/transaction-service:1.0.0` | 4 | same |
| `md287/risk-assessment-service:1.0.0` | 5 | `labs/day-05/lab5/tools/push-risk-image.ps1` |

### 4.5 Host ports (must be free on the VM)

Only **one** Compose stack should bind these ports at a time. Lab 4/5 guides tell participants to `docker compose down` older labs first.

| Port | Process | Labs |
| ---- | ------- | ---- |
| 8081 | Account Service | 1–4 |
| 8082 | Transaction Service | 2–4 |
| 8083 | Risk Assessment Service | 5 |
| 5433 | Account Postgres (`account_db` / user `account`) | 1–4 |
| 5434 | Transaction Postgres (`transaction_db` / user `transaction`) | 2–4 |
| 5435 | Risk Postgres (`risk_db` / user `risk`) | 5 |
| 9092 | Kafka advertised to the host (`PLAINTEXT_HOST`) | 2–5 |
| 8090 | Local OpenShift AI stand-in (`mock-model.py`) | 5 |

Kafka **inside** Compose also listens on **19092** (`PLAINTEXT://kafka:19092`) for container-to-container traffic. That is the same port used on ARO (`kafka:19092`).

### 4.6 Network from the VM

Outbound HTTPS (443) from the TEKsystems VM to:

| Destination | Why |
| ----------- | --- |
| `github.com` | Public clone (no authentication) |
| Maven Central (`repo.maven.apache.org` and mirrors) | First `mvn` / image build |
| Docker Hub | Image pulls if not pre-pulled |
| ARO API server | `oc login` (Days 4–5) |
| ARO apps domain `*.eastus2.aroapp.io` | Routes (Account, Risk, model, registry) |
| Image-registry default Route | `docker login` / `docker push` |

Loopback `localhost` must work. Proxy exceptions if the classroom uses an HTTP proxy.

### 4.7 VM smoke test (Day 1, after clone)

From `labs\day-01\lab1\starter\account-service`:

```powershell
java -version
mvn -version
docker info
oc version --client
git --version
python --version
docker compose up -d
mvn spring-boot:run
```

Pass: `java` shows 21; `mvn` uses Java 21; `docker info` shows a Server Version; `python` is 3.12+ (not Store stub); `curl.exe -s http://localhost:8081/actuator/health` returns `"status":"UP"`.

---

## 5. IIS ARO cluster — required for Labs 4–5

Stand up **before Day 4**. Tear down after class. Use the **default** apps subdomain — **no custom domain**.

### 5.1 Cluster

| ID | Item | Requirement |
| -- | ---- | ----------- |
| CL-1 | Product | **Azure Red Hat OpenShift (ARO)** |
| CL-2 | Region | **East US 2** |
| CL-3 | Topology | 3 control plane + 3 workers (`Standard_D8s_v3` unless headcount needs more) |
| CL-4 | Apps URL | Default `apps.<cluster>.eastus2.aroapp.io` |
| CL-5 | Reachability | API, Routes, and **image-registry default Route** reachable from TEKsystems VMs |
| CL-6 | TLS | Classroom Routes may use the default router cert; labs allow `curl.exe -k` if needed |

### 5.2 Per-participant project

| ID | Item | Requirement |
| -- | ---- | ----------- |
| NS-1 | Namespace | **One project per participant**, named after their account, **pre-created** |
| NS-2 | Participant action | `oc project <assigned>`. **Skip** `openshift/00-namespace.yaml`. Apply with `oc apply -n <project> -f ...` |
| NS-3 | RBAC | **edit** in that project only: Deployment, Service, Route, ConfigMap, Secret, ImageStream/`docker push`, `oc rollout undo`, `oc set image`, `oc set env` |
| NS-4 | Isolation | Participant cannot list or enter other participants’ projects |
| NS-5 | Login | `oc login <api-url> --username <participant> --password <password>` **before Lab 4** |
| NS-6 | Secrets channel | Azure Key Vault (same pattern as other IIS classes) for API URL, username, password |

Participants do **not** create namespaces, install operators, or administer the cluster.

### 5.3 Services in each participant project

Names, ports, and credentials **must** match lab ConfigMaps and Secrets.

| ID | Service | Port | Details | Lab |
| -- | ------- | ---- | ------- | --- |
| SVC-1 | `account-db` | 5432 | PostgreSQL 16. Database `account_db`. User/password `account` / `account` | 4 |
| SVC-2 | `transaction-db` | 5432 | PostgreSQL 16. Database `transaction_db`. User/password `transaction` / `transaction` | 4 |
| SVC-3 | `risk-db` | 5432 | PostgreSQL 16. Database `risk_db`. User/password `risk` / `risk` | 5 |
| SVC-4 | `kafka` | **19092** | Bootstrap **`kafka:19092`**. Topics `transactions.submitted` and `transactions.submitted.DLT` (1 partition, RF 1 is OK) | 4, 5 |
| SVC-5 | `md287-risk-model` | **8090** | Pre-deployed OpenShift AI **stand-in** (same HTTP contract as `labs/day-05/lab5/tools/mock-model.py`). Expose a Route. Give participants the URL as `MD287_MODEL_ROUTE` | 5 |

ConfigMap JDBC URLs used by the labs:

```text
jdbc:postgresql://account-db:5432/account_db
jdbc:postgresql://transaction-db:5432/transaction_db
jdbc:postgresql://risk-db:5432/risk_db
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:19092
MD287_ACCOUNT_SERVICE_BASE_URL=http://account-service:8081
MD287_MODEL_BASE_URL=http://md287-risk-model:8090
```

### 5.4 OpenShift AI model stand-in (required)

The outline requires a **pre-deployed OpenShift AI** model endpoint (auth, timeout, fallback). Workbenches, notebooks, and KServe internals stay **lecture-only**. IIS deploys an HTTP service that matches the classroom contract:

| Item | Value |
| ---- | ----- |
| Service name | `md287-risk-model` |
| In-cluster URL | `http://md287-risk-model:8090` |
| Health | `GET /v1/health` → `{"status":"UP","modelName":"md287-risk-model","modelVersion":"1.0.0"}` |
| Score | `POST /v1/score` with `Authorization: Bearer md287-classroom-model-token` |
| Success body | `{"score": <0-99>, "modelName":"md287-risk-model","modelVersion":"1.0.0"}` |
| Timeout drill | amount `13.13` sleeps ~10s (caller times out → HOLD) |
| Error drill | amount `66.66` returns HTTP 500 |
| Participant env | Instructor publishes Route as **`MD287_MODEL_ROUTE`** (no trailing slash) |

Reference implementation: `labs/day-05/lab5/tools/mock-model.py`.

### 5.5 Image registry (required)

Without this, `oc apply` yields **ImagePullBackOff**. YAML ships with local names (`md287/...:1.0.0`) until `oc set image` after push.

| ID | Item | Requirement |
| -- | ---- | ----------- |
| REG-1 | External push | Expose `default-route` in `openshift-image-registry` **or** set `MD287_REGISTRY` for every participant |
| REG-2 | Login | Participant can `docker login` with `oc whoami` / `oc whoami -t` |
| REG-3 | Push tags | `<registry>/<project>/account-service:1.0.0`, `transaction-service:1.0.0`, `risk-assessment-service:1.0.0` |
| REG-4 | In-cluster pull | `image-registry.openshift-image-registry.svc:5000/<project>/<name>:1.0.0` |

Scripts: `labs/day-04/lab4/tools/push-images.ps1` and `labs/day-05/lab5/tools/push-risk-image.ps1`.

### 5.6 IIS verification (before Day 4)

From a TEKsystems VM using one participant account:

```powershell
oc login <api-url> --username <participant> --password <password>
oc project <participant-project>
oc get svc account-db transaction-db risk-db kafka md287-risk-model
oc get route md287-risk-model
```

- [ ] Participant can log in; cannot see other projects
- [ ] All five Services exist in the assigned project
- [ ] Kafka answers on **19092** from in-cluster clients
- [ ] Topics `transactions.submitted` and `transactions.submitted.DLT` exist
- [ ] Model Route `GET /v1/health` is UP; instructor can quote `MD287_MODEL_ROUTE`
- [ ] Image-registry Route allows `docker push` from the VM
- [ ] Participant can create a Route; default `*.aroapp.io` works
- [ ] `oc rollout undo` is permitted on Deployments in that project

### 5.7 Do not provision

These appear on the **slide** technology stack. They are **not** in Labs 1–5.

| Skip | Why |
| ---- | --- |
| Keycloak / Azure AD / client IdP | Labs use `issue-jwt.py` HMAC JWT on the VM |
| Redis | Not used |
| Jenkins | Outline: awareness only |
| OpenShift Pipelines operator | Required pipeline evidence is `labs/day-04/lab4/tools/run-pipeline-locally.ps1` (SBOM / scan / sign). Tekton YAML is optional if the operator happens to be present |
| OpenShift AI operator, workbenches, KServe | Lecture only; pre-deploy **`md287-risk-model`** HTTP stand-in |
| MCP server process | Exercise 5.3 is a worksheet |
| Custom domain / TLS cert purchase | Default `*.aroapp.io` |
| GitHub Copilot seats | Instructor demo only |
| Standalone IIS Kafka or SQL outside the project | Compose on the VM (Days 1–5) + Services in §5.3 (Days 4–5) |
| Podman | Labs use Docker Desktop |
| Spring Boot CLI | Labs use Maven |

---

## 6. Course repository

| Item | Value |
| ---- | ----- |
| Public clone | `https://github.com/Innovation-In-Software/springboot-microservices-with-openshift-ai-and-devops-participant.git` |
| Default branch | `main` |
| Login | None |
| Work folder | Each lab `starter/` (this participant repo does not include `solution/`) |
| Day slide PDFs | `slides/MD287_DayN_Slides.pdf` |
| Do not pre-copy | Labs must **not** be baked onto the VM as a substitute for clone |

Suggested clone location: `%USERPROFILE%\MD287`.

---

## 7. Week flow (what runs where)

| Day | Lab | On the TEKsystems VM | On IIS ARO |
| --- | --- | -------------------- | ---------- |
| 1 | Account Service | Compose Postgres **5433**; `mvn spring-boot:run` **8081**; OpenAPI / Actuator | Idle |
| 2 | Transaction Service | Lab 1 still on **8081**; Compose Postgres **5434** + Kafka **9092**; app **8082**; REST validate + Kafka publish | Idle |
| 3 | Secure and resilient | `issue-jwt.py`; JWT on APIs; Resilience4j; integration test `AccountPersistenceTest` | Idle |
| 4 | Deploy capstone services | `docker build` non-root images; Compose probes; `run-pipeline-locally.ps1` | **`oc login`**, skip `00-namespace.yaml`, `oc apply`, `push-images.ps1`, Account Route readiness **200**, **`oc rollout undo`** |
| 5 | Risk Assessment | PolicyEngine; local mock **8090**; sample `TransactionSubmitted` events | **`MD287_MODEL_ROUTE`** health; `push-risk-image.ps1`; Risk Route **200** |

Capstone evidence that needs the cluster: OpenShift deploy, Route 200, rollback history, model endpoint call with timeout/fallback.

---

## 8. Classroom secrets (lab values only)

These are **classroom HMAC / DB passwords**, not Bank of America secrets. Keep Git, ConfigMaps, and seeded cluster Secrets in sync.

| Name | Value | Used by |
| ---- | ----- | ------- |
| JWT HMAC | `md287-lab-only-hmac-secret-32bytes!` | Labs 3–5; OpenShift Secret `MD287_JWT_SECRET` |
| Model API key | `md287-classroom-model-token` | Lab 5 `Authorization: Bearer`; Secret `MD287_MODEL_API_KEY` |
| Account DB | `account` / `account` | Compose + `account-db` |
| Transaction DB | `transaction` / `transaction` | Compose + `transaction-db` |
| Risk DB | `risk` / `risk` | Compose + `risk-db` |

Participant OpenShift passwords go through the IIS Key Vault channel — not this repo.

---

## 9. Acceptance checklist

### 9.1 TEKsystems (image as provided)

- [ ] Fresh logon: Docker engine up without a manual start
- [ ] `java -version` → 21 first on PATH
- [ ] `mvn -version` uses Java 21
- [ ] `python --version` → 3.12+ (not Store stub)
- [ ] `oc version --client` → 4.x
- [ ] `git clone` of the public URL succeeds (no GitHub login)
- [ ] Lab 1 health `http://localhost:8081/actuator/health` is UP
- [ ] Images in §4.4 present or pullable on Day 1
- [ ] Windows Firewall allows Docker Desktop Backend

### 9.2 IIS (before Day 4)

- [ ] ARO East US 2 reachable from the Ablaze VMs
- [ ] One project per participant; `edit` only
- [ ] Services `account-db`, `transaction-db`, `risk-db`, `kafka`, `md287-risk-model`
- [ ] Kafka **19092** + both topics
- [ ] Model Route health UP; `MD287_MODEL_ROUTE` ready to hand out
- [ ] Image-registry Route (or `MD287_REGISTRY`) allows push
- [ ] Default `*.aroapp.io` Routes work

### 9.3 Instructor (Day 4 morning)

- [ ] Issue API URL, username, password per participant
- [ ] Confirm `oc whoami` from a sample VM
- [ ] Quote `MD287_MODEL_ROUTE` and, if needed, `MD287_REGISTRY`

---

## 10. RACI

| Activity | TEKsystems | IIS | Instructor | Participant |
| -------- | ---------- | --- | ---------- | ----------- |
| Provide / keep Ablaze VM | **A/R** | I | C | I |
| Clone public repo | I | I | C | **R** |
| Provision ARO + per-project services | I | **A/R** | C | I |
| Issue `oc login` and model Route | I | C | **A/R** | I |
| Execute Labs 1–5 | I | I | C | **R** |
| Tear down ARO after class | I | **A/R** | C | I |

R = Responsible · A = Accountable · C = Consulted · I = Informed

---

## 11. Related documents

| Document | Who | Purpose |
| -------- | --- | ------- |
| [`MD287_System_Requirements.docx`](MD287_System_Requirements.docx) | TEKsystems, IIS, instructors | **Complete** Word copy of this spec |
| [`lab-environment-flow.svg`](lab-environment-flow.svg) | All | Delivery flowchart |
| [`MD287_Lab_Workstation_System_Requirements.docx`](MD287_Lab_Workstation_System_Requirements.docx) | TEKsystems | Short VM install list |
| [`MD287_IIS_Pending_System_Requirements.docx`](MD287_IIS_Pending_System_Requirements.docx) | IIS | Short ARO provisioning list |
| [`MD287_Complete_Course_Outline.md`](MD287_Complete_Course_Outline.md) | Participants | Week outline as designed |
| [`../labs/LABS-INDEX.md`](../labs/LABS-INDEX.md) | Participants | Lab entry points |

---

© 2026 Innovation In Software Corporation
