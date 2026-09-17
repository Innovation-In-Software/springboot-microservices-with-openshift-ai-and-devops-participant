# Push Lab 4 images to the participant OpenShift project (ARO internal registry).
# Requires: oc login, docker images md287/account-service:1.0.0 and md287/transaction-service:1.0.0
#
# From labs/day-04/lab4:
#   $env:MD287_REGISTRY = "default-route-openshift-image-registry.apps.aro-md287.centralus.aroapp.io"
#   powershell -File tools\push-images.ps1
#
# Ablaze Docker Desktop often returns HTTP 403 if it uses Windows Credential Manager
# (the OpenShift token is too long). The helper uses a throwaway docker config, then
# skopeo, then a Python registry push that skips TLS verify.

$ErrorActionPreference = "Stop"

. "$PSScriptRoot\..\..\..\tools\push-to-openshift.ps1"

Push-Md287Image -Local "md287/account-service:1.0.0" -Name "account-service" | Out-Null
Push-Md287Image -Local "md287/transaction-service:1.0.0" -Name "transaction-service" | Out-Null

Write-Host "Push complete. If pods are ImagePullBackOff, run the oc set image lines in LAB-4-GUIDE.md Step 5."
Write-Host "Internal pullspec example: image-registry.openshift-image-registry.svc:5000/$(oc project -q)/account-service:1.0.0"
