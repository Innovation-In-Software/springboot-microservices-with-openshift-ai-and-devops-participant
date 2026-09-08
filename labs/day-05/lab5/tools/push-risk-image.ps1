# Push Lab 5 Risk Assessment image. Requires oc login and md287/risk-assessment-service:1.0.0
# From labs/day-05/lab5:
#   powershell -File tools\push-risk-image.ps1

$ErrorActionPreference = "Stop"
if (-not (oc whoami 2>$null)) { throw "oc login first." }
$Project = (oc project -q).Trim()
$RegHost = $env:MD287_REGISTRY
if (-not $RegHost) {
    $RegHost = oc get route default-route -n openshift-image-registry -o jsonpath="{.spec.host}" 2>$null
}
if (-not $RegHost) { throw "Set MD287_REGISTRY or expose the image-registry default Route." }

$who = oc whoami
$token = oc whoami -t
docker login -u $who -p $token $RegHost | Out-Host
docker tag md287/risk-assessment-service:1.0.0 "$RegHost/$Project/risk-assessment-service:1.0.0"
docker push "$RegHost/$Project/risk-assessment-service:1.0.0"
$Internal = "image-registry.openshift-image-registry.svc:5000/$Project/risk-assessment-service:1.0.0"
$exists = oc get deploy risk-assessment-service -n $Project --ignore-not-found
if ($exists) {
    oc set image deploy/risk-assessment-service "risk-assessment-service=$Internal" -n $Project
}
Write-Host "Pushed. Internal pullspec: $Internal"
