# Lab 0 — Environment Setup

**Day:** 1 morning — before Module 1  
**Capstone:** None yet. This lab proves the workstation can run Labs 1–5.  
**Time:** 20–30 minutes  
**Difficulty:** Beginner

**Objective:** Log on to the TEKsystems Ablaze VM, clone the public course repo, and prove Java 21, Maven, Docker, Git, Python, `oc`, and `curl.exe` work **before** you write any Account Service code.

---

## What you will finish with

By the end of this lab you will have:

- A working logon to the Ablaze Desktop VM
- The course repo cloned to `%USERPROFILE%\MD287`
- Java **21** first on PATH, and Maven using that JDK
- Docker Desktop **engine** running (not only installed)
- Git, Python 3.12+, OpenShift CLI `oc`, and **`curl.exe`** verified
- Lab 1 PostgreSQL healthy on host port **5433**
- Maven dependencies for Account Service already downloaded
- A PASS report from `tools\check-environment.ps1`

You will **not** start Account Service, change Java, or log in to OpenShift. Those are Labs 1 and 4.

---

## Knowledge you need (before Day 1 teaching)

Read this once. Each idea shows up in a later step.

| Idea | How it appears in this lab |
| --- | --- |
| **Workstation, not your laptop** | Labs run on the TEKsystems Ablaze VM. Do not re-image it or install a parallel JDK. |
| **`curl.exe` vs `curl`** | PowerShell aliases `curl` to `Invoke-WebRequest`. Always type **`curl.exe`**. |
| **Java 21 first on PATH** | `java -version` must print **21**. A second JDK 17 first on PATH breaks every lab. |
| **Docker engine vs Docker Desktop** | The whale icon idle means the engine is up. `docker info` must print a **Server Version**. |
| **Public clone, no GitHub login** | You clone the course repo yourself. Do not copy labs from a USB stick as a substitute. |
| **`starter/` vs `solution/`** | You work in each lab `starter/`. `solution/` is instructor reference. |
| **One Compose stack at a time** | Account Postgres uses host **5433**. If that port is busy, stop the other container first. |
| **OpenShift is later** | `oc version --client` today. `oc login` is **Day 4**. Do not invent a cluster URL. |
| **Python Store stub** | Days 3–5 need real Python 3.12+ for `issue-jwt.py`, not the Microsoft Store placeholder. |

### What this VM must provide

| Check | Pass |
| --- | --- |
| OS | Windows 10/11, 64-bit · PowerShell in VS Code |
| JDK | **21** first on PATH |
| Maven | 3.9.x using Java 21 |
| Git | 2.40+ |
| Docker Desktop | Engine running · Compose v2 |
| Python | 3.12+ (not `WindowsApps` Store stub) |
| OpenShift CLI | `oc` 4.x client |
| HTTP | **`curl.exe`** |

OpenShift **login**, the ARO project, and the model Route are **not** part of Lab 0. The instructor issues those before Lab 4.

---

## Environment basics (read this first)

