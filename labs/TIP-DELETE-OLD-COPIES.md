# Tip — Delete old copies (keep `MD287`)

**Where:** Ablaze VM (not your laptop)  
**When:** after you renamed or cloned into `%USERPROFILE%\MD287`, and a long-named folder is still on disk

Keep **one** clone:

```text
%USERPROFILE%\MD287
```

Delete leftover folders that still use the long GitHub name. Do **not** delete `MD287`.

Rename first if you do not have `MD287` yet: [Tip — short folder `MD287`](TIP-SHORT-FOLDER.md).

---

## 1. See what copies exist

In **VM** PowerShell:

```powershell
$keep = "$env:USERPROFILE\MD287"
$old = @(
  "$env:USERPROFILE\.vscode\springboot-microservices-with-openshift-ai-and-devops-participant",
  "$env:USERPROFILE\springboot-microservices-with-openshift-ai-and-devops-participant",
  "$env:USERPROFILE\springboot-microservices-with-openshift-ai-and-devops",
  "$env:USERPROFILE\.vscode\springboot-microservices-with-openshift-ai-and-devops"
)
Write-Host "KEEP: $keep  exists=$(Test-Path $keep)"
$old | ForEach-Object { Write-Host "OLD:  $_  exists=$(Test-Path $_)" }
```

Confirm `KEEP` is `exists=True` and you see `labs\` inside it before you delete anything:

```powershell
Get-ChildItem $keep
```

---

## 2. Delete old copies

This does **not** touch `MD287`.

```powershell
foreach ($p in $old) {
  if (Test-Path $p) {
    Remove-Item -LiteralPath $p -Recurse -Force
    Write-Host "Removed $p"
  }
}
```

Do **not** run `Remove-Item` on `$env:USERPROFILE\MD287`.

---

## 3. Confirm and reopen

```powershell
cd $env:USERPROFILE\MD287
Get-ChildItem
git status
git pull
```

In VS Code: **File → Open Folder** → `%USERPROFILE%\MD287`. Close any window still pointed at the long folder name.

The prompt should end with `\MD287>`.

---

## Continue Lab 0

```powershell
cd $env:USERPROFILE\MD287
powershell -ExecutionPolicy Bypass -File "labs\day-00\lab0\tools\check-environment.ps1"
```

Full steps: [Lab 0 guide](day-00/lab0/LAB-0-GUIDE.md).

---

© 2026 Innovation In Software Corporation
