# Push Lab 5 Risk Assessment image. Requires oc login and md287/risk-assessment-service:1.0.0
# From labs/day-05/lab5:
#   $env:MD287_REGISTRY = "default-route-openshift-image-registry.apps.aro-md287.centralus.aroapp.io"
#   powershell -File tools\push-risk-image.ps1
#
# Same helper as Lab 4: Python first. Do NOT docker login by hand.

$ErrorActionPreference = "Stop"

. "$PSScriptRoot\..\..\..\tools\push-to-openshift.ps1"

$internal = Push-Md287Image -Local "md287/risk-assessment-service:1.0.0" -Name "risk-assessment-service"
Write-Host "Pushed. Internal pullspec: $internal"
