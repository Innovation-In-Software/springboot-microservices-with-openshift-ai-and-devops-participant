# Lab 0 — Log in to the VM and set up the workstation

**Day:** 1 morning — before Module 1  
**Time:** 20–30 minutes

Do **all** of this **on the Ablaze VM**. Your laptop is only the browser. Do not start Account Service. Do not run `oc login`.

Work from the short folder **`%USERPROFILE%\MD287`** (example: `C:\Users\student.VLAB\MD287`).

---

### Step 1 — Log in to Ablaze

1. Get **your** username and the class password from the instructor. Do not use `MSMICR26-TD`.
2. Open **https://my.ablazedesktop.com** and sign in.
3. Wait for the **Windows desktop** (Start, taskbar, icons). Leave this tab open all week.

---

### Step 2 — Open VS Code and Copilot on the VM

1. On that desktop: **Start → Visual Studio Code** (not on your laptop).
2. Press **Ctrl+`**. Choose **Windows PowerShell**.
3. **Ctrl+Shift+P** → **GitHub Copilot: Sign In**. Status bar should show Copilot ready.

---

### Step 3 — Check the toolchain

```powershell
java -version
mvn -version
git --version
python --version
oc version --client
curl.exe --version
```

| Command | Pass |
| --- | --- |
| `java -version` | `21` |
| `mvn -version` | Maven 3.9.x and Java 21 |
| `python --version` | 3.12 or 3.13 — not the Microsoft Store |
| `oc version --client` | 4.x |
| `curl.exe --version` | Windows curl (always type **`curl.exe`**) |

If `java` is 17, or `python` opens the Store, raise a hand. Do not install software yourself.

---

### Step 4 — Put the repo in `MD287`

**If you do not have the repo yet:**

```powershell
cd $env:USERPROFILE
git clone https://github.com/Innovation-In-Software/springboot-microservices-with-openshift-ai-and-devops-participant.git MD287
cd MD287
```

**If VS Code already cloned the long name** (folder under `.vscode\...-participant`): do **not** `git clone` again. Do **not** `Move-Item` — VS Code has that folder locked. Make a short name, then pull:

```powershell
cd $env:USERPROFILE
cmd /c mklink /J MD287 "%USERPROFILE%\.vscode\springboot-microservices-with-openshift-ai-and-devops-participant"
cd $env:USERPROFILE\MD287
git pull
```

**If you are already in `MD287`:**

```powershell
cd $env:USERPROFILE\MD287
git pull
```

`git pull` only works **inside** the repo. Do not run it from `C:\Users\student.VLAB`.

Then: **File → Open Folder** → `%USERPROFILE%\MD287`.

The prompt must end with `\MD287>`. Confirm `labs\` is there:

```powershell
Get-ChildItem
```

---

### Step 5 — Start Docker Desktop

1. Open **Docker Desktop** from Start. Wait until the whale icon is idle.
2. If Windows Firewall asks, click **Allow**.

```powershell
docker info
docker compose version
```

`docker info` must print a **Server Version**. If you see `error during connect`, wait and retry.

---

### Step 6 — Run the check script

```powershell
cd $env:USERPROFILE\MD287
powershell -ExecutionPolicy Bypass -File "labs\day-00\lab0\tools\check-environment.ps1"
```

You need **LAB 0 ENVIRONMENT: PASS**. Fix any FAIL, then re-run.

---

### Step 7 — Start Lab 1 Postgres

```powershell
cd "$env:USERPROFILE\MD287\labs\day-01\lab1\starter\account-service"
docker compose up -d
docker compose ps
```

Wait until `md287-account-db` is **(healthy)** on host port **5433**. Leave it running.

---

### Step 8 — Warm the Maven cache

Still in `account-service`:

```powershell
mvn -q -DskipTests dependency:go-offline
```

Wait until it returns with no `BUILD FAILURE`. Do **not** run `mvn spring-boot:run` (that is Lab 1).

---

## Done when

- VS Code is open on **`MD287`**
- Check script prints **PASS**
- `md287-account-db` is **(healthy)**
- Maven cache command finished
- You did **not** start Account Service and did **not** run `oc login`

---

## If something fails

| Symptom | What to do |
| --- | --- |
| `git fetch` / `git pull`: not a git repository | You are in the home folder. `cd $env:USERPROFILE\MD287` first. |
| `git clone` fails because `MD287` exists | Do not clone. Use `cd $env:USERPROFILE\MD287` then `git pull`. |
| `Move-Item` / rename: in use | Do not rename. Use the `mklink /J` commands in Step 4. |
| `mklink`: name already exists | `cd $env:USERPROFILE\MD287` then `git pull`. |
| `docker info`: error during connect | Start Docker Desktop; wait for the whale; retry. |
| `python` opens Microsoft Store | Raise a hand (TEKsystems). |
| `java` is 17 | Raise a hand. Do not install another JDK. |
| `curl` prints PowerShell XML | Type **`curl.exe`**. |

Leave Postgres running into Lab 1. To stop for the day:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-01\lab1\starter\account-service"
docker compose down
```

Do not add `-v`.

---

© 2026 Innovation In Software Corporation
