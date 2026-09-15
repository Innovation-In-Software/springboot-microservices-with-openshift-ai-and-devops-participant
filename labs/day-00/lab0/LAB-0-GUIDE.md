# Lab 0 — Log in to the VM and set up the workstation

**Day:** 1 morning — before Module 1  
**Capstone:** None yet. This lab gets you onto the classroom VM and proves it can run Labs 1–5.  
**Time:** 20–30 minutes  
**Difficulty:** Beginner

**Objective:** Sign in to **your assigned TEKsystems Ablaze virtual machine**, then do **all** setup **on that VM**: clone the course repo, sign in to **GitHub Copilot Free** in VS Code, and prove Java 21, Maven, Docker, Git, Python, `oc`, and `curl.exe` work **before** you write any Account Service code.

Do **not** run Labs 1–5 on your personal laptop. The gold image (JDK, Maven, Docker, `oc`) is on the VM.

> **Start here (even before you have this file):** find your name in [LAB-ACCESS.md](../../../LAB-ACCESS.md) → open **https://my.ablazedesktop.com** → wait for the Windows desktop → open VS Code **inside that desktop**. Then continue from Step 1 below. Your laptop is only the browser.

---

## What you will finish with

By the end of this lab you will have:

- A browser session at **https://my.ablazedesktop.com** showing **your** Windows VM desktop (not a classmate’s)
- **GitHub Copilot Free** signed in inside VS Code on that VM (every participant VM and the instructor VM already have Copilot)
- The course repo cloned to the **short folder** `%USERPROFILE%\MD287` (for example `C:\Users\student.VLAB\MD287`)
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
| **Workstation, not your laptop** | You connect from a browser. All `java` / `mvn` / `docker` commands run **inside** the Ablaze Windows VM. |
| **One login per person** | Username looks like `MSMICR26-01`. Sharing a seat kicks the other person off. |
| **`curl.exe` vs `curl`** | PowerShell aliases `curl` to `Invoke-WebRequest`. Always type **`curl.exe`**. |
| **Java 21 first on PATH** | `java -version` must print **21**. A second JDK 17 first on PATH breaks every lab. |
| **Docker engine vs Docker Desktop** | The whale icon idle means the engine is up. `docker info` must print a **Server Version**. |
| **Public clone, no GitHub login** | You clone the course repo yourself into the **short folder** `MD287`. Do not copy labs from a USB stick as a substitute. Clone still needs **no** GitHub password. |
| **GitHub Copilot Free** | Every student VM and the instructor VM already have Copilot via a **free** GitHub Copilot account. Sign in once in VS Code on this VM. That sign-in is for the editor only — it is not required for `git clone`. **Review before accept** on every later lab. |
| **Work in `starter/`** | Each later lab has a `starter/` folder. This participant pack has no `solution/`. |
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
| GitHub Copilot | **Copilot Free** in VS Code — already available on this VM |

OpenShift **login**, the ARO project, and the model Route are **not** part of Lab 0. The instructor issues those before Lab 4.

---

## Environment basics (read this first)

**Where you type:** after Step 1, everything is **on the Ablaze VM** (Windows 10/11 · PowerShell in VS Code). Your laptop is only the browser that shows that desktop.

| Task | How |
| --- | --- |
| Open the VM | Browser → **https://my.ablazedesktop.com** |
| Username | Your Ablaze login from [LAB-ACCESS.md](../../../LAB-ACCESS.md) (pattern `MSMICR26-` plus two digits) |
| Password | The class password in [LAB-ACCESS.md](../../../LAB-ACCESS.md) (same for every seat) |
| After desktop loads | Use **Start → Visual Studio Code** on the VM (not an editor on your laptop) |
| GitHub Copilot | Sign in on this VM with the GitHub account that has **Copilot Free**. Every participant VM and the instructor VM already have it. |
| Terminal | On the VM: Ctrl+` → PowerShell |
| Clone folder | On the VM: **`%USERPROFILE%\MD287`** (short name). Later steps run from there. If VS Code cloned the long repo name, rename it to `MD287` (Step 4). |
| HTTP calls | Use **`curl.exe`**, not `curl` |
| Escalate VM / Docker / Java | Lab assistant → **TEKsystems** |
| Escalate OpenShift | Not today |

**Do not:**

- Install another JDK, Maven, or Docker beside the gold image
- Fork the course repo, or type a GitHub password for `git clone` (the clone URL is public)
- Leave the clone under the long GitHub repo name — rename it to **`MD287`** so later labs match the guide
- Skip Copilot sign-in in VS Code — you already have **Copilot Free** on this VM
- Run `oc login`
- Copy files from a `solution/` folder (this pack does not include one)
- Start coding Account Service (that is Lab 1)

---

## Steps from the training slides

Follow these steps in order. Finish one step before starting the next.

### Step 1 — Log in to your Ablaze virtual machine

You can do this step from the classroom laptop, a loaner, or any browser. You do **not** need the Git repo yet.

**Do this:**

1. Find **your name**, Ablaze username, and password in [LAB-ACCESS.md](../../../LAB-ACCESS.md). Do not use `MSMICR26-TD` (trainer account) and do not borrow a neighbour’s login.
2. Open a browser and go to **https://my.ablazedesktop.com**
3. Sign in with that username and password.
4. Wait until the **Windows desktop** appears (icons, taskbar, Start). First connect after class start can take a minute. If the page says access is not yet scheduled, wait for the TEKsystems window and retry.
5. Confirm you can click **Start** on that desktop. You are now **on the VM**. Leave this browser tab open all week.

**Expected result:**

- The browser shows a full Windows desktop (Ablaze / Spark View), not only a login form
- You are the only person on that username (nobody else is being kicked off)
- You have **not** installed software on your personal laptop for this course

**Why this matters:** Java, Maven, Docker Desktop, and `oc` are already on this image. Setup on a laptop will not match Lab 1–5.

---

### Step 2 — On the VM, open VS Code and PowerShell

**Do this:**

1. **Inside the Ablaze desktop** (not on your local PC), open **Visual Studio Code** from the Start menu. Cursor is not required.
2. Press **Ctrl+`** for a terminal. If the shell is not PowerShell, click the `+` dropdown and choose **Windows PowerShell**.
3. Sign in to **GitHub Copilot** in VS Code. Every student VM and the instructor VM already have Copilot via a **Copilot Free** account.
   - Open the Command Palette (`Ctrl+Shift+P`) and run **GitHub Copilot: Sign In**, or click the Copilot icon in the status bar.
   - Sign in with the GitHub account that already has Copilot Free. Do not create a new GitHub account during this lab unless the instructor says the Free plan is not yet on your account.
   - Confirm the status bar shows Copilot ready (not “Sign in” or inactive).
   - This sign-in is **only** for the editor. Step 4 `git clone` still needs no GitHub password. Do **not** fork the course repo.

