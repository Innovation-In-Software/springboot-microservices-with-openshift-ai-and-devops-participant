# Push Lab 4 images to the participant OpenShift project (ARO internal registry).
# Requires: oc login, docker images md287/account-service:1.0.0 and md287/transaction-service:1.0.0
#
# From labs/day-04/lab4:
#   $env:MD287_REGISTRY = "default-route-openshift-image-registry.apps.aro-md287.centralus.aroapp.io"
#   powershell -File tools\push-images.ps1
#
# Ablaze Docker Desktop often returns HTTP 403 if it uses Windows Credential Manager
# (the OpenShift token is too long). The helper pushes with Python first (verified
# path). Do NOT docker login by hand. A failed oc patch must not skip Transaction.

$ErrorActionPreference = "Stop"

. "$PSScriptRoot\..\..\..\tools\push-to-openshift.ps1"

Write-Host "=== Account Service image ==="
Push-Md287Image -Local "md287/account-service:1.0.0" -Name "account-service" | Out-Null
Write-Host "=== Transaction Service image ==="
Push-Md287Image -Local "md287/transaction-service:1.0.0" -Name "transaction-service" | Out-Null

Write-Host ""
Write-Host "Push complete. Both images must have printed: Pushed ... (python)  or  Pushed ... (docker)"
Write-Host "If a pod is ImagePullBackOff, run the oc set image block in LAB-4-GUIDE.md Step 5."
Write-Host "Internal pullspec example: image-registry.openshift-image-registry.svc:5000/$(oc project -q)/account-service:1.0.0"
