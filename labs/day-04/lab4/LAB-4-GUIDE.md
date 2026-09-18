# Lab 4 — Deploy the Capstone Services

**Day:** 4 — Observability, Containers, OpenShift, and CI/CD  
**Capstone:** Package and operate services 1 and 2 of the AI-Assisted Banking Transaction Risk Platform  
**Time:** 90–120 minutes  
**Difficulty:** Beginner

**Objective:** Containerize Account and Transaction services (non-root images), run them with Docker Compose, **deploy them to the pre-provisioned OpenShift project**, walk a prepared pipeline (SBOM, signature, vulnerability gate), then roll forward a version and **roll it back on OpenShift**.

This is an **ops lab**. You do **not** recopy Lab 3 Java. Images are built from **your finished Lab 3 starter**:

- `labs\day-03\lab3\starter\account-service`
- `labs\day-03\lab3\starter\transaction-service`

Do **not** copy from a `solution/` folder. The participant pack does not include one.

---

## What you will finish with

By the end of this lab you will have:

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
| **Non-root image** | `USER md287` in the Containerfile. OpenShift also sets `runAsNonRoot: true` and `runAsUser: 100`. |
| **Config vs secret** | JDBC URL in a ConfigMap. DB password and JWT secret in a Secret. |
| **Immutable tags** | Promote `1.0.0` / `1.0.1`, not `:latest`. |
| **Pipeline gates** | CRITICAL CVE → fail. SBOM is the ingredients list. Unsigned images must not run. |
| **Rollback** | **`oc rollout undo` on the assigned project** (required). |
| **GitHub Copilot Free** | Already on this VM. Ask Copilot to *explain* `USER md287` / non-root. **Review before accept.** |

### What you are not installing today

Postgres, Kafka, and the image registry are **platform** concerns. Compose starts Postgres and Kafka on the VM so you can prove probes before you push. On the classroom ARO cluster those backing services are **pre-provisioned** in **your assigned project** (service names `account-db`, `transaction-db`, `kafka` on **19092**).

**`oc login` is required in this lab.** If `oc whoami` fails, stop and get the instructor — do not skip the cluster steps.

Jenkins is **awareness only**. Do not install Jenkins. GitOps is conceptual.

---

## Environment basics (read this first)

Do **all** of this **on the Ablaze VM**. Your laptop is only the browser. Copy **one block at a time**. Do not paste two commands on the same line. Do **not** paste this whole guide (or a chat) into the terminal.

**Repo root (from Lab 0):** `%USERPROFILE%\MD287`. Example: `C:\Users\student.VLAB\MD287`. Do **not** clone. Do **not** run `mklink`. If the prompt shows the long `.vscode\...-participant` path, that is the same repo — `cd` to `MD287` before git commands.

Work in `labs\day-04\lab4\starter\`. Java stays in the Lab 3 starter you finished yesterday.

| Task | How |
| --- | --- |
| Folder | **File → Open Folder** → `%USERPROFILE%\MD287` |
| Terminal 1 | Docker build / `docker compose` / `oc` |
| Terminal 2 | `curl.exe` and `python tools\issue-jwt.py` |
| HTTP | **`curl.exe`** (not `curl`). On OpenShift Routes use **`curl.exe -k`** in this lab (classroom certificate). |
| Tokens | From `labs\day-03\lab3`: `$TELLER = (python tools\issue-jwt.py teller).Trim()` |
| Stop older labs first | Lab 1–3 Compose stacks use the same host ports |
| GitHub Copilot | Signed in during Lab 0. Review before accept. |

**You need:** Docker Desktop, Python 3 for JWTs, **OpenShift `oc`**, and classroom `oc login` to your assigned project. Finish **Lab 3** first so the image contains JWT and the circuit breaker.

| Cluster item | Value |
| --- | --- |
| API | `https://api.aro-md287.centralus.aroapp.io:6443/` |
| OpenShift username | `student01` … `student25` from the instructor. **Not** `student.VLAB`. **Not** `MSMICR26-26`. |
| OpenShift project | `md287-student01` (same number as your OpenShift username) |
| Password | Class password from the instructor. Do not use `MSMICR26-TD`. |
| Image registry | `default-route-openshift-image-registry.apps.aro-md287.centralus.aroapp.io` |

**Port map (same as Labs 1–3):**

