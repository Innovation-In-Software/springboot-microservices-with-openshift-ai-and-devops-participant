#Requires -Version 5.1
<#
.SYNOPSIS
    MD287 Lab 0 — verify the TEKsystems Ablaze VM can run Labs 1-5.

.DESCRIPTION
    Checks Java 21, Maven-on-21, Git, Python 3.12+, oc client, curl.exe,
    Docker engine, and host ports used by Lab 1. Does not start Account
    Service and does not run oc login.

.NOTES
    Run from anywhere:
      powershell -ExecutionPolicy Bypass -File labs\day-00\lab0\tools\check-environment.ps1
#>
[CmdletBinding()]
param()

$ErrorActionPreference = "Continue"

function Get-CommandPath {
    param([Parameter(Mandatory)][string]$Name)
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    return $null
}

function Get-JavaVersionText {
    $old = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $lines = & java -version 2>&1 | ForEach-Object { $_.ToString() }
        return ($lines -join "`n")
    } catch {
        return ""
    } finally {
        $ErrorActionPreference = $old
    }
}

function Test-PortFree {
    param([Parameter(Mandatory)][int]$Port)
    try {
        $listeners = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
        if ($listeners) { return $false }
    } catch {
        # Fallback when the NetTCPIP cmdlet is unavailable.
        $netstat = & netstat.exe -ano 2>$null | Select-String -Pattern ":$Port\s"
        if ($netstat) { return $false }
    }
    return $true
}

function Get-PythonVersionText {
    $old = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $out = & python --version 2>&1 | ForEach-Object { $_.ToString() }
        return ($out -join " ").Trim()
    } catch {
        return ""
    } finally {
        $ErrorActionPreference = $old
    }
}

function Get-DockerInfoText {
    param([int]$TimeoutMs = 20000)
    $outFile = Join-Path $env:TEMP "md287-lab0-docker-info-out.txt"
    $errFile = Join-Path $env:TEMP "md287-lab0-docker-info-err.txt"
    Remove-Item $outFile, $errFile -ErrorAction SilentlyContinue
    try {
        $p = Start-Process -FilePath "docker" -ArgumentList "info" -NoNewWindow -PassThru `
            -RedirectStandardOutput $outFile -RedirectStandardError $errFile
        if (-not $p.WaitForExit($TimeoutMs)) {
            try { $p.Kill() } catch { }
            return "TIMEOUT"
        }
        $out = ""
        if (Test-Path $outFile) { $out += (Get-Content $outFile -Raw -ErrorAction SilentlyContinue) }
        if (Test-Path $errFile) { $out += "`n" + (Get-Content $errFile -Raw -ErrorAction SilentlyContinue) }
        return $out
    } catch {
        return $_.Exception.Message
    }
}

$script:failed = 0
$script:warned = 0
$rows = New-Object System.Collections.Generic.List[object]

function Add-Check {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][ValidateSet("PASS", "FAIL", "WARN")][string]$Status,
        [Parameter(Mandatory)][string]$Detail
    )
    $rows.Add([pscustomobject]@{ Check = $Name; Status = $Status; Detail = $Detail })
    if ($Status -eq "FAIL") { $script:failed++ }
    if ($Status -eq "WARN") { $script:warned++ }
}

Write-Host ""
Write-Host "MD287 Lab 0 — environment check" -ForegroundColor Cyan
Write-Host ("Host: {0}  User: {1}  {2}" -f $env:COMPUTERNAME, $env:USERNAME, (Get-Date -Format "yyyy-MM-dd HH:mm"))
Write-Host ""

# --- Java 21 ---
$javaPath = Get-CommandPath "java"
if (-not $javaPath) {
    Add-Check "Java" "FAIL" "java is not on PATH"
} else {
    $javaText = Get-JavaVersionText
    if ($javaText -match 'version\s+"21([\.\-"]|$)') {
        $line = ($javaText -split "`n")[0].Trim()
        Add-Check "Java 21" "PASS" "$line  ($javaPath)"
    } elseif ($javaText -match 'version\s+"(\d+)') {
        Add-Check "Java 21" "FAIL" "Found Java $($Matches[1]) at $javaPath — need 21 first on PATH"
    } else {
        Add-Check "Java 21" "FAIL" "Could not parse java -version. Output: $javaText"
    }
}

