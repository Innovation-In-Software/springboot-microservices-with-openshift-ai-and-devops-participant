# Walk the prepared pipeline stages locally (no Tekton required).
# Run from anywhere:  powershell -File labs/day-04/lab4/tools/run-pipeline-locally.ps1

$ErrorActionPreference = "Stop"
$tools = Split-Path -Parent $MyInvocation.MyCommand.Path
$labRoot = Split-Path -Parent $tools
Set-Location $labRoot

function Invoke-Gate([string]$report) {
    $script = Join-Path $tools "scan-gate.ps1"
    $arg = "-NoProfile -File `"$script`" -ReportPath `"$report`""
    $p = Start-Process -FilePath "powershell.exe" -ArgumentList $arg -Wait -PassThru -NoNewWindow
    return [int]$p.ExitCode
}

function Stage([string]$name, [scriptblock]$body) {
    Write-Host ""
    Write-Host "======== STAGE: $name ========"
    & $body
}

Stage "checkout" { Write-Host "Git revision: master (classroom). No clone needed - you already have the repo." }
Stage "compile" { Write-Host "Image build (Docker) compiles with Maven inside the Containerfile. Skip extra mvn here." }
Stage "test" { Write-Host "Lab 3 unit tests already guard JWT and the circuit breaker. Do not skip them in a real pipeline." }
Stage "scan" {
    $pass = Invoke-Gate (Join-Path $tools "sample-scan-pass.json")
    if ($pass -ne 0) { throw "Unexpected fail on the PASS sample" }
    Write-Host "Demonstrating the fail sample (expected GATE FAIL):"
    $fail = Invoke-Gate (Join-Path $tools "sample-scan-fail.json")
    if ($fail -eq 0) { throw "Fail sample should not pass" }
    Write-Host "Gate correctly rejected CRITICAL. Continuing with the PASS report."
}
Stage "image" { Write-Host "Inspect with: docker images md287/account-service" }
Stage "SBOM" { & powershell -NoProfile -File (Join-Path $tools "generate-sbom.ps1") }
Stage "sign" { & powershell -NoProfile -File (Join-Path $tools "verify-signature.ps1") }
Stage "deploy" { Write-Host "Compose or oc rollout is Step 4 / Step 5 / Step 7 of the lab guide." }
Stage "smoke" { Write-Host "curl.exe -s http://localhost:8081/actuator/health/readiness" }

Write-Host ""
Write-Host "Pipeline review complete. Record: scan PASS (sample), SBOM present, signature reviewed."
