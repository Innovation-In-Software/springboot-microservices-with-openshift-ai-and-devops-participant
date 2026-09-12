# Push Lab 4 images to the participant OpenShift project (ARO internal registry).
# Requires: oc login, docker images md287/account-service:1.0.0 and md287/transaction-service:1.0.0
# IIS must expose the image-registry default Route.
#
# From labs/day-04/lab4:
#   powershell -File tools\push-images.ps1

$ErrorActionPreference = "Stop"

function Require-Cmd([string]$Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command not found: $Name"
    }
}

Require-Cmd oc
Require-Cmd docker

$who = oc whoami 2>$null
if (-not $who) {
    throw "oc whoami failed. Log in first: oc login https://api.aro-md287.centralus.aroapp.io:6443/ --username <your-username> --password <password>"
}

$Project = (oc project -q).Trim()
if (-not $Project) { throw "oc project -q returned empty. Run: oc project md287-<your-username>" }

$RegHost = $env:MD287_REGISTRY
if (-not $RegHost) {
    $RegHost = oc get route default-route -n openshift-image-registry -o jsonpath="{.spec.host}" 2>$null
}
if (-not $RegHost) {
    $RegHost = "default-route-openshift-image-registry.apps.aro-md287.centralus.aroapp.io"
}

Write-Host "Participant: $who"
Write-Host "Project:     $Project"
Write-Host "Registry:    $RegHost"

$token = oc whoami -t
docker login -u $who -p $token $RegHost | Out-Host

$pairs = @(
    @{ Local = "md287/account-service:1.0.0"; Remote = "$RegHost/$Project/account-service:1.0.0"; Deploy = "account-service"; Container = "account-service" },
    @{ Local = "md287/transaction-service:1.0.0"; Remote = "$RegHost/$Project/transaction-service:1.0.0"; Deploy = "transaction-service"; Container = "transaction-service" }
)

foreach ($p in $pairs) {
    docker image inspect $p.Local | Out-Null
    docker tag $p.Local $p.Remote
    docker push $p.Remote
    Write-Host "Pushed $($p.Remote)"
}

$Internal = "image-registry.openshift-image-registry.svc:5000"
foreach ($p in $pairs) {
    $exists = oc get deploy $p.Deploy -n $Project --ignore-not-found
    if ($exists) {
        oc set image "deploy/$($p.Deploy)" "$($p.Container)=$Internal/$Project/$($p.Deploy):1.0.0" -n $Project
        Write-Host "Pointed deploy/$($p.Deploy) at the internal pullspec"
    }
}

Write-Host "Push complete. Apply YAML if you have not yet, then oc set image as above."
Write-Host "Internal pullspec example: $Internal/$Project/account-service:1.0.0"