Confirm the prompt is a VM profile, for example `PS C:\Users\student>` or `PS C:\Users\student.VLAB>`.

**Expected result:**

- VS Code is running **on the virtual machine**
- You can type in PowerShell (the prompt is not frozen)
- GitHub Copilot is signed in on this VM (Copilot Free)

**Why this matters:** If you open VS Code on your laptop instead of the VM, `java` and `docker` will not be the classroom toolchain. Copilot on this VM is how you will explain and draft later lab code — still **review before accept**.

---

### Step 3 — Verify the toolchain on the VM

**Do this:**

In that **VM** PowerShell window, run each command. Read the output before going on.

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

### Step 4 — Clone the course repo on the VM

Use the **short folder name `MD287`**. Do not keep the long GitHub repo name as the folder.

**Do this (preferred — PowerShell):**

Still in **VM** PowerShell (not a terminal on your laptop):

```powershell
cd $env:USERPROFILE
git clone https://github.com/Innovation-In-Software/springboot-microservices-with-openshift-ai-and-devops-participant.git MD287
cd MD287
```

The last argument (`MD287`) is the folder name. That is what makes the path short: `%USERPROFILE%\MD287`.

If `MD287` already exists from a previous attempt:

```powershell
cd $env:USERPROFILE\MD287
git status
git pull
```

**If VS Code already cloned the long name:** the folder is often `%USERPROFILE%\.vscode\springboot-microservices-with-openshift-ai-and-devops-participant`. Rename it once, then reopen it. Do **not** clone a second copy. Full copy-paste: **[Tip — short folder `MD287`](../../TIP-SHORT-FOLDER.md)**.

```powershell
$long = "$env:USERPROFILE\.vscode\springboot-microservices-with-openshift-ai-and-devops-participant"
$short = "$env:USERPROFILE\MD287"
if ((Test-Path $long) -and -not (Test-Path $short)) {
  Move-Item $long $short
}
cd $env:USERPROFILE\MD287
```

Then in VS Code: **File → Open Folder** → `%USERPROFILE%\MD287`.

**If you clone with VS Code instead of PowerShell:**

1. **Ctrl+Shift+P** → **Git: Clone**.
2. Paste the same GitHub URL (or the **participant** URL the instructor issued).
3. Choose **`%USERPROFILE%`** as the parent folder (**not** `.vscode`).
4. After the clone finishes, rename the long folder to `MD287` with the `Move-Item` commands above, then **File → Open Folder** → `%USERPROFILE%\MD287`.