# --- Maven using Java 21 ---
$mvnPath = Get-CommandPath "mvn"
if (-not $mvnPath) {
    Add-Check "Maven" "FAIL" "mvn is not on PATH"
} else {
    $mvnText = (& mvn -version 2>&1 | ForEach-Object { $_.ToString() }) -join "`n"
    $mvnLine = ($mvnText -split "`n" | Where-Object { $_ -match "Apache Maven" } | Select-Object -First 1)
    $javaLine = ($mvnText -split "`n" | Where-Object { $_ -match "Java version" } | Select-Object -First 1)
    if (-not $mvnLine) { $mvnLine = "mvn ran" }
    if ($mvnText -match "Java version:\s*21") {
        Add-Check "Maven on Java 21" "PASS" "$($mvnLine.Trim()); $($javaLine.Trim())"
    } elseif ($javaLine) {
        Add-Check "Maven on Java 21" "FAIL" "$($mvnLine.Trim()); $($javaLine.Trim()) — Maven must use Java 21"
    } else {
        Add-Check "Maven on Java 21" "FAIL" "mvn -version did not print a Java version"
    }
}

# --- Git ---
$gitPath = Get-CommandPath "git"
if (-not $gitPath) {
    Add-Check "Git" "FAIL" "git is not on PATH"
} else {
    $gitVer = (& git --version 2>&1 | ForEach-Object { $_.ToString() }) -join " "
    if ($gitVer -match "git version 2\.(\d+)") {
        $minor = [int]$Matches[1]
        if ($minor -ge 40) {
            Add-Check "Git 2.40+" "PASS" $gitVer.Trim()
        } else {
            Add-Check "Git 2.40+" "WARN" "$($gitVer.Trim()) — 2.40+ preferred"
        }
    } else {
        Add-Check "Git" "WARN" $gitVer.Trim()
    }
}

# --- Python 3.12+ (not Store stub) ---
$pythonPath = Get-CommandPath "python"
if (-not $pythonPath) {
    Add-Check "Python 3.12+" "FAIL" "python is not on PATH (needed Days 3-5 for issue-jwt.py)"
} elseif ($pythonPath -match "WindowsApps") {
    Add-Check "Python 3.12+" "FAIL" "Microsoft Store stub at $pythonPath — need a real 3.12+ install"
} else {
    $pyText = Get-PythonVersionText
    if ($pyText -match "Python 3\.(1[2-9]|[2-9]\d)") {
        Add-Check "Python 3.12+" "PASS" "$pyText  ($pythonPath)"
    } elseif ($pyText -match "Python 3\.(\d+)") {
        Add-Check "Python 3.12+" "FAIL" "$pyText at $pythonPath — need 3.12+"
    } else {
        Add-Check "Python 3.12+" "FAIL" "Unexpected python --version: $pyText ($pythonPath)"
    }
}

# --- oc client ---
$ocPath = Get-CommandPath "oc"
if (-not $ocPath) {
    Add-Check "OpenShift CLI" "FAIL" "oc is not on PATH (required Days 4-5). Do not oc login today."
} else {
    $ocText = (& oc version --client 2>&1 | ForEach-Object { $_.ToString() }) -join " "
    if ($ocText -match "4\.\d+") {
        Add-Check "OpenShift CLI 4.x" "PASS" "$($ocText.Trim()) — do not oc login until Day 4"
    } else {
        Add-Check "OpenShift CLI" "WARN" "$($ocText.Trim()) ($ocPath)"
    }
}

# --- curl.exe ---
$curlPath = Get-CommandPath "curl.exe"
if (-not $curlPath) {
    Add-Check "curl.exe" "FAIL" "curl.exe not found — do not use the curl alias"
} else {
    $curlText = (& curl.exe --version 2>&1 | Select-Object -First 1).ToString()
    Add-Check "curl.exe" "PASS" "$curlText"
}