| Process | Host port |
| --- | --- |
| Account Service | 8081 |
| Transaction Service | 8082 |
| Account Postgres | 5433 |
| Transaction Postgres | 5434 |
| Kafka | 9092 |

---

## Steps from the training slides

Follow these steps in order. Finish one step before starting the next.

### Step 0 — Pull the latest repo

Get the latest Lab 4 guide and starter from GitHub. Do **not** clone. Do **not** run `mklink`.

```powershell
cd $env:USERPROFILE\MD287
git pull
```

**Expected:** `Already up to date.` or a Fast-forward. Prompt ends with `\MD287>`.

Then: **File → Open Folder** → `%USERPROFILE%\MD287` if it is not already open.

`git pull` only works **inside** the repo. Do not run it from `C:\Users\student.VLAB`.

If pull aborts with **Your local changes … would be overwritten by merge** on `openshift\10-account.yaml` / `20-transaction.yaml`, you already filled those files in this lab. Stash, pull, then restore — see **Troubleshooting**. Do **not** `git checkout --` those two files.

### Step 1 — Stop older labs and confirm the Lab 3 starter

You package **your** Lab 3 services. You do not rebuild JWT or Kafka logic here.

**Do this:**

1. Stop leftover Maven (**Ctrl+C**, then **`Y`** in any `spring-boot:run` tab). Then stop Lab 1–3 Compose so ports are free:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-01\lab1\starter\account-service"
docker compose down
cd "$env:USERPROFILE\MD287\labs\day-02\lab2\starter\transaction-service"
docker compose down
cd "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\account-service"
docker compose down
cd "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\transaction-service"
docker compose down
```

If a port is still busy:

```powershell
foreach ($port in 8081, 8082) {
  $p = (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1).OwningProcess
  if ($p) { Stop-Process -Id $p -Force; "Stopped PID $p on $port" } else { "$port is free" }
}
docker ps
```

2. Confirm Actuator probes are on in the Lab 3 starter:

```powershell
Select-String -Path "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\account-service\src\main\resources\application.yml" -Pattern "probes"
Get-Content "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\account-service\.dockerignore"
```

Need `probes:` / `enabled: true`, and a `.dockerignore` that lists `target/` and `src/test/`.

**Expected result:** you know the Docker **context** is the Lab 3 **starter**, and the Containerfile lives in Lab 4.

**Why this matters:** Twelve-factor **build** is separate from **run**. The JAR you ship should be the one you already tested.

---

### Step 2 — Finish the Containerfiles (non-root)

Stop any running compose. Replace both Containerfiles. GitHub Copilot can explain `USER md287` — **review before accept**. Do not accept a `USER root` image.

**Do this:**

```powershell
cd "$env:USERPROFILE\MD287\labs\day-04\lab4\starter"
@'
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S md287 && adduser -S md287 -G md287
COPY --from=build /src/target/*.jar app.jar
RUN chown md287:md287 /app/app.jar

USER md287
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
'@ | Set-Content -Encoding ascii "account-service\Containerfile"

@'
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S md287 && adduser -S md287 -G md287
COPY --from=build /src/target/*.jar app.jar
RUN chown md287:md287 /app/app.jar

USER md287
EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
'@ | Set-Content -Encoding ascii "transaction-service\Containerfile"
Select-String -Path "account-service\Containerfile","transaction-service\Containerfile" -Pattern "TODO|USER md287|EXPOSE"
```

Need `USER md287`, `EXPOSE 8081` / `8082`, and **no** `TODO`.

Build Account (first build downloads Maven inside Docker — several minutes). Leave this in **Terminal 1**:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-04\lab4\starter"
docker build `
  -f account-service\Containerfile `
  -t md287/account-service:1.0.0 `
  "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\account-service"
```

Then Transaction:

```powershell
docker build `
  -f transaction-service\Containerfile `
  -t md287/transaction-service:1.0.0 `
  "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\transaction-service"
```

Confirm the image is not root:

```powershell
docker run --rm --entrypoint id md287/account-service:1.0.0
docker images md287/account-service
docker images md287/transaction-service
```

**Expected result:** `uid=100(md287) gid=100(md287)` — **not** `uid=0(root)`. Both images show tag `1.0.0`. Build logs end with `Successfully tagged md287/account-service:1.0.0` (and the same for transaction).

**Why this matters:** A container that runs as root is one break-out away from host power. OpenShift will often **refuse** a root image when `runAsNonRoot: true` is set.

---

### Step 3 — Exercise 4.2 recap (layered image)

```powershell
docker history md287/account-service:1.0.0
```

**Expected result:** a Maven build stage is **not** in the final history (multi-stage left only the JRE + JAR). You should see `EXPOSE` and `USER`.

**Why this matters:** The compiler and Maven cache must not ship to production.

---

### Step 4 — Run the stack and prove probes, metrics, and correlation

**Do this** in **Terminal 1**:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-04\lab4\starter"
docker compose up -d --build
Start-Sleep -Seconds 30
docker compose ps
```

Wait until `md287-lab4-account` and `md287-lab4-transaction` are running (first boot runs Flyway). If status is still `starting`, wait 15 seconds and run `docker compose ps` again. `md287-lab4-kafka-init` should have **exited**.

**Terminal 2:**

```powershell
curl.exe -s http://localhost:8081/actuator/health/liveness
curl.exe -s http://localhost:8081/actuator/health/readiness
curl.exe -s http://localhost:8082/actuator/health/readiness
curl.exe -s http://localhost:8081/actuator/info
```

Issue tokens (keep this window so `$TELLER` / `$OPS` stay set):

```powershell
cd "$env:USERPROFILE\MD287\labs\day-03\lab3"
$TELLER = (python tools\issue-jwt.py teller).Trim()
$OPS = (python tools\issue-jwt.py ops).Trim()
"teller length=$($TELLER.Length) ops length=$($OPS.Length)"
```

```powershell
curl.exe -s -H "Authorization: Bearer $TELLER" http://localhost:8081/actuator/metrics

cd "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\account-service"
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Authorization: Bearer $TELLER" `
  -H "Content-Type: application/json" `
  --data-binary "@requests/create-valid.json" `
  http://localhost:8081/api/v1/accounts
```

Need **HTTP:201**. Copy **your** `accountId`. Do **not** call `ACC-YOUR-ID`.

```powershell
$accountId = "ACC-AABBCCDD"
```

Change the value to **your** create response, then activate:

```powershell
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Authorization: Bearer $TELLER" `
  -X POST http://localhost:8081/api/v1/accounts/$accountId/activate
```

Need **HTTP:200** `"status":"ACTIVE"`.

Write the Lab 4 transaction request:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-04\lab4"
@"
{
  "accountId": "$accountId",
  "amount": 25.00,
  "currency": "USD",
  "type": "DEBIT"
}
"@ | Set-Content -Encoding ascii "tools\requests\create-transaction.json"
Get-Content "tools\requests\create-transaction.json"
```

```powershell
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Authorization: Bearer $OPS" `
  -H "X-Correlation-Id: lab4-demo" `
  -H "Content-Type: application/json" `
  --data-binary "@tools\requests\create-transaction.json" `
  http://localhost:8082/api/v1/transactions
```

Need **HTTP:201** `"status":"RECEIVED"`. Copy **your** `transactionId`:

```powershell
$txnId = "TXN-00000000"
Start-Sleep -Seconds 2
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Authorization: Bearer $OPS" `
  http://localhost:8082/api/v1/transactions/$txnId
```

Change `$txnId` to **your** id. GET should become `"status":"SUBMITTED"` (wait and GET again if it is still `RECEIVED`).

Logs (**Terminal 1** or a new terminal):

```powershell
docker logs md287-lab4-account --tail 40
docker logs md287-lab4-transaction --tail 40
```

**Checklist — logs must NOT contain:** `Authorization`, `Bearer`, `eyJ`, request JSON, DB passwords.

**Checklist — logs MAY contain:** `accountId`, `transactionId`, `correlationId=lab4-demo`.

**Expected result:**

| Check | Result |
| --- | --- |
| liveness / readiness | **200** without a JWT |
| `/actuator/info` | `"version":"1.0.0"` (or the Compose tag) |
| `/actuator/metrics` with `$TELLER` | meter names listed |
| create + activate + transaction | **201** / **200** / **201** |
| logs | `correlationId=lab4-demo` |
| logs must **not** contain | `Authorization`, `Bearer eyJ`, request JSON |

**Why this matters:** Operators probe health without a token. Business APIs still require JWT. Correlation is how you follow one customer action across two containers.

---

### Step 5 — Log in, apply manifests, and push images (OpenShift — required)

The outline deploys to **pre-provisioned** projects. You do **not** create a namespace. **Skip** `00-namespace.yaml`.

**Do this:**

1. Replace the Account and Transaction Deployment YAML (probes + resources):

```powershell
cd "$env:USERPROFILE\MD287\labs\day-04\lab4\starter"
@'
apiVersion: apps/v1
kind: Deployment
metadata:
  name: account-service
  labels:
    app: account-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: account-service
  template:
    metadata:
      labels:
        app: account-service
    spec:
      containers:
        - name: account-service
          image: md287/account-service:1.0.0
          imagePullPolicy: Always
          ports:
            - containerPort: 8081
          envFrom:
            - configMapRef:
                name: account-config
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: openshift
            - name: MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE
              value: health,info,metrics
            - name: INFO_APP_VERSION
              value: "1.0.0"
            - name: SPRING_DATASOURCE_USERNAME
              valueFrom:
                secretKeyRef:
                  name: md287-db-secrets
                  key: ACCOUNT_DB_USER
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: md287-db-secrets
                  key: ACCOUNT_DB_PASSWORD
            - name: MD287_JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: md287-db-secrets
                  key: MD287_JWT_SECRET
          resources:
            requests:
              cpu: 100m
              memory: 256Mi
            limits:
              cpu: "1"
              memory: 512Mi
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8081
            failureThreshold: 30
            periodSeconds: 5
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8081
            periodSeconds: 15
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8081
            periodSeconds: 10
          securityContext:
            runAsNonRoot: true
            runAsUser: 100
            allowPrivilegeEscalation: false
---
apiVersion: v1
kind: Service
metadata:
  name: account-service
spec:
  selector:
    app: account-service
  ports:
    - port: 8081
      targetPort: 8081
---
apiVersion: route.openshift.io/v1
kind: Route
metadata:
  name: account-service
spec:
  to:
    kind: Service
    name: account-service
  port:
    targetPort: 8081
  tls:
    termination: edge
    insecureEdgeTerminationPolicy: Redirect
'@ | Set-Content -Encoding ascii "openshift\10-account.yaml"

@'
apiVersion: apps/v1
kind: Deployment
metadata:
  name: transaction-service
  labels:
    app: transaction-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: transaction-service
  template:
    metadata:
      labels:
        app: transaction-service
    spec:
      containers:
        - name: transaction-service
          image: md287/transaction-service:1.0.0
          imagePullPolicy: Always
          ports:
            - containerPort: 8082
          envFrom:
            - configMapRef:
                name: transaction-config
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: openshift
            - name: MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE
              value: health,info,metrics
            - name: INFO_APP_VERSION
              value: "1.0.0"
            - name: SPRING_DATASOURCE_USERNAME
              valueFrom:
                secretKeyRef:
                  name: md287-db-secrets
                  key: TRANSACTION_DB_USER
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: md287-db-secrets
                  key: TRANSACTION_DB_PASSWORD
            - name: MD287_JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: md287-db-secrets
                  key: MD287_JWT_SECRET
          resources:
            requests:
              cpu: 100m
              memory: 256Mi
            limits:
              cpu: "1"
              memory: 512Mi
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8082
            failureThreshold: 30
            periodSeconds: 5
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8082
            periodSeconds: 15
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8082
            periodSeconds: 10
          securityContext:
            runAsNonRoot: true
            runAsUser: 100
            allowPrivilegeEscalation: false
---
apiVersion: v1
kind: Service
metadata:
  name: transaction-service
spec:
  selector:
    app: transaction-service
  ports:
    - port: 8082
      targetPort: 8082
---
apiVersion: route.openshift.io/v1
kind: Route
metadata:
  name: transaction-service
spec:
  to:
    kind: Service
    name: transaction-service
  port:
    targetPort: 8082
  tls:
    termination: edge
    insecureEdgeTerminationPolicy: Redirect
'@ | Set-Content -Encoding ascii "openshift\20-transaction.yaml"
Select-String -Path "openshift\10-account.yaml","openshift\20-transaction.yaml" -Pattern "TODO|readinessProbe|runAsUser"
```

Need `readinessProbe` and `runAsUser: 100`. No `TODO`.

2. Log in. Get **your OpenShift** username (`student01` … `student25`) and the class password from the instructor. This is **not** the Ablaze id `MSMICR26-26` and **not** `student.VLAB`. Do not use `MSMICR26-TD`.

```powershell
oc login https://api.aro-md287.centralus.aroapp.io:6443/
```

At the prompts type your **OpenShift** username (example `student12`) and the class password. Then:

```powershell
oc whoami
oc project md287-student12
oc project -q
```

Change `md287-student12` to **your** number (username `student12` → project `md287-student12`).

**Expected:** `oc whoami` prints `student12` (your number). `oc project -q` prints `md287-student12`. If login fails or the project is Forbidden, **stop** — get the instructor. YAML review alone does not complete this lab.

3. Confirm backing services exist:

```powershell
oc get svc account-db transaction-db kafka
```

**Expected:** all three Services are listed. Kafka is used at **`kafka:19092`** (see ConfigMap).

4. Apply ConfigMap, Secret, Deployments, Services, Routes. **Skip** `00-namespace.yaml`.

```powershell
cd "$env:USERPROFILE\MD287\labs\day-04\lab4\starter"
$PROJECT = oc project -q
oc apply -n $PROJECT -f openshift\01-configmap.yaml
oc apply -n $PROJECT -f openshift\02-secret.yaml
oc apply -n $PROJECT -f openshift\10-account.yaml
oc apply -n $PROJECT -f openshift\20-transaction.yaml
oc -n $PROJECT get deploy,svc,route,cm,secret
```

5. Push the images from Step 2 into this project. **`git pull` first** if `docker push` already failed with **403** / `denied` — Ablaze Docker Desktop cannot store the OpenShift token in Windows Credential Manager. The script uses a throwaway docker login (then skopeo / Python if needed).

```powershell
cd "$env:USERPROFILE\MD287"
git pull
cd "$env:USERPROFILE\MD287\labs\day-04\lab4"
$env:MD287_REGISTRY = "default-route-openshift-image-registry.apps.aro-md287.centralus.aroapp.io"
powershell -File tools\push-images.ps1
```

If `git pull` aborts because `10-account.yaml` / `20-transaction.yaml` have local edits, run this instead, then continue with `cd` / `$env:MD287_REGISTRY` / `push-images.ps1`:

```powershell
cd "$env:USERPROFILE\MD287"
git stash push -m "lab4 yaml" -- labs/day-04/lab4/starter/openshift/10-account.yaml labs/day-04/lab4/starter/openshift/20-transaction.yaml
git pull
git stash pop
```

If `stash pop` reports a conflict, keep **your** filled YAML (probes / `runAsUser: 100`), not the starter TODOs.

Participants have **edit** on their project only, so they cannot always read the Route in `openshift-image-registry`. Setting `$env:MD287_REGISTRY` is the reliable path.

```powershell
$PROJECT = oc project -q
oc -n $PROJECT set image deploy/account-service account-service=image-registry.openshift-image-registry.svc:5000/$PROJECT/account-service:1.0.0
oc -n $PROJECT set image deploy/transaction-service transaction-service=image-registry.openshift-image-registry.svc:5000/$PROJECT/transaction-service:1.0.0
oc -n $PROJECT rollout restart deploy/account-service deploy/transaction-service
oc -n $PROJECT delete pod -l app=account-service --wait=false
oc -n $PROJECT delete pod -l app=transaction-service --wait=false
oc -n $PROJECT rollout status deploy/account-service
oc -n $PROJECT rollout status deploy/transaction-service
oc -n $PROJECT get pods,route
```

You must assign `$PROJECT = oc project -q` in this same paste. Running `oc project -q` alone does not set `$PROJECT`. The push script already `oc set image`s and restarts; this block recovers ImagePullBackOff after a same-tag push.

6. Call the **Account Route** (not localhost):

```powershell
$PROJECT = oc project -q
$ROUTE_HOST = oc -n $PROJECT get route account-service -o jsonpath="{.spec.host}"
curl.exe -sk https://$ROUTE_HOST/actuator/health/readiness
```

Do not use `$HOST` — PowerShell already owns that name (`$Host` is read-only).

Use **`curl.exe -k`** (or `-sk`) only in this lab for the classroom certificate.

**Expected result:** Deployments list probes, requests/limits, `runAsNonRoot: true`. Secret holds `MD287_JWT_SECRET`. ConfigMap holds JDBC and Kafka URLs — not the password. Pods become Ready. Readiness on the Route returns **200**.

**Why this matters:** ConfigMaps are not for passwords. Probes stop sending traffic to a pod that is not ready. The outline requires a real deploy, not a YAML-only review.

---

### Step 6 — Prepared pipeline: SBOM, signature, scan gate

Fill **Exercise 4.3** first: `starter\pipeline\OWNERSHIP.md` (who owns scan / SBOM / sign / promote). Leave it in the repo folder; the instructor will look at it.

Walk the prepared pipeline (this is the required CI/CD evidence). Jenkins is awareness only; do not install it.

```powershell
cd "$env:USERPROFILE\MD287\labs\day-04\lab4"
powershell -File tools\run-pipeline-locally.ps1
```

If the cluster has OpenShift Pipelines, also apply the classroom Pipeline:

```powershell
$PROJECT = oc project -q
oc apply -n $PROJECT -f "$env:USERPROFILE\MD287\labs\day-04\lab4\starter\pipeline\pipeline.yaml"
oc create -n $PROJECT -f "$env:USERPROFILE\MD287\labs\day-04\lab4\starter\pipeline\pipelinerun.yaml"
oc get pipelinerun -n $PROJECT
```

If the Pipelines operator is missing, `oc apply` will error. The **local script still satisfies** the outline’s prepared pipeline (scan / SBOM / sign). Record that in Exercise 4.3.

**Expected result:** the script prints stages including scan PASS, then a **GATE FAIL** on the CRITICAL sample, then SBOM and signature. That fail is **success** for the gate. Confirm `tools\sample-sbom-account-service.json` lists `spring-boot-starter-web` **3.4.5** and Temurin 21.

**Why this matters:** A green deploy with a CRITICAL CVE is not a success. An SBOM is how you answer “what did we actually ship?” after a new CVE drops on Friday.

---

### Step 7 — Deploy a new version and roll it back (OpenShift — required)

You will change **only the visible version** (`INFO_APP_VERSION`). You are practicing the **mechanic**, not rewriting Java.

```powershell
$PROJECT = oc project -q
oc -n $PROJECT set env deploy/account-service INFO_APP_VERSION=1.0.1
oc -n $PROJECT rollout status deploy/account-service
$ROUTE_HOST = oc -n $PROJECT get route account-service -o jsonpath="{.spec.host}"
curl.exe -sk https://$ROUTE_HOST/actuator/info
oc -n $PROJECT rollout undo deploy/account-service
oc -n $PROJECT rollout status deploy/account-service
oc -n $PROJECT rollout history deploy/account-service
curl.exe -sk https://$ROUTE_HOST/actuator/info
```

**Expected:** first `/actuator/info` shows `"version":"1.0.1"`. After undo, history has more than one revision and `/actuator/info` returns `"version":"1.0.0"`.

**Why this matters:** Banks need a rehearsed rollback. “Redeploy yesterday’s tag” is faster than debugging a bad Friday release in production.

---

## Success criteria

- [ ] Account and Transaction images build from the Lab 3 **starter**
- [ ] Container process is **not** root (`docker run --rm --entrypoint id ...`)
- [ ] Compose stack: liveness, readiness, `/actuator/metrics`, `/actuator/info`
- [ ] JWT still required on business APIs; health stays public
- [ ] Correlation id appears in logs; no `Authorization` text
- [ ] `oc whoami` is `studentNN`; manifests applied in `md287-studentNN` (not `00-namespace.yaml`)
- [ ] Images pushed; pods Ready; Account **Route** readiness **200**
- [ ] Pipeline: CRITICAL scan fails the gate; SBOM reviewed; signature command reviewed
- [ ] `oc rollout undo` restored `1.0.0` (history shows more than one revision)
- [ ] Ownership map for Exercise 4.3 filled

---

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| `Terminate batch job (Y/N)?` | Type **`Y`** and Enter. |
| Port already allocated | Step 1 `docker compose down` in Lab 1–3 folders; `docker ps` |
| Image build COPY fails | Build **context** must be the Lab 3 **starter** module. `-f` is the Lab 4 Containerfile. |
| `id` still shows root | `USER md287` missing; rebuild `docker build --no-cache ...` |
| Readiness never 200 | Postgres/Kafka not healthy; `docker compose ps` and `docker logs md287-lab4-account` |
| Transaction stays RECEIVED | Wait 2s and GET `$txnId` again; `kafka-init` must complete |
| `ACCOUNT_NOT_FOUND` for `ACC-YOUR-ID` / `TRANSACTION_NOT_FOUND` for `TXN-YOUR-ID` | Placeholders. Use the id from your **201** body. |
| 401 with a token | Re-run `issue-jwt.py` into `$TELLER` / `$OPS`. Use `curl.exe`. Same secret as Lab 3. |
| 503 on POST transaction | Account container not ready — Lab 3 safe fallback, **do not** fake ACTIVE |
| `oc whoami` failed | Required. Use OpenShift `studentNN`, not Ablaze `MSMICR26-NN`. Get login from [LAB-ACCESS.md](../../../LAB-ACCESS.md). |
| `oc apply` Unauthorized / Forbidden | Wrong project. `oc project md287-studentNN` (same number as `oc whoami`). |
| `oc login` with `student.VLAB` or `MSMICR26-26` | Those are Windows / Ablaze ids. OpenShift is `student01`–`student25`. |
| ImagePullBackOff | `git pull`, re-run `tools\push-images.ps1`, then **`$PROJECT = oc project -q`** (assignment required) and `oc rollout restart` / `oc delete pod -l app=account-service`. Same tag `1.0.0` does not create a new rollout by itself. AGE of 8d/18h means those pods never picked up the new push. |
| Python `HTTP 307 Temporary Redirect` on `/blobs/sha256:...` | Old pusher treated storage redirects as failure. `git pull` and re-run `push-images.ps1`. |
| `docker-daemon:... is not a valid image reference` | Harmless; the script falls through to Python. `git pull` picks up the `docker-daemon://` fix. |
| `docker push` **403** / `denied` / `unauthorized` | `git pull`, then re-run `tools\push-images.ps1`. Do **not** `docker login` by hand (Windows Credential Manager truncates the token). Expected last line includes `Pushed ... (docker)` / `(skopeo)` / `(oc image mirror)` / `(python)`. |
| Python `HTTP Error 400: Authentication information is not given` | Old pusher. `cd $env:USERPROFILE\MD287`; `git pull`; re-run `tools\push-images.ps1`. `oc whoami` must be `studentNN`, not `student.VLAB`. |
| HTML **Application is not available** on the Account/Transaction Route | Pods are not Ready yet (usually ImagePullBackOff because the push has not succeeded). Fix the push, then wait for Ready. That page is **not** the registry. |
| `x509` / certificate error on push | The script falls through to Python (`push_image.py`) which skips TLS verify. Wait for that attempt. |
| Pod `CreateContainerConfigError` / `non-numeric user` | Keep `runAsNonRoot: true` and `runAsUser: 100` (the uid `docker run --entrypoint id` printed). |
| Registry Route missing | `$env:MD287_REGISTRY = "default-route-openshift-image-registry.apps.aro-md287.centralus.aroapp.io"` then re-run `push-images.ps1` |
| Pipelines CRDs missing | Use `tools\run-pipeline-locally.ps1` (that is the prepared pipeline) |
| PSReadLine crash / huge paste | Copy **one** command block only. |
| `git pull`: not a git repository | `cd $env:USERPROFILE\MD287` then `git pull`. |
| `git pull`: local changes would be overwritten (`10-account.yaml` / `20-transaction.yaml`) | Those files are **your** Step 4 YAML. From `%USERPROFILE%\MD287`: `git stash push -m "lab4 yaml" -- labs/day-04/lab4/starter/openshift/10-account.yaml labs/day-04/lab4/starter/openshift/20-transaction.yaml` then `git pull` then `git stash pop`. If pop conflicts, keep your filled YAML. Do **not** `git checkout --` those files. |

---

## Clean shutdown

Leave the OpenShift Deployments running unless the instructor says otherwise. On the VM you may stop Compose:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-04\lab4\starter"
docker compose down
```

Do **not** delete your OpenShift project.

---

## Optional stretch (only if you finished early)

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

---

© 2026 Innovation In Software Corporation
