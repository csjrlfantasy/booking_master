[CmdletBinding()]
param(
    [ValidateSet('framework', 'api-read', 'api-write', 'customer-ui', 'admin-ui', 'smoke', 'e2e', 'full')]
    [string]$Suite = 'framework',
    [ValidateSet('local', 'public')][string]$Environment = 'local',
    [switch]$AllowWrite,
    [switch]$AllowDestructiveCommerce,
    [switch]$CollectOnly,
    [switch]$DryRun,
    [switch]$Help,
    [string]$OutputRoot = '',
    [string]$Python = 'python'
)
$ErrorActionPreference = 'Stop'
if ($Help) {
    Write-Output 'run-suite.ps1 -Suite framework|api-read|api-write|customer-ui|admin-ui|smoke|e2e|full -Environment local|public [-AllowWrite] [-AllowDestructiveCommerce] [-CollectOnly] [-DryRun]'
    Write-Output 'DryRun prints argument arrays only. Write suites need COURSE_TEST_KEY; admin suites need BOOKING_ADMIN_USERNAME and BOOKING_ADMIN_PASSWORD. E2E/full execute serially.'
    exit 0
}
$testRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($OutputRoot)) { $OutputRoot = Join-Path $testRoot 'report' }
$runName = '{0}-{1}-{2}' -f [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffffffZ'), $Environment, [guid]::NewGuid().ToString('N').Substring(0,12)
$runDirectory = Join-Path ([IO.Path]::GetFullPath($OutputRoot)) $runName
$testArguments = @('-m', 'pytest', '--env', $Environment, '-n', '0')
$needsWrite = $Suite -in @('api-write', 'customer-ui', 'admin-ui', 'e2e', 'full')
$needsAdmin = $Suite -in @('api-read', 'api-write', 'admin-ui', 'e2e', 'full')
$needsCommerce = $Suite -in @('e2e', 'full')
switch ($Suite) {
    'framework' { $testArguments += @('tests/framework') }
    'api-read' { $testArguments += @('tests/api', '-m', 'api and not write and not destructive_commerce') }
    'api-write' { $testArguments += @('tests/api', '-m', 'api and write and not destructive_commerce') }
    'customer-ui' { $testArguments += @('tests/ui/customer', '--browser', 'chromium', '-m', 'not destructive_commerce') }
    'admin-ui' { $testArguments += @('tests/ui/admin', '--browser', 'chromium', '-m', 'not destructive_commerce') }
    'smoke' { $testArguments += @('tests/ui/customer', '-m', 'smoke and cross_browser and not write', '--browser', 'chromium', '--browser', 'firefox', '--browser', 'webkit') }
    'e2e' { $testArguments += @('tests/e2e', '--browser', 'chromium') }
    'full' { $testArguments += @('tests', '--browser', 'chromium') }
}
if ($AllowWrite) { $testArguments += '--allow-write' }
if ($AllowDestructiveCommerce) { $testArguments += '--allow-destructive-commerce' }
if ($CollectOnly) { $testArguments += '--collect-only' }
$testArguments += @('--evidence-dir', $runDirectory, '--junitxml', (Join-Path $runDirectory 'junit.xml'),
                    '--html', (Join-Path $runDirectory 'pytest.html'), '--self-contained-html',
                    '--alluredir', (Join-Path $runDirectory 'allure-results'))
if ($DryRun) {
    Write-Output ($testArguments | ConvertTo-Json -Compress)
    exit 0
}
if (-not $CollectOnly) {
    if ($needsWrite -and -not $AllowWrite) { throw 'Selected suite requires explicit -AllowWrite.' }
    if ($needsCommerce -and -not $AllowDestructiveCommerce) { throw 'Selected suite requires explicit -AllowDestructiveCommerce after -AllowWrite.' }
    $required = @()
    if ($needsWrite) { $required += 'COURSE_TEST_KEY' }
    if ($needsAdmin) { $required += @('BOOKING_ADMIN_USERNAME', 'BOOKING_ADMIN_PASSWORD') }
    foreach ($name in $required) {
        if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
            throw "Required environment variable is missing: $name"
        }
    }
}
Get-Command $Python -ErrorAction Stop | Out-Null
Push-Location -LiteralPath $testRoot
try {
    & $Python @testArguments
    $pytestExitCode = $LASTEXITCODE
    Write-Output "Evidence directory: $runDirectory"
} finally {
    Pop-Location
}
exit $pytestExitCode
