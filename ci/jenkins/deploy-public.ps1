[CmdletBinding(SupportsShouldProcess)]
param(
    [string]$Jar,
    [string]$FrontendArchive,
    [string]$ReleaseId,
    [switch]$Execute
)
$ErrorActionPreference = 'Stop'
if ($ReleaseId -notmatch '^[0-9]{1,12}-[a-f0-9]{7,40}$') { throw 'ReleaseId must contain a build number and commit hash.' }
& (Join-Path $PSScriptRoot 'verify-prerequisites.ps1') -Mode deploy
foreach ($artifact in @($Jar, $FrontendArchive)) {
    if ([string]::IsNullOrWhiteSpace($artifact) -or -not (Test-Path -LiteralPath $artifact -PathType Leaf)) {
        throw 'Both built deployment artifacts are required.'
    }
}
$jarPath = (Resolve-Path -LiteralPath $Jar).Path
$frontendPath = (Resolve-Path -LiteralPath $FrontendArchive).Path
$jarHash = (Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash.ToLowerInvariant()
$frontendHash = (Get-FileHash -LiteralPath $frontendPath -Algorithm SHA256).Hash.ToLowerInvariant()
if (-not $Execute) {
    Write-Output 'Validated only. Supply -Execute to enable deployment.'
    return
}
if (-not $PSCmdlet.ShouldProcess('public teaching environment', 'Install and activate audited release')) { return }
Get-Command ssh, scp -ErrorAction Stop | Out-Null
$destination = $env:BOOKING_SSH_USER + '@154.12.20.250'
$sshOptions = @('-o', 'BatchMode=yes', '-o', 'StrictHostKeyChecking=yes', '-o', 'ConnectTimeout=10',
    '-o', ('UserKnownHostsFile=' + $env:BOOKING_KNOWN_HOSTS), '-i', $env:BOOKING_SSH_KEY)
# This operator-installed contract must exist and be audited before enabling deployment.
# It is NOT inferred from the repository's Windows developer startup scripts.
$entry = '/opt/booking-master/bin/release'
function Invoke-Release([string]$Action) {
    & ssh @sshOptions $destination $entry $Action $ReleaseId $jarHash $frontendHash *> $null
    if ($LASTEXITCODE -ne 0) { throw "Remote release action failed: $Action" }
}
# verify performs read-only runtime/DB checks; prepare creates a NEW staging directory.
Invoke-Release 'verify'
Invoke-Release 'prepare'
foreach ($item in @(@($jarPath, 'booking-api.jar'), @($frontendPath, 'frontend.tar.gz'))) {
    $target = $destination + ':/opt/booking-master/staging/' + $ReleaseId + '/' + $item[1]
    & scp @sshOptions $item[0] $target *> $null
    if ($LASTEXITCODE -ne 0) { throw 'Artifact transfer failed; active release was not changed.' }
}
Invoke-Release 'install'
$activationAttempted = $false
try {
    $activationAttempted = $true
    Invoke-Release 'activate'
    Invoke-Release 'health'
} catch {
    if ($activationAttempted) {
        try { Invoke-Release 'rollback' }
        catch { throw 'Deployment failed and rollback failed. Operator recovery is required.' }
    }
    throw 'Deployment failed; previous release restored by the audited rollback contract.'
}
Write-Output 'Remote activation and health contract completed; public business validation is still separate.'
