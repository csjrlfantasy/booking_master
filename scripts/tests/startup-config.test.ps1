$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$failures = New-Object System.Collections.Generic.List[string]

function Assert-Contains {
    param(
        [string]$Path,
        [string]$Pattern,
        [string]$Message
    )

    $content = Get-Content -Raw -Encoding UTF8 (Join-Path $root $Path)
    if ($content -notmatch $Pattern) {
        $failures.Add($Message)
    }
}

Assert-Contains "booking-api/src/main/resources/application.yml" 'port:\s*8088' "Backend default port must be 8088."
Assert-Contains "frontend/vite.config.ts" "VITE_PROXY_TARGET \|\| 'http://127\.0\.0\.1:8088'" "Frontend proxy must default to backend port 8088."
Assert-Contains "start-all.ps1" '\[int\]\$BackendPort\s*=\s*8088' "One-click script must default the backend to 8088."
Assert-Contains "start-all.ps1" '\[int\]\$FrontendPort\s*=\s*3000' "One-click script must default the frontend to 3000."
Assert-Contains "start-all.ps1" '-pl\s+booking-api\s+-am\s+package\s+-DskipTests' "One-click script must build a fresh backend package."
Assert-Contains "start-all.ps1" '"-jar"' "One-click script must launch the packaged Spring Boot jar directly."
Assert-Contains "start-all.ps1" 'Import-LocalEnvironment' "One-click script must load local runtime environment values before starting the backend."
Assert-Contains "start-all.ps1" '\.env' "One-click script must support an ignored local .env configuration file."
Assert-Contains "start-all.ps1" 'WaitForExit\(\)' "One-click script must refresh a failed child process before reporting its exit code."
Assert-Contains "start-all.ps1" 'StandardOutputLog' "One-click script must include backend standard output in startup failure details."
Assert-Contains "README.md" 'http://127\.0\.0\.1:8088/api' "README must document the direct backend API address."
Assert-Contains "README.md" 'http://127\.0\.0\.1:3000/shop' "README must document the customer frontend address."

$tokens = $null
$parseErrors = $null
[System.Management.Automation.Language.Parser]::ParseFile((Join-Path $root "start-all.ps1"), [ref]$tokens, [ref]$parseErrors) | Out-Null
if ($parseErrors.Count -gt 0) {
    $failures.Add("One-click script must be valid PowerShell: $($parseErrors[0].Message)")
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Host "[PASS] Startup configuration is consistent." -ForegroundColor Green
