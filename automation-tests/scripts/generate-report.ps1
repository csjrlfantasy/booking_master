[CmdletBinding()]
param(
    [string]$ResultsDirectory,
    [string]$OutputDirectory,
    [switch]$Help
)
$ErrorActionPreference = 'Stop'
if ($Help) {
    Write-Output 'generate-report.ps1 -ResultsDirectory <run/allure-results> [-OutputDirectory <new-directory>]'
    Write-Output 'Requires an installed Allure CLI. Raw result files are not a generated HTML report.'
    exit 0
}
if ([string]::IsNullOrWhiteSpace($ResultsDirectory)) { throw 'ResultsDirectory is required.' }
$results = (Resolve-Path -LiteralPath $ResultsDirectory -ErrorAction Stop).Path
if (-not (Get-ChildItem -LiteralPath $results -Filter '*-result.json' -File)) { throw 'No Allure test result JSON files found.' }
$allure = Get-Command allure -ErrorAction SilentlyContinue
if (-not $allure) { throw 'Allure CLI is missing. Install it and retry; no HTML report was generated.' }
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path (Split-Path -Parent $results) ('allure-html-' + [guid]::NewGuid().ToString('N').Substring(0,12))
}
$output = [IO.Path]::GetFullPath($OutputDirectory)
if (Test-Path -LiteralPath $output) { throw 'OutputDirectory must be new; existing reports will not be overwritten.' }
& $allure.Source generate $results --output $output
$allureExitCode = $LASTEXITCODE
if ($allureExitCode -ne 0) { exit $allureExitCode }
if (-not (Test-Path -LiteralPath (Join-Path $output 'index.html') -PathType Leaf)) { throw 'Allure CLI returned success without index.html; report generation is incomplete.' }
Write-Output "Generated Allure report: $output"
exit 0
