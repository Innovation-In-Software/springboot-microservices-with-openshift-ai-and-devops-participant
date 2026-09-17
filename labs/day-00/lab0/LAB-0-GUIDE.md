# Lab 0 — Log in to the VM and set up the workstation

**Day:** 1 morning — before Module 1  
**Time:** 20–30 minutes

Do **all** of this **on the Ablaze VM**. Your laptop is only the browser.

Do **not** run `mvn spring-boot:run`. Do **not** run `oc login`.

Work in **`%USERPROFILE%\MD287`**. Example: `C:\Users\student.VLAB\MD287`.

Copy **one block at a time**. If a command says the folder already exists, skip it and go to the next step.

---

### Step 1 — Log in to Ablaze

1. Find **your name**, Ablaze username, and password in [LAB-ACCESS.md](../../../LAB-ACCESS.md). Do not use `MSMICR26-TD`.
2. Open **https://my.ablazedesktop.com** and sign in.
3. Wait for the **Windows desktop**. Leave this tab open all week.

---

### Step 2 — VS Code and Copilot on the VM

1. **Start → Visual Studio Code** (on the VM, not the laptop).
2. **Ctrl+`** → **Windows PowerShell**.
3. **Ctrl+Shift+P** → **GitHub Copilot: Sign In**.

The prompt may show the long `.vscode\...-participant` path. That is the same repo. Always `cd` to `MD287` before git commands.

---

### Step 3 — Toolchain

```powershell
java -version
mvn -version
git --version
python --version
oc version --client
curl.exe --version
```

| Command | Pass on this VM |
| --- | --- |
| `java -version` | `openjdk version "21"` |
| `mvn -version` | Maven 3.9.x and **Java version: 21** |
| `git --version` | 2.40 or newer |
| `python --version` | 3.12 or newer (3.14 is OK). Not the Microsoft Store. |
| `oc version --client` | `Client Version: 4.` |
| `curl.exe --version` | Windows curl. Always type **`curl.exe`**. |

If `java` is 17, or `python` opens the Store, raise a hand. Do not install software yourself.

---

### Step 4 — Open `MD287` and pull

Most seats already have the folder (VS Code clone plus a short name). Run **only** this:

```powershell
cd $env:USERPROFILE\MD287
git pull
Get-ChildItem
```

**Expected:** prompt ends with `\MD287>`, `git pull` prints `Already up to date.` or a Fast-forward, and you see folders `labs` and `slides`.

Then: **File → Open Folder** → `%USERPROFILE%\MD287`.

Do **not** run `git clone`. Do **not** run `mklink`. Those fail when `MD287` already exists (`destination path 'MD287' already exists` / `Cannot create a file when that file already exists`). That is normal. Stay in `MD287`.

`git pull` only works **inside** the repo. Do not run it from `C:\Users\student.VLAB`.

**Only if** `cd $env:USERPROFILE\MD287` says the path does not exist, use [First-time folder](#first-time-folder) at the bottom, then come back and run the block above.

---

### Step 5 — Docker Desktop

1. **Start → Docker Desktop**. Wait until the whale icon is idle.
2. If Windows Firewall asks, click **Allow**.

```powershell
docker info
docker compose version
```

**Expected:** `Server Version:` (for example `29.7.2`) and `Docker Compose version` (for example `v5.4.0`).

If you see `error during connect`, wait and run the two commands again.

---

### Step 6 — Check script

```powershell
cd $env:USERPROFILE\MD287
powershell -ExecutionPolicy Bypass -File "labs\day-00\lab0\tools\check-environment.ps1"
```

**Expected last line:** `LAB 0 ENVIRONMENT: PASS`

Every row should be **PASS**. Warnings are OK — if a port is already in use the script prints `PASS WITH WARNINGS`, which is also success. If **FAIL**, raise a hand and re-run after the fix.

---

### Step 7 — Lab 1 Postgres

```powershell
cd "$env:USERPROFILE\MD287\labs\day-01\lab1\starter\account-service"
docker compose up -d
Start-Sleep -Seconds 20
docker compose ps
```

**Expected:** `md287-account-db` is **`(healthy)`** and ports include `5433->5432`.

If STATUS still says `health: starting`, wait and run only:

```powershell
docker compose ps
```

Leave it running. Do not run `docker compose down` until the end of the day.

---

### Step 8 — Maven cache

Stay in `account-service` (same folder as Step 7):

```powershell
mvn -q -DskipTests dependency:go-offline
```

**Expected:** the prompt returns with no `BUILD FAILURE`. This can take several minutes the first time.

Do **not** run `mvn spring-boot:run` (that is Lab 1).

---

## Done when

- VS Code folder is **`MD287`**
- Check script printed **LAB 0 ENVIRONMENT: PASS**
- `docker compose ps` shows `md287-account-db` **(healthy)** on **5433**
- Maven cache command finished
- You did **not** start Account Service and did **not** run `oc login`

Next: [Lab 1](../../day-01/lab1/LAB-1-GUIDE.md).

---

## If something fails

| You see | Do this |
| --- | --- |
| `git pull`: not a git repository | `cd $env:USERPROFILE\MD287` then `git pull`. |
| `destination path 'MD287' already exists` | Ignore. You already have the repo. `cd $env:USERPROFILE\MD287` then `git pull`. |
| `mklink`: already exists | Ignore. `cd $env:USERPROFILE\MD287` then `git pull`. |
| `Move-Item`: in use | Do not rename. Use [First-time folder](#first-time-folder) only if `MD287` is missing. |
| `docker info`: error during connect | Start Docker Desktop; wait for the whale; retry. |
| `docker compose ps`: `health: starting` | Wait 20 seconds; run `docker compose ps` again. |
| `python` opens Microsoft Store | Raise a hand. |
| `java` is 17 | Raise a hand. Do not install another JDK. |
| `curl` prints PowerShell XML | Type **`curl.exe`**. |

Leave Postgres running into Lab 1. To stop for the day:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-01\lab1\starter\account-service"
docker compose down
```

Do not add `-v`.

---

## First-time folder

Use this **only** when `cd $env:USERPROFILE\MD287` fails.

**A — VS Code already cloned the long name** (do not `Move-Item`):

```powershell
cd $env:USERPROFILE
cmd /c mklink /J MD287 "%USERPROFILE%\.vscode\springboot-microservices-with-openshift-ai-and-devops-participant"
cd $env:USERPROFILE\MD287
git pull
```

**B — nothing cloned yet:**

```powershell
cd $env:USERPROFILE
git clone https://github.com/Innovation-In-Software/springboot-microservices-with-openshift-ai-and-devops-participant.git MD287
cd MD287
```

Then continue at Step 4 (`git pull` / `Get-ChildItem`).

---

© 2026 Innovation In Software Corporation