# --- Docker engine ---
$dockerPath = Get-CommandPath "docker"
if (-not $dockerPath) {
    Add-Check "Docker" "FAIL" "docker is not on PATH"
} else {
    $dockerInfo = Get-DockerInfoText
    if ($dockerInfo -eq "TIMEOUT") {
        Add-Check "Docker engine" "FAIL" "docker info timed out. Start Docker Desktop and wait until the whale icon is idle."
    } elseif ($dockerInfo -match "Server Version") {
        $server = ($dockerInfo -split "`n" | Where-Object { $_ -match "Server Version" } | Select-Object -First 1)
        Add-Check "Docker engine" "PASS" $server.Trim()
    } else {
        $hint = "Start Docker Desktop and wait until the whale icon is idle"
        if ($dockerInfo -match "error during connect|docker_engine|The system cannot find the file specified") {
            Add-Check "Docker engine" "FAIL" "$hint. docker info cannot reach the engine."
        } else {
            $snippet = if ($dockerInfo) { $dockerInfo.Substring(0, [Math]::Min(180, $dockerInfo.Length)) } else { "(no output)" }
            Add-Check "Docker engine" "FAIL" "$hint. Output snippet: $snippet"
        }
    }
    $composeText = (& docker compose version 2>&1 | ForEach-Object { $_.ToString() }) -join " "
    if ($LASTEXITCODE -eq 0 -and $composeText -match "v?2\.|version") {
        Add-Check "Docker Compose v2" "PASS" $composeText.Trim()
    } else {
        Add-Check "Docker Compose v2" "FAIL" "docker compose version failed. Need Compose v2 (docker compose, two words)."
    }
}

# --- Repo layout (if we can see it) ---
$repoRoot = $null
$here = $PSScriptRoot
if ($here -and (Test-Path (Join-Path $here "..\..\..\..\README.md"))) {
    $repoRoot = (Resolve-Path (Join-Path $here "..\..\..\..")).Path
}
if (-not $repoRoot -and (Test-Path ".\labs\day-01\lab1\starter\account-service\pom.xml")) {
    $repoRoot = (Resolve-Path ".").Path
}
if ($repoRoot) {
    $accountPom = Join-Path $repoRoot "labs\day-01\lab1\starter\account-service\pom.xml"
    $composeFile = Join-Path $repoRoot "labs\day-01\lab1\starter\account-service\docker-compose.yml"
    if ((Test-Path $accountPom) -and (Test-Path $composeFile)) {
        Add-Check "Course repo" "PASS" $repoRoot
    } else {
        Add-Check "Course repo" "FAIL" "MD287 clone incomplete at $repoRoot"
    }
} else {
    Add-Check "Course repo" "WARN" "Run this script from the cloned MD287 repo (or after Step 3 of LAB-0-GUIDE.md)"
}

# --- Lab 1 ports ---
foreach ($port in @(8081, 5433)) {
    $label = switch ($port) {
        8081 { "Port 8081 (Account Service)" }
        5433 { "Port 5433 (Account Postgres)" }
    }
    if (Test-PortFree -Port $port) {
        Add-Check $label "PASS" "Listening-free on localhost"
    } else {
        $who = ""
        try {
            $procId = (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1).OwningProcess
            if ($procId) {
                $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
                if ($proc) { $who = " (PID $procId $($proc.ProcessName))" }
            }
        } catch { }
        if ($port -eq 5433) {
            Add-Check $label "WARN" "In use$who — OK if md287-account-db is already healthy from Step 6"
        } else {
            Add-Check $label "WARN" "In use$who — stop the other process before Lab 1"
        }
    }
}

# --- Print table ---
Write-Host ("{0,-32} {1,-6} {2}" -f "CHECK", "STATUS", "DETAIL")
Write-Host ("{0,-32} {1,-6} {2}" -f ("-" * 32), ("-" * 6), ("-" * 40))
foreach ($row in $rows) {
    $color = switch ($row.Status) {
        "PASS" { "Green" }
        "WARN" { "Yellow" }
        default { "Red" }
    }
    Write-Host ("{0,-32} {1,-6} {2}" -f $row.Check, $row.Status, $row.Detail) -ForegroundColor $color
}

Write-Host ""
if ($script:failed -gt 0) {
    Write-Host "LAB 0 ENVIRONMENT: FAIL  ($($script:failed) check(s) failed, $($script:warned) warning(s))" -ForegroundColor Red
    Write-Host "Fix the FAIL rows using labs\day-00\lab0\LAB-0-GUIDE.md (Troubleshooting), then re-run." -ForegroundColor Red
    exit 1
}

if ($script:warned -gt 0) {
    Write-Host "LAB 0 ENVIRONMENT: PASS WITH WARNINGS  ($($script:warned) warning(s))" -ForegroundColor Yellow
    Write-Host "You can continue. Read the WARN rows before Lab 1." -ForegroundColor Yellow
    exit 0
}

Write-Host "LAB 0 ENVIRONMENT: PASS" -ForegroundColor Green
Write-Host "Do not start Account Service yet. Do not oc login. Continue to Lab 0 Step 6 if Compose is not up." -ForegroundColor Green
exit 0
