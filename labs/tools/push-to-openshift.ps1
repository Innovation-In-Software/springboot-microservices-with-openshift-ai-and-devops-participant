# Shared helper: push a local Docker image to the ARO integrated registry.
# Dot-source from Lab 4 / Lab 5 push scripts. Do not run this file by itself.
#
# Classroom fact (verified 18 Sep 2026 on Docker Desktop 24.x / Windows):
#   docker push against the ARO registry often fails (Credential Manager truncates
#   the OpenShift token, or docker.exe rejects --disable-content-trust).
#   Python (push_image.py) is the reliable path. It is tried FIRST.
#   After a successful push this helper points the Deployment at the in-cluster
#   pullspec and restarts the rollout. A noisy oc patch must NEVER abort the
#   second image (Account then Transaction in Lab 4).

$script:Md287RegistryDefault = "default-route-openshift-image-registry.apps.aro-md287.centralus.aroapp.io"
$script:Md287PushImagePy = Join-Path $PSScriptRoot "push_image.py"

function Add-Md287OcToPath {
    if (Get-Command oc -ErrorAction SilentlyContinue) { return }
    $hint = Join-Path $env:LOCALAPPDATA "Programs\openshift-client"
    $exe = Join-Path $hint "oc.exe"
    if (Test-Path $exe) {
        $env:PATH = "$hint;$env:PATH"
    }
}

function Invoke-Md287Oc {
    Add-Md287OcToPath
    $oldEap = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & oc @args
        return $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $oldEap
    }
}

function Get-Md287OcIdentity {
    Add-Md287OcToPath
    $who = (oc whoami 2>$null | Out-String).Trim()
    if (-not $who) {
        throw "oc whoami failed. Log in first: oc login https://api.aro-md287.centralus.aroapp.io:6443/"
    }
    $token = (oc whoami -t 2>$null | Out-String).Trim()
    if (-not $token) {
        throw "oc whoami -t returned empty. Log in again."
    }
    $project = (oc project -q | Out-String).Trim()
    if (-not $project) {
        throw "oc project -q returned empty. Run: oc project md287-<your-username>"
    }
    return [pscustomobject]@{ User = $who; Token = $token; Project = $project }
}

function Get-Md287RegistryHost {
    if ($env:MD287_REGISTRY) {
        return $env:MD287_REGISTRY.Trim()
    }
    $fromRoute = (oc get route default-route -n openshift-image-registry -o jsonpath="{.spec.host}" 2>$null | Out-String).Trim()
    if ($fromRoute) { return $fromRoute }
    return $script:Md287RegistryDefault
}

function New-Md287DockerConfig {
    param(
        [Parameter(Mandatory = $true)][string]$Registry,
        [Parameter(Mandatory = $true)][string]$User,
        [Parameter(Mandatory = $true)][string]$Token
    )
    $dir = Join-Path $env:TEMP ("md287-docker-" + [guid]::NewGuid().ToString("n"))
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $pair = "{0}:{1}" -f $User, $Token
    $auth = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pair))
    # No credsStore / credHelpers — Docker Desktop's Windows helper truncates the OpenShift token (HTTP 403).
    $json = @"
{"auths":{"$Registry":{"auth":"$auth"},"https://$Registry":{"auth":"$auth"}}}
"@
    Set-Content -Path (Join-Path $dir "config.json") -Value $json -Encoding ascii
    return $dir
}

function Invoke-Md287DockerPush {
    param(
        [Parameter(Mandatory = $true)][string]$Remote,
        [Parameter(Mandatory = $true)][string]$ConfigDir
    )
    $oldCfg = $env:DOCKER_CONFIG
    $oldEap = $ErrorActionPreference
    $oldTrust = $env:DOCKER_CONTENT_TRUST
    $env:DOCKER_CONFIG = $ConfigDir
    $env:DOCKER_CONTENT_TRUST = "0"
    $ErrorActionPreference = "Continue"
    try {
        # Do not pass --disable-content-trust: Docker Desktop 24.x treats it as an unknown global flag.
        docker --config $ConfigDir push $Remote
        return ($LASTEXITCODE -eq 0)
    } finally {
        $ErrorActionPreference = $oldEap
        if ($null -ne $oldCfg) { $env:DOCKER_CONFIG = $oldCfg } else { Remove-Item Env:DOCKER_CONFIG -ErrorAction SilentlyContinue }
        if ($null -ne $oldTrust) { $env:DOCKER_CONTENT_TRUST = $oldTrust } else { Remove-Item Env:DOCKER_CONTENT_TRUST -ErrorAction SilentlyContinue }
    }
}

