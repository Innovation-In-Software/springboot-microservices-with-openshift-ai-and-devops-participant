# Tip — Use the short folder `MD287`

**Where:** Ablaze VM (not your laptop)  
**When:** Lab 0 clone, or any time VS Code used the long GitHub repo name

Labs 0–5 expect this path:

```text
%USERPROFILE%\MD287
```

Example: `C:\Users\student.VLAB\MD287`

Do **not** leave the clone under the long GitHub name (`springboot-microservices-with-openshift-ai-and-devops` or `...-participant`). Every later `cd labs\day-NN\...` command uses the short folder.

---

## Preferred — clone with the short name

In **VM** PowerShell:

```powershell
cd $env:USERPROFILE
git clone https://github.com/Innovation-In-Software/springboot-microservices-with-openshift-ai-and-devops-participant.git MD287
cd MD287
```

The last argument (`MD287`) is the folder name.

Then in VS Code: **File → Open Folder** → `%USERPROFILE%\MD287`.

The prompt should end with `\MD287>`.

---

## If VS Code already cloned the long name

The folder is often:

```text
%USERPROFILE%\.vscode\springboot-microservices-with-openshift-ai-and-devops-participant
```

Rename it **once**. Do not clone a second copy.

```powershell
$long = "$env:USERPROFILE\.vscode\springboot-microservices-with-openshift-ai-and-devops-participant"
$short = "$env:USERPROFILE\MD287"
if ((Test-Path $long) -and -not (Test-Path $short)) {
  Move-Item $long $short
}
cd $env:USERPROFILE\MD287
Get-ChildItem
```

You should see `README.md` and `labs\`.

Then in VS Code: **File → Open Folder** → `%USERPROFILE%\MD287`.

If `$short` already exists, do not `Move-Item`. Stay in `MD287` and delete the long folder: [Tip — delete old copies](TIP-DELETE-OLD-COPIES.md).

---

## If you clone with VS Code instead of PowerShell

1. **Ctrl+Shift+P** → **Git: Clone**.
2. Paste the course GitHub URL (or the participant URL the instructor issued).
3. Choose **`%USERPROFILE%`** as the parent folder (**not** `.vscode`).
4. Rename the long folder to `MD287` with the `Move-Item` commands above.
5. **File → Open Folder** → `%USERPROFILE%\MD287`.

---

## Continue Lab 0 after the rename

```powershell
cd $env:USERPROFILE\MD287
powershell -ExecutionPolicy Bypass -File "labs\day-00\lab0\tools\check-environment.ps1"
```

Full steps: [Lab 0 guide](day-00/lab0/LAB-0-GUIDE.md) (Step 4 is the clone / rename).

---

© 2026 Innovation In Software Corporation
