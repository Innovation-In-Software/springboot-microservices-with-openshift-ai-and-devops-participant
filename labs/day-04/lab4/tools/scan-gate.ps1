# Evaluate a Trivy-style JSON report. Exit 1 if any CRITICAL finding exists.

param(
    [Parameter(Mandatory = $true)]
    [string]$ReportPath
)

$ErrorActionPreference = "Stop"
if (-not (Test-Path $ReportPath)) {
    throw "Scan report not found: $ReportPath"
}

$report = Get-Content -Raw -Path $ReportPath | ConvertFrom-Json
$critical = @()
foreach ($result in @($report.Results)) {
    foreach ($vuln in @($result.Vulnerabilities)) {
        if ($vuln -and $vuln.Severity -eq "CRITICAL") {
            $critical += $vuln
        }
    }
}

if ($critical.Count -gt 0) {
    Write-Host "SCAN GATE FAIL - $($critical.Count) CRITICAL finding(s)"
    $critical | ForEach-Object { Write-Host ("  {0} {1}" -f $_.VulnerabilityID, $_.PkgName) }
    exit 1
}

Write-Host "SCAN GATE PASS - 0 CRITICAL findings"
exit 0
