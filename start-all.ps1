<#
.SYNOPSIS
  Starts the TicketMore web frontend and backend.
.DESCRIPTION
  Rebuilds the backend with JDK 17, waits for its health endpoint, and then
  starts the Vite frontend. Defaults: frontend 3000, backend 8088, API /api.
#>

param(
    [int]$BackendPort = 8088,
    [int]$FrontendPort = 3000,
    [int]$MetroPort = 8081,
    [switch]$SkipBackend,
    [switch]$SkipFrontend,
    [switch]$IncludeMetro
)

$ErrorActionPreference = "Stop"
$Root = $PSScriptRoot
$BackendJar = Join-Path $Root "booking-api\target\booking-api-1.0.0-SNAPSHOT.jar"
$FrontendDir = Join-Path $Root "frontend"
$MobileDir = Join-Path $Root "booking-app"
$LogDir = Join-Path $Root "logs"
$LocalEnvFile = Join-Path $Root ".env"

function New-LocalSecret {
    param([int]$ByteCount = 48)

    $bytes = New-Object byte[] $ByteCount
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
    return [Convert]::ToBase64String($bytes)
}

function Initialize-LocalEnvironment {
    param([string]$Path)

    if (Test-Path $Path) {
        return
    }

    $content = @(
        "# Local development configuration. This file is ignored by Git.",
        "# Set DB_PASSWORD when your local MySQL root account has a password.",
        "DB_PASSWORD=",
        "REDIS_PASSWORD=",
        "JWT_SECRET=$(New-LocalSecret)",
        "PAYMENT_CALLBACK_SECRET_KEY=$(New-LocalSecret)",
        "COURSE_TEST_KEY=",
        "COURSE_TEST_SUPPORT_ENABLED=false"
    )
    Set-Content -LiteralPath $Path -Value $content -Encoding ASCII
    Write-Host "[CONFIG] Created local runtime configuration: $Path" -ForegroundColor Yellow
}

function Import-LocalEnvironment {
    param([string]$Path)

    Initialize-LocalEnvironment -Path $Path
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith("#")) {
            continue
        }

        $parts = $trimmed.Split("=", 2)
        if ($parts.Count -ne 2) {
            throw "Invalid local environment entry in ${Path}: $line"
        }

        $name = $parts[0].Trim()
        $value = $parts[1].Trim()
        if ($name -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
            throw "Invalid local environment variable name in ${Path}: $name"
        }
        if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        if ($null -eq [Environment]::GetEnvironmentVariable($name, [EnvironmentVariableTarget]::Process)) {
            [Environment]::SetEnvironmentVariable($name, $value, [EnvironmentVariableTarget]::Process)
        }
    }
    Write-Host "[CONFIG] Loaded local runtime configuration: $Path" -ForegroundColor DarkGray
}

function Get-StartupLogTail {
    param(
        [string]$StandardOutputLog,
        [string]$ErrorLog
    )

    $availableLogs = @($StandardOutputLog, $ErrorLog) |
        Where-Object { $_ -and (Test-Path $_) } |
        Select-Object -Unique
    if (-not $availableLogs) {
        return "No startup log was created."
    }

    $sections = foreach ($log in $availableLogs) {
        "[$([IO.Path]::GetFileName($log))]"
        (Get-Content -LiteralPath $log -Tail 40) -join [Environment]::NewLine
    }
    return $sections -join [Environment]::NewLine
}

function Wait-HttpReady {
    param(
        [string]$ServiceName,
        [string]$Url,
        [int]$TimeoutSeconds,
        [System.Diagnostics.Process]$Process,
        [string]$StandardOutputLog,
        [string]$ErrorLog
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if ($Process -and $Process.HasExited) {
            $Process.WaitForExit()
            $tail = Get-StartupLogTail -StandardOutputLog $StandardOutputLog -ErrorLog $ErrorLog
            throw "$ServiceName exited with code $($Process.ExitCode).`n$tail"
        }

        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                return
            }
        } catch {
            Start-Sleep -Seconds 1
        }
    }

    $tail = Get-StartupLogTail -StandardOutputLog $StandardOutputLog -ErrorLog $ErrorLog
    throw "$ServiceName was not ready after $TimeoutSeconds seconds: $Url`n$tail"
}

function Test-HttpOk {
    param([string]$Url)

    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
        return $response.StatusCode -ge 200 -and $response.StatusCode -lt 500
    } catch {
        return $false
    }
}