function Invoke-Md287SkopeoPush {
    param(
        [Parameter(Mandatory = $true)][string]$Local,
        [Parameter(Mandatory = $true)][string]$Registry,
        [Parameter(Mandatory = $true)][string]$RepoTag,
        [Parameter(Mandatory = $true)][string]$User,
        [Parameter(Mandatory = $true)][string]$Token
    )
    if (-not (Get-Command skopeo -ErrorAction SilentlyContinue)) {
        return $false
    }
    $oldEap = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $tar = Join-Path $env:TEMP ("md287-" + [guid]::NewGuid().ToString("n") + ".tar")
    try {
        docker save $Local -o $tar
        if ($LASTEXITCODE -ne 0) { return $false }
        Write-Host "docker push failed or was skipped; copying with skopeo (insecure TLS)..."
        skopeo copy --dest-tls-verify=false --dest-creds "${User}:${Token}" "docker-archive:$tar" "docker://${Registry}/${RepoTag}"
        return ($LASTEXITCODE -eq 0)
    } finally {
        $ErrorActionPreference = $oldEap
        Remove-Item $tar -Force -ErrorAction SilentlyContinue
    }
}

function Test-Md287RegistryHost {
    param([Parameter(Mandatory = $true)][string]$Registry)
    $tmp = Join-Path $env:TEMP ("md287-reg-" + [guid]::NewGuid().ToString("n") + ".txt")
    $code = "000"
    $oldEap = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $code = & curl.exe -k -sS -o $tmp -w "%{http_code}" --max-time 20 "https://$Registry/v2/"
    } catch {
        $code = "000"
    } finally {
        $ErrorActionPreference = $oldEap
    }
    $body = ""
    if (Test-Path $tmp) {
        $body = (Get-Content -Raw -Path $tmp -ErrorAction SilentlyContinue)
        Remove-Item $tmp -Force -ErrorAction SilentlyContinue
    }
    if ($body -match "Application is not available" -or $body -match "host doesn't exist") {
        throw @"
Registry host $Registry is the OpenShift router default page, not the image registry.

Use:
  `$env:MD287_REGISTRY = "default-route-openshift-image-registry.apps.aro-md287.centralus.aroapp.io"

The Account/Transaction Route showing "Application is not available" is expected until the image is pushed and the pod is Ready. Do not curl that Route as MD287_REGISTRY.
Ask the instructor if IIS has not exposed the default-route in openshift-image-registry.
"@
    }
    Write-Host "Registry /v2/ HTTP $code (401 here is normal before login)"
}

function Invoke-Md287PythonPush {
    param(
        [Parameter(Mandatory = $true)][string]$Local,
        [Parameter(Mandatory = $true)][string]$Registry,
        [Parameter(Mandatory = $true)][string]$RepoTag,
        [Parameter(Mandatory = $true)][string]$User,
        [Parameter(Mandatory = $true)][string]$Token
    )
    $py = $script:Md287PushImagePy
    if (-not $py -or -not (Test-Path $py)) {
        return $false
    }
    Write-Host "Pushing with Python (skips TLS verify; avoids Docker Credential Manager). 1-2 minutes is normal."
    $env:OC_USER = $User
    $env:OC_TOKEN = $Token
    $oldEap = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & python $py $Local $Registry $RepoTag
        $code = $LASTEXITCODE
        if ($null -eq $code) { $code = 1 }
        return ($code -eq 0)
    } finally {
        $ErrorActionPreference = $oldEap
        Remove-Item Env:OC_TOKEN -ErrorAction SilentlyContinue
        Remove-Item Env:OC_USER -ErrorAction SilentlyContinue
    }
}

function Ensure-Md287ImageStream {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Project
    )
    $oldEap = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $exists = oc get is $Name -n $Project --ignore-not-found 2>$null
        if (-not $exists) {
            oc create is $Name -n $Project 2>$null | Out-Host
        }
    } finally {
        $ErrorActionPreference = $oldEap
    }
}

function Update-Md287DeploymentImage {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Project,
        [Parameter(Mandatory = $true)][string]$Internal
    )
    $oldEap = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $deploy = oc get deploy $Name -n $Project --ignore-not-found 2>$null
        if (-not $deploy) {
            Write-Host "No deploy/$Name yet — apply the OpenShift YAML first, then re-run this script or the oc set image block in the guide."
            return
        }
        oc set image "deploy/$Name" "${Name}=$Internal" -n $Project 2>$null | Out-Host
        oc rollout restart "deploy/$Name" -n $Project 2>$null | Out-Host
        Write-Host "Pointed deploy/$Name at $Internal and restarted the rollout (same tag does not pull by itself)."
    } catch {
        Write-Warning "Image is in the registry. Run the oc set image / rollout restart block in the lab guide. $_"
    } finally {
        $ErrorActionPreference = $oldEap
    }
}

function Push-Md287Image {
    param(
        [Parameter(Mandatory = $true)][string]$Local,
        [Parameter(Mandatory = $true)][string]$Name,
        [string]$Tag = "1.0.0"
    )
    Add-Md287OcToPath
    if (-not (Get-Command oc -ErrorAction SilentlyContinue)) { throw "oc is not on PATH. Lab 0 should have installed it. Raise a hand." }
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw "docker is not on PATH" }
    if (-not (Get-Command python -ErrorAction SilentlyContinue)) { throw "python is not on PATH (needed to push when docker push is blocked)" }

    $oldEap = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    docker image inspect $Local 2>$null | Out-Null
    $inspectCode = $LASTEXITCODE
    $ErrorActionPreference = $oldEap
    if ($inspectCode -ne 0) {
        throw "Local image $Local not found. Build it first (Lab 4 Step 2 / Lab 5 Step 8)."
    }

    $id = Get-Md287OcIdentity
    $registry = Get-Md287RegistryHost
    $repoTag = "{0}/{1}:{2}" -f $id.Project, $Name, $Tag
    $remote = "{0}/{1}" -f $registry, $repoTag

    Write-Host "Participant: $($id.User)"
    Write-Host "Project:     $($id.Project)"
    Write-Host "Registry:    $registry"
    Write-Host "Pushing:     $Local  ->  $remote"

    Test-Md287RegistryHost -Registry $registry
    Ensure-Md287ImageStream -Name $Name -Project $id.Project
    docker tag $Local $remote

    $how = $null
    if (Invoke-Md287PythonPush -Local $Local -Registry $registry -RepoTag $repoTag -User $id.User -Token $id.Token) {
        $how = "python"
    } else {
        Write-Host "Python push did not succeed; trying isolated docker push (often HTTP 403 on Ablaze)..."
        $cfg = New-Md287DockerConfig -Registry $registry -User $id.User -Token $id.Token
        try {
            if (Invoke-Md287DockerPush -Remote $remote -ConfigDir $cfg) {
                $how = "docker"
            }
        } finally {
            Remove-Item $cfg -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
    if (-not $how) {
        if (Invoke-Md287SkopeoPush -Local $Local -Registry $registry -RepoTag $repoTag -User $id.User -Token $id.Token) {
            $how = "skopeo"
        }
    }
    if (-not $how) {
        throw @"
Could not push $Local to $remote.

Do not run docker login by hand (Windows Credential Manager truncates the OpenShift token).
Do not treat the Account Route HTML page as the registry.

Check:
  1. oc whoami is studentNN (not MSMICR26-NN / student.VLAB)
  2. oc project -q is md287-studentNN
  3. The image exists: docker images $Local
  4. `$env:MD287_REGISTRY is default-route-openshift-image-registry.apps.aro-md287.centralus.aroapp.io
  5. python --version works (the classroom push uses Python)
Raise a hand. Do not paste oc whoami -t into chat.
"@
    }

    Write-Host "Pushed $remote ($how)"

    $internal = "image-registry.openshift-image-registry.svc:5000/{0}/{1}:{2}" -f $id.Project, $Name, $Tag
    Update-Md287DeploymentImage -Name $Name -Project $id.Project -Internal $internal
    return $internal
}
