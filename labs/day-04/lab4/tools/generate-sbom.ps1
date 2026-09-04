# Generate a CycloneDX SBOM for the Account Service image.
# Prefers Syft if installed: https://github.com/anchore/syft
# Otherwise copies the classroom sample so the review step still works.

param(
    [string]$Image = "md287/account-service:1.0.0",
    [string]$OutFile = ""
)

$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $OutFile) {
    $OutFile = Join-Path $here "generated-sbom-account-service.json"
}

$syft = Get-Command syft -ErrorAction SilentlyContinue
if ($syft) {
    Write-Host "Using Syft to generate SBOM for $Image"
    & syft $Image -o cyclonedx-json="$OutFile"
    if ($LASTEXITCODE -ne 0) {
        throw "Syft failed with exit code $LASTEXITCODE"
    }
} else {
    $sample = Join-Path $here "sample-sbom-account-service.json"
    Write-Host "Syft not found. Copying classroom sample SBOM to $OutFile"
    Copy-Item -Path $sample -Destination $OutFile -Force
}

Write-Host "SBOM written: $OutFile"
Write-Host "Review: component names, versions, and that no unexpected license appears."