New-Item -ItemType Directory -Path $LogDir -Force | Out-Null
Import-LocalEnvironment -Path $LocalEnvFile

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  TicketMore - one-click web startup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$backendProcess = $null
if (-not $SkipBackend) {
    $backendHealth = "http://127.0.0.1:$BackendPort/api/actuator/health"
    if (Test-HttpOk $backendHealth) {
        Write-Host "[BACKEND] Already running: $backendHealth" -ForegroundColor Green
    } else {
        $jdkCandidates = @(
            "C:\Program Files\Java\jdk-17",
            $env:JAVA_HOME
        ) | Where-Object { $_ -and (Test-Path (Join-Path $_ "bin\java.exe")) }

        if (-not $jdkCandidates) {
            throw "JDK 17 was not found. Install it or set JAVA_HOME."
        }

        $env:JAVA_HOME = $jdkCandidates[0]
        $env:PATH = "$(Join-Path $env:JAVA_HOME 'bin');$env:PATH"
        $javaExecutable = Join-Path $env:JAVA_HOME "bin\java.exe"
        $maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
        if (-not $maven) {
            throw "Maven (mvn.cmd) was not found on PATH."
        }

        Write-Host "[BACKEND] JDK: $env:JAVA_HOME" -ForegroundColor DarkGray
        Write-Host "[BACKEND] Rebuilding package to prevent stale dependencies..." -ForegroundColor Yellow
        & $maven.Source -pl booking-api -am package -DskipTests
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path $BackendJar)) {
            throw "Backend package failed. Check the Maven output above."
        }

        $backendOut = Join-Path $LogDir "backend.out.log"
        $backendErr = Join-Path $LogDir "backend.err.log"
        $backendArgs = @(
            "-jar",
            "`"$BackendJar`"",
            "--spring.profiles.active=dev",
            "--server.port=$BackendPort"
        )
        $backendProcess = Start-Process -FilePath $javaExecutable -ArgumentList $backendArgs -WorkingDirectory $Root -WindowStyle Hidden -RedirectStandardOutput $backendOut -RedirectStandardError $backendErr -PassThru
        Set-Content -Path (Join-Path $LogDir "backend.pid") -Value $backendProcess.Id -Encoding ASCII

        Write-Host "[BACKEND] Waiting for health check..." -ForegroundColor Yellow
        Wait-HttpReady -ServiceName "Backend" -Url $backendHealth -TimeoutSeconds 90 -Process $backendProcess -StandardOutputLog $backendOut -ErrorLog $backendErr
        Write-Host "[BACKEND] Ready: $backendHealth" -ForegroundColor Green
    }
}

$frontendProcess = $null
if (-not $SkipFrontend) {
    $frontendUrl = "http://127.0.0.1:$FrontendPort/shop"
    if (Test-HttpOk $frontendUrl) {
        Write-Host "[FRONTEND] Already running: $frontendUrl" -ForegroundColor Green
    } else {
        if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
            throw "Node.js was not found on PATH."
        }
        if (-not (Get-Command npm.cmd -ErrorAction SilentlyContinue)) {
            throw "npm.cmd was not found on PATH."
        }
        if (-not (Test-Path (Join-Path $FrontendDir "node_modules"))) {
            Write-Host "[FRONTEND] Installing dependencies..." -ForegroundColor Yellow
            Push-Location $FrontendDir
            try {
                & npm.cmd install
                if ($LASTEXITCODE -ne 0) { throw "Frontend dependency installation failed." }
            } finally {
                Pop-Location
            }
        }

        $env:VITE_DEV_PORT = "$FrontendPort"
        $env:VITE_PROXY_TARGET = "http://127.0.0.1:$BackendPort"
        $frontendOut = Join-Path $LogDir "frontend.out.log"
        $frontendErr = Join-Path $LogDir "frontend.err.log"
        $frontendCommand = "npm.cmd run dev -- --host 127.0.0.1 --port $FrontendPort"
        $frontendProcess = Start-Process -FilePath "cmd.exe" -ArgumentList @("/d", "/c", $frontendCommand) -WorkingDirectory $FrontendDir -WindowStyle Hidden -RedirectStandardOutput $frontendOut -RedirectStandardError $frontendErr -PassThru
        Set-Content -Path (Join-Path $LogDir "frontend.pid") -Value $frontendProcess.Id -Encoding ASCII

        Write-Host "[FRONTEND] Waiting for page..." -ForegroundColor Yellow
        Wait-HttpReady -ServiceName "Frontend" -Url $frontendUrl -TimeoutSeconds 45 -Process $frontendProcess -StandardOutputLog $frontendOut -ErrorLog $frontendErr
        Write-Host "[FRONTEND] Ready: $frontendUrl" -ForegroundColor Green
    }
}

if ($IncludeMetro) {
    if (-not (Test-Path (Join-Path $MobileDir "node_modules"))) {
        Push-Location $MobileDir
        try {
            & npm.cmd install
            if ($LASTEXITCODE -ne 0) { throw "Android dependency installation failed." }
        } finally {
            Pop-Location
        }
    }

    $metroOut = Join-Path $LogDir "metro.out.log"
    $metroErr = Join-Path $LogDir "metro.err.log"
    $metroCommand = "npm.cmd start -- --port $MetroPort"
    $metroProcess = Start-Process -FilePath "cmd.exe" -ArgumentList @("/d", "/c", $metroCommand) -WorkingDirectory $MobileDir -WindowStyle Hidden -RedirectStandardOutput $metroOut -RedirectStandardError $metroErr -PassThru
    Set-Content -Path (Join-Path $LogDir "metro.pid") -Value $metroProcess.Id -Encoding ASCII
    Write-Host "[ANDROID] Metro started: http://127.0.0.1:$MetroPort" -ForegroundColor Green
}

Write-Host ""
Write-Host "Startup completed" -ForegroundColor Cyan
Write-Host "  Customer site : http://127.0.0.1:$FrontendPort/shop"
Write-Host "  Admin login   : http://127.0.0.1:$FrontendPort/login"
Write-Host "  Backend API   : http://127.0.0.1:$BackendPort/api"
Write-Host "  Swagger UI    : http://127.0.0.1:$BackendPort/api/swagger-ui.html"
Write-Host "  Logs          : $LogDir"