Confirm you see `README.md` and `labs\`:

```powershell
cd $env:USERPROFILE\MD287
Get-ChildItem
```

**Expected result:**

- Clone finishes without asking for a GitHub username or password (Copilot sign-in in VS Code is separate and does not change this)
- `labs\day-00\lab0\LAB-0-GUIDE.md` exists (this file)
- `labs\day-01\lab1\starter\account-service\pom.xml` exists
- VS Code is rooted at **`MD287`** (the prompt ends with `\MD287>`)

If `git clone` hangs or fails with a proxy / SSL error, raise a hand (network / TEKsystems). Do **not** download a ZIP from a personal laptop as a workaround unless the instructor says so.

**Why this matters:** Labs live in this repo. The short folder `MD287` is what every later `cd` command uses. Pre-copying files onto the VM hides clone problems that every later `git pull` would hit anyway.

---

### Step 5 — Start Docker Desktop on the VM

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

### Step 6 — Run the environment check script

**Do this:**

From the **repo root** `%USERPROFILE%\MD287`:

```powershell
cd $env:USERPROFILE\MD287
powershell -ExecutionPolicy Bypass -File "labs\day-00\lab0\tools\check-environment.ps1"
```

Read the table it prints. Fix any **FAIL** with the troubleshooting section below, then re-run the script.

**Expected result:**

- The script prints **LAB 0 ENVIRONMENT: PASS**
- Java 21, Maven-on-21, Git, Python 3.12+, `oc` client, `curl.exe`, and Docker engine are all green
- Host ports **8081** and **5433** are free (or already used by `md287-account-db` from a retry — that is OK)

If the script is missing, you are not in the cloned repo. Go back to Step 4 and `cd $env:USERPROFILE\MD287`.

**Why this matters:** The same checks are what lab assistants look at on the floor. A screenshot of PASS is enough evidence that your VM is ready.

---

### Step 7 — Start Lab 1 Postgres

**Do this:**

```powershell
cd "$env:USERPROFILE\MD287\labs\day-01\lab1\starter\account-service"
docker compose up -d
docker compose ps
```

Wait until `STATUS` includes **`(healthy)`**. Right after `up -d` it often says `(health: starting)` — wait about 10 seconds and run `docker compose ps` again.

**Expected result:**

- Container `md287-account-db` is **Up** and **(healthy)**
- Host port **5433** is mapped to container **5432**
- You did **not** run `mvn spring-boot:run` yet (that is Lab 1)

Leave this database running. Lab 1 Step 1 will reuse it.

If port **5433** is already allocated, run `docker ps` and stop the other container, or `docker compose down` in an older copy of this folder.

**Why this matters:** Account Service talks only to this database. If Compose cannot start Postgres, Lab 1 cannot start.

---

### Step 8 — Warm the Maven cache

**Do this:**

Still in `labs\day-01\lab1\starter\account-service` under `%USERPROFILE%\MD287`:

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

- [ ] Browser is on **https://my.ablazedesktop.com** and shows **your** Windows VM desktop
- [ ] VS Code + PowerShell are open **on that VM** (not on your laptop)
- [ ] GitHub Copilot is signed in on this VM (Copilot Free)
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
| Cannot sign in / “access not scheduled” | Wait for the TEKsystems lab window. Confirm your username and password in [LAB-ACCESS.md](../../../LAB-ACCESS.md). |
| Login works then you are kicked off | Someone else is using the same username. One person per seat. |
| Desktop never appears / black screen | Lab assistant → **TEKsystems**. Do not share someone else’s VM. |
| You ran `java` on your laptop | Close that window. Type only **inside** the Ablaze desktop. |
| VS Code missing / frozen desktop | TEKsystems image issue. Do not install Cursor on the VM unless the instructor says so. |
| Copilot asks you to sign in / shows inactive | Use **GitHub Copilot: Sign In** in VS Code on the VM with the GitHub account that has **Copilot Free**. Every student VM and the instructor VM already have Copilot. Raise a hand if the extension is missing. |
| `java -version` shows 17 | JDK 21 is not first on PATH. TEKsystems — do not install a second JDK yourself. |
| `mvn` not recognized | Maven is not on PATH. TEKsystems gold image. |
| `python` opens the Microsoft Store | Store stub. Need Python 3.12+ on PATH (`C:\Python312` or similar), not `WindowsApps`. |
| `git clone` asks for a password | Wrong URL, or a proxy intercepting GitHub. Public clone needs **no** login. |
| `git clone` fails / SSL error | Outbound HTTPS to `github.com` — TEKsystems network. |
| Terminal is in `...\.vscode\...-participant` | Rename that folder to `%USERPROFILE%\MD287` (Lab 0 Step 4 `Move-Item`), then **File → Open Folder** → `MD287`. |
| `check-environment.ps1` missing | You are not at repo root. `cd $env:USERPROFILE\MD287` so `labs\` is a child of the current directory. |
| `docker info` → `error during connect` | Start Docker Desktop; wait until the whale is idle; retry. Firewall must allow **Docker Desktop Backend**. |
| `docker compose` not found | Need Compose **v2** (`docker compose`, two words). TEKsystems. |
| Port 5433 already in use | `docker ps`; `docker compose down` in the other folder that bound 5433. |
| Compose stays `(health: starting)` | Wait 10s and `docker compose ps` again. If it loops, `docker compose logs`. |
| Maven `Could not transfer artifact` | Maven Central blocked — TEKsystems network / proxy. |
| `oc` not recognized | Client missing; needed on Days 4–5. Report it now even though you will not log in today. |
| `curl` prints PowerShell XML / errors | You typed `curl`. Use **`curl.exe`**. |
| Script execution disabled | Use `powershell -ExecutionPolicy Bypass -File ...` as in Step 6. |

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
