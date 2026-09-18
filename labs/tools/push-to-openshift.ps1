# Shared helper: push a local Docker image to the ARO integrated registry.
# Dot-source from Lab 4 / Lab 5 push scripts. Do not run this file by itself.

$script:Md287RegistryDefault = "default-route-openshift-image-registry.apps.aro-md287.centralus.aroapp.io"
$script:Md287PushImagePy = Join-Path $PSScriptRoot "push_image.py"

function Get-Md287OcIdentity {
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
    $env:DOCKER_CONFIG = $ConfigDir
    $ErrorActionPreference = "Continue"
    try {
        docker --config $ConfigDir --disable-content-trust push $Remote
        return ($LASTEXITCODE -eq 0)
    } finally {
        $ErrorActionPreference = $oldEap
        if ($null -ne $oldCfg) { $env:DOCKER_CONFIG = $oldCfg } else { Remove-Item Env:DOCKER_CONFIG -ErrorAction SilentlyContinue }
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

function Invoke-Md287OcImageMirror {
    param(
        [Parameter(Mandatory = $true)][string]$Local,
        [Parameter(Mandatory = $true)][string]$Registry,
        [Parameter(Mandatory = $true)][string]$RepoTag
    )
    Write-Host "Trying oc image mirror (uses the oc token, not Docker Credential Manager)..."
    $oldEap = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        oc image mirror --insecure=true --keep-manifest-list=false "docker-daemon:$Local" "${Registry}/${RepoTag}"
        return ($LASTEXITCODE -eq 0)
    } finally {
        $ErrorActionPreference = $oldEap
    }
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
    Write-Host "Trying Python registry push (skips TLS verify; avoids Docker Credential Manager)..."
    $env:OC_USER = $User
    $env:OC_TOKEN = $Token
    $oldEap = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        python $py $Local $Registry $RepoTag
        return ($LASTEXITCODE -eq 0)
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
    $exists = oc get is $Name -n $Project --ignore-not-found 2>$null
    if (-not $exists) {
        oc create is $Name -n $Project 2>$null | Out-Host
    }
}

function Push-Md287Image {
    param(
        [Parameter(Mandatory = $true)][string]$Local,
        [Parameter(Mandatory = $true)][string]$Name,
        [string]$Tag = "1.0.0"
    )
    if (-not (Get-Command oc -ErrorAction SilentlyContinue)) { throw "oc is not on PATH" }
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw "docker is not on PATH" }

    docker image inspect $Local | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Local image $Local not found. Build it first (Lab 4 Step 2 / Lab 5 Step 7)."
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

    $cfg = New-Md287DockerConfig -Registry $registry -User $id.User -Token $id.Token
    try {
        $pushed = Invoke-Md287DockerPush -Remote $remote -ConfigDir $cfg
        if (-not $pushed) {
            Write-Host "docker push with isolated login failed; trying username unused..."
            $cfgUnused = New-Md287DockerConfig -Registry $registry -User "unused" -Token $id.Token
            try {
                $pushed = Invoke-Md287DockerPush -Remote $remote -ConfigDir $cfgUnused
            } finally {
                Remove-Item $cfgUnused -Recurse -Force -ErrorAction SilentlyContinue
            }
        }
        if (-not $pushed) {
            Write-Host "docker push with isolated login failed; trying oc registry login..."
            oc registry login --registry $registry --to (Join-Path $cfg "config.json") --insecure --skip-check 2>$null | Out-Host
            $pushed = Invoke-Md287DockerPush -Remote $remote -ConfigDir $cfg
        }
        if ($pushed) {
            Write-Host "Pushed $remote (docker)"
        } elseif (Invoke-Md287SkopeoPush -Local $Local -Registry $registry -RepoTag $repoTag -User $id.User -Token $id.Token) {
            Write-Host "Pushed $remote (skopeo)"
        } elseif (Invoke-Md287OcImageMirror -Local $Local -Registry $registry -RepoTag $repoTag) {
            Write-Host "Pushed $remote (oc image mirror)"
        } elseif (Invoke-Md287PythonPush -Local $Local -Registry $registry -RepoTag $repoTag -User $id.User -Token $id.Token) {
            Write-Host "Pushed $remote (python)"
        } else {
            throw @"
Could not push $Local to $remote.

Ablaze Docker Desktop often returns HTTP 403 against the ARO registry because Windows Credential Manager truncates the OpenShift token. This script already tried an isolated docker login, oc image mirror, then Python.

Check:
  1. oc whoami is studentNN (not MSMICR26-NN / student.VLAB)
  2. oc project -q is md287-studentNN
  3. The image exists: docker images $Local
  4. IIS exposed the registry Route $registry
  5. git pull from C:\Users\student.VLAB\MD287 then re-run this script
The Account Route HTML "Application is not available" means pods are not Ready yet — usually because this push has not succeeded.
Raise a hand. Do not paste oc whoami -t into chat.
"@
        }
    } finally {
        Remove-Item $cfg -Recurse -Force -ErrorAction SilentlyContinue
    }

    $internal = "image-registry.openshift-image-registry.svc:5000/{0}/{1}:{2}" -f $id.Project, $Name, $Tag
    $deploy = oc get deploy $Name -n $id.Project --ignore-not-found
    if ($deploy) {
        oc set image "deploy/$Name" "${Name}=$internal" -n $id.Project | Out-Host
        Write-Host "Pointed deploy/$Name at $internal"
    }
    return $internal
}
