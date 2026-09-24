[CmdletBinding(SupportsShouldProcess)]
param([ValidateSet('offline', 'deploy', 'public')][string]$Mode = 'offline')
$ErrorActionPreference = 'Stop'
function Require-Value([string]$Name) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($Name))) {
        throw "Required configuration is missing: $Name"
    }
}
function Require-Flag([string]$Name) {
    if ([Environment]::GetEnvironmentVariable($Name) -cne 'true') {
        throw "Required authorization is not enabled: $Name"
    }
}
if ($Mode -eq 'offline') {
    Write-Output 'Offline quality gates selected; no server credentials or business requests.'
    return
}
if ($Mode -eq 'deploy') { Require-Flag 'DEPLOY_APP' }
if ($Mode -eq 'public') { Require-Flag 'RUN_PUBLIC_TESTS' }
# Even deployment requires cleanup readiness; public validation includes writes.
foreach ($name in @('MIGRATION_ACK', 'TEST_SUPPORT_READY', 'ALLOW_WRITE_TESTS', 'ALLOW_DESTRUCTIVE_COMMERCE')) {
    Require-Flag $name
}
foreach ($name in @('COURSE_TEST_KEY', 'BOOKING_ADMIN_USERNAME', 'BOOKING_ADMIN_PASSWORD', 'BOOKING_MIGRATION_EVIDENCE')) {
    Require-Value $name
}
if ($Mode -eq 'deploy') {
    foreach ($name in @('BOOKING_SSH_KEY', 'BOOKING_KNOWN_HOSTS', 'BOOKING_SSH_USER')) { Require-Value $name }
    foreach ($name in @('BOOKING_SSH_KEY', 'BOOKING_KNOWN_HOSTS')) {
        if (-not (Test-Path -LiteralPath ([Environment]::GetEnvironmentVariable($name)) -PathType Leaf)) {
            throw "Required credential file is unavailable: $name"
        }
    }
    if ($env:BOOKING_SSH_USER -notmatch '^[a-z_][a-z0-9_-]{0,31}$' -or $env:BOOKING_SSH_USER -eq 'root') {
        throw 'A dedicated non-root SSH deployment account is required.'
    }
}
try {
    $proof = Get-Content -LiteralPath $env:BOOKING_MIGRATION_EVIDENCE -Raw | ConvertFrom-Json -ErrorAction Stop
    $auditedAt = [DateTimeOffset]::Parse($proof.auditedAtUtc).ToUniversalTime()
} catch { throw 'Required migration evidence file is unreadable or malformed.' }
$age = [DateTimeOffset]::UtcNow - $auditedAt
if ($proof.environment -cne 'public' -or $proof.inventoryRestoredColumn -isnot [bool] -or
    -not $proof.inventoryRestoredColumn -or $proof.unknownInventoryRestoredRows -isnot [long] -or
    $proof.unknownInventoryRestoredRows -ne 0 -or
    [string]::IsNullOrWhiteSpace($proof.backupReference) -or
    [string]::IsNullOrWhiteSpace($proof.auditedBy) -or $age.TotalHours -gt 24 -or $age.TotalMinutes -lt -5) {
    throw 'Required migration evidence must confirm audited zero unknown rows and a backup within 24 hours.'
}
Write-Output 'Prerequisite assertions validated. Live cleanup preflight and remote verification remain mandatory.'