**Demonstration environment:** Windows 10/11 · PowerShell in VS Code (press Ctrl+` to open the terminal)

| Task | How |
| --- | --- |
| Log on | Ablaze Desktop with the classroom username the lab assistant issued |
| Editor | VS Code (already on the VM). Cursor is **not** required. |
| Terminal | Ctrl+` → PowerShell |
| Clone folder | `%USERPROFILE%\MD287` (for example `C:\Users\student\MD287`) |
| HTTP calls | Use **`curl.exe`**, not `curl` |
| Escalate VM / Docker / Java | Lab assistant → **TEKsystems** |
| Escalate OpenShift | Not today |

**Do not:**

- Install another JDK, Maven, or Docker beside the gold image
- Create a GitHub account or fork the repo
- Run `oc login`
- Copy files from `solution/`
- Start coding Account Service (that is Lab 1)

---

## Steps from the training slides

Follow these steps in order. Finish one step before starting the next.

### Step 1 — Log on and open PowerShell

**Do this:**

1. Sign in to **Ablaze Desktop** with the credentials the lab assistant gave you.
2. Wait until the Windows desktop is usable (icons load; you can open the Start menu).
3. Open **Visual Studio Code**.
4. Open a terminal: press **Ctrl+`**. If the shell is not PowerShell, click the `+` dropdown and choose **Windows PowerShell**.

Confirm the prompt looks like a normal user profile, for example `PS C:\Users\student>`.

**Expected result:**

- You are on the Windows desktop of the classroom VM
- VS Code is open with a PowerShell terminal
- You can type commands (the prompt is not frozen)

**Why this matters:** Every lab this week runs on this VM. If logon fails, stop and raise a hand — that is a TEKsystems ticket, not a Java problem.

---

### Step 2 — Verify the toolchain

**Do this:**

In the same PowerShell window, run each command. Read the output before going on.

```powershell
java -version
mvn -version
git --version
python --version
oc version --client
curl.exe --version
```

**Expected result:**

| Command | You should see |
| --- | --- |
| `java -version` | `openjdk version "21...` (21 is enough; patch level may differ) |
| `mvn -version` | Apache Maven **3.9.x** and **Java version: 21** |
| `git --version` | `git version 2.` plus a number 40 or higher |
| `python --version` | `Python 3.12` or `3.13` — **not** a Microsoft Store window |
| `oc version --client` | Client Version **4.**x |
| `curl.exe --version` | `curl` from Windows, not a PowerShell error |

If `python` opens the Microsoft Store, or `Get-Command python` shows a path under `WindowsApps`, raise a hand. Days 3–5 need a real interpreter.

If `java` prints **17** (or anything other than 21), raise a hand. Do **not** install another JDK yourself.

**Why this matters:** Lab 1 will not start if Maven is bound to JDK 17. Finding that now saves an hour after lunch.

---

### Step 3 — Clone the public course repo

**Do this:**

```powershell
cd $env:USERPROFILE
git clone https://github.com/Innovation-In-Software/springboot-microservices-with-openshift-ai-and-devops-participant.git MD287
cd MD287
```

If `MD287` already exists from a previous attempt:

```powershell
cd $env:USERPROFILE\MD287
git status
git pull
```

Confirm you see `README.md`, `labs\`, and `curriculum\`:

```powershell
Get-ChildItem
```

In VS Code: **File → Open Folder** → `%USERPROFILE%\MD287`.

**Expected result:**

- Clone finishes without asking for a GitHub username or password
- `labs\day-00\lab0\LAB-0-GUIDE.md` exists (this file)
- `labs\day-01\lab1\starter\account-service\pom.xml` exists
- VS Code is rooted at the `MD287` folder

If `git clone` hangs or fails with a proxy / SSL error, raise a hand (network / TEKsystems). Do **not** download a ZIP from a personal laptop as a workaround unless the instructor says so.

**Why this matters:** Labs live in this repo. Pre-copying files onto the VM hides clone problems that every later `git pull` would hit anyway.

---

### Step 4 — Start Docker Desktop

**Do this:**

1. Open **Docker Desktop** from the Start menu if it is not already running.
2. Wait until the whale icon in the system tray is **idle** (not animating). First start after logon can take up to two minutes.
3. Then run:

```powershell
docker info
docker compose version
```

**Expected result:**

- `docker info` prints a **Server Version** (for example `28.x` or similar)
- You do **not** see `error during connect` or `The system cannot find the file specified` for `docker_engine`
- `docker compose version` prints Compose **v2** (v2.x or a Docker Compose version 5.x that still understands `docker compose`)

If the engine is down, wait and retry. Do not start Step 6 until this passes.

**Why this matters:** Compose is how every lab starts PostgreSQL (and later Kafka). An installed Docker Desktop with a stopped engine looks fine until the first `up -d` fails.

---

### Step 5 — Run the environment check script

**Do this:**

From the repo root (`MD287`):

```powershell
cd $env:USERPROFILE\MD287
powershell -ExecutionPolicy Bypass -File "labs\day-00\lab0\tools\check-environment.ps1"
```

Read the table it prints. Fix any **FAIL** with the troubleshooting section below, then re-run the script.

**Expected result:**

- The script prints **LAB 0 ENVIRONMENT: PASS**
- Java 21, Maven-on-21, Git, Python 3.12+, `oc` client, `curl.exe`, and Docker engine are all green
- Host ports **8081** and **5433** are free (or already used by `md287-account-db` from a retry — that is OK)

If the script is missing, you are not in the cloned repo. Go back to Step 3.

**Why this matters:** The same checks are what lab assistants look at on the floor. A screenshot of PASS is enough evidence that your VM is ready.

---

### Step 6 — Smoke-test Docker Compose

**Do this:**

```powershell
cd "$env:USERPROFILE\MD287\labs\day-01\lab1\starter\account-service"
docker compose up -d
docker compose ps
```

Wait until `STATUS` includes **`(healthy)`**. Right after `up -d` it often says `(health: starting)` — wait about 10 seconds and run `docker compose ps` again.

Optional — confirm the image is the classroom Postgres:

```powershell
docker compose images
```

**Expected result:**

- Container `md287-account-db` is **Up** and **(healthy)**
- Host port **5433** is mapped to container **5432**
- You did **not** run `mvn spring-boot:run` yet (that is Lab 1)

Leave this database running. Lab 1 Step 1 will reuse it.

If port **5433** is already allocated, run `docker ps` and stop the other container, or `docker compose down` in an older copy of this folder.

**Why this matters:** Account Service talks only to this database. If Compose cannot start Postgres, Lab 1 cannot start.

---

### Step 7 — Warm the Maven cache

**Do this:**

Still in `labs\day-01\lab1\starter\account-service`:

```powershell
mvn -q -DskipTests dependency:go-offline
```

The **first** run on a new VM may take several minutes while Maven downloads Spring Boot 3.4 libraries from Maven Central. Stay on this step until it finishes.

**Expected result:**

- The command returns to the prompt with no `BUILD FAILURE`
- Later `mvn spring-boot:run` in Lab 1 starts in seconds rather than 10+ minutes

If this hangs or fails on `Could not transfer artifact`, raise a hand (outbound HTTPS to Maven Central).

**Why this matters:** The first Maven download during the afternoon lab is the most common “my app will not start” ticket. Doing it now is the whole point of Lab 0.

---

## Success criteria

- [ ] Ablaze VM logon works; VS Code PowerShell is open
- [ ] `java -version` prints **21**
- [ ] `mvn -version` uses Java **21**
- [ ] `git --version` works
- [ ] `python --version` is **3.12+** (not the Microsoft Store stub)
- [ ] `oc version --client` prints 4.x
- [ ] `curl.exe --version` works
- [ ] Repo cloned to `%USERPROFILE%\MD287` with `labs\` present
- [ ] `docker info` prints a **Server Version**
- [ ] `check-environment.ps1` prints **PASS**
- [ ] `md287-account-db` is **(healthy)** on host **5433**
- [ ] `mvn -DskipTests dependency:go-offline` completed without error
- [ ] You did **not** start Account Service and did **not** run `oc login`

---

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| Cannot sign in to Ablaze | Lab assistant → **TEKsystems**. Do not share someone else’s VM. |
| VS Code missing / frozen desktop | TEKsystems image issue. Do not install Cursor as a workaround unless the instructor says so. |
| `java -version` shows 17 | JDK 21 is not first on PATH. TEKsystems — do not install a second JDK yourself. |
| `mvn` not recognized | Maven is not on PATH. TEKsystems gold image. |
| `python` opens the Microsoft Store | Store stub. Need Python 3.12+ on PATH (`C:\Python312` or similar), not `WindowsApps`. |
| `git clone` asks for a password | Wrong URL, or a proxy intercepting GitHub. Public clone needs **no** login. |
| `git clone` fails / SSL error | Outbound HTTPS to `github.com` — TEKsystems network. |
| `docker info` → `error during connect` | Start Docker Desktop; wait until the whale is idle; retry. Firewall must allow **Docker Desktop Backend**. |
| `docker compose` not found | Need Compose **v2** (`docker compose`, two words). TEKsystems. |
| Port 5433 already in use | `docker ps`; `docker compose down` in the other folder that bound 5433. |
| Compose stays `(health: starting)` | Wait 10s and `docker compose ps` again. If it loops, `docker compose logs`. |
| Maven `Could not transfer artifact` | Maven Central blocked — TEKsystems network / proxy. |
| `oc` not recognized | Client missing; needed on Days 4–5. Report it now even though you will not log in today. |
| `curl` prints PowerShell XML / errors | You typed `curl`. Use **`curl.exe`**. |
| Script execution disabled | Use `powershell -ExecutionPolicy Bypass -File ...` as in Step 5. |

---

## Clean shutdown

**Leave Postgres running** if you are going straight into Module 1 and then Lab 1.

If you are stopping for the day before Lab 1:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-01\lab1\starter\account-service"
docker compose down
```

Do **not** add `-v` unless the instructor says to wipe the volume.

---

## Optional stretch (only if you finished early)

Pull the other classroom images so Days 2–5 are faster. This can take a while on classroom Wi-Fi — skip it if the instructor is starting Module 1.

```powershell
docker pull postgres:16-alpine
docker pull apache/kafka:3.8.1
docker pull python:3.12-alpine
docker pull maven:3.9.9-eclipse-temurin-21-alpine
docker pull eclipse-temurin:21-jre-alpine
```

Do **not** build `md287/account-service` images. Do **not** `oc login`. Do **not** open Lab 1 Java files to start coding.

---

## What you built in the capstone

```text
Lab 0  Workstation ready     ← you are here
Day 1  Account Service
Day 2  + Transaction Service (REST validate + Kafka events)
Day 3  + JWT, Resilience4j, tests
Day 4  + containers, OpenShift, CI/CD
Day 5  + Risk Assessment Service and OpenShift AI
```

Lab 0 does not add a microservice. It removes the setup failures that would otherwise eat Lab 1.

---

© 2026 Innovation In Software Corporation
