# =============================================================================
# 票务App - 快速启动脚本
# =============================================================================
# 用法:
#   .\quick-start.ps1                编译 + 启动 (端口3000)
#   .\quick-start.ps1 -Port 8088     指定端口
#   .\quick-start.ps1 -Frontend      同时启动前端
#   .\quick-start.ps1 -SkipCompile   跳过编译 (前提:已编译过)
# =============================================================================
param(
    [switch]$Frontend,
    [switch]$SkipCompile,
    [int]$Port = 3000
)

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot
$BackendDir  = "$ProjectRoot\booking-api"
$FrontendDir = "$ProjectRoot\frontend"

# =============================================================================
# 1. Banner
# =============================================================================
Write-Host @"

========================================
  TicketMore Booking API - Quick Start
========================================
"@ -ForegroundColor Cyan

# =============================================================================
# 2. 查找 JDK 17+
# =============================================================================
$JdkCandidates = @(
    "C:\Program Files\Java\jdk-21.0.10",
    "C:\Program Files\Java\jdk-21",
    "C:\Program Files\Java\jdk-17",
    "C:\Program Files\Java\latest"
)

$JAVA_HOME = $null
foreach ($c in $JdkCandidates) {
    if (Test-Path "$c\bin\java.exe") { $JAVA_HOME = $c; break }
}
if (-not $JAVA_HOME) { $JAVA_HOME = $env:JAVA_HOME }
if (-not $JAVA_HOME) {
    Write-Host "[ERROR] JDK 17+ not found. Install JDK 17 or 21." -ForegroundColor Red
    exit 1
}

$env:JAVA_HOME = $JAVA_HOME
$env:PATH      = "$JAVA_HOME\bin;$env:PATH"
Write-Host "[JDK] $JAVA_HOME" -ForegroundColor Green
(& java -version 2>&1 | Select-Object -First 1) -match "version `"(.+)`"" | Out-Null
Write-Host "      $($Matches[1])"

# =============================================================================
# 3. 查找 Maven
# =============================================================================
$mvn = $null
@("mvn", "mvn.cmd", "D:\software\apache-maven-3.8.8-bin\apache-maven-3.8.8\bin\mvn.cmd") | ForEach-Object {
    if (-not $mvn -and (Get-Command $_ -ErrorAction SilentlyContinue)) { $mvn = $_ }
}
if (-not $mvn) {
    Write-Host "[WARN] Maven not found - will skip compile" -ForegroundColor Yellow
    $SkipCompile = $true
} else {
    Write-Host "[MVN] $mvn" -ForegroundColor Green
}

# =============================================================================
# 4. 依赖检查
# =============================================================================
Write-Host "[DEPS] Checking MySQL..." -ForegroundColor Gray
$cfg = "$BackendDir\src\main\resources\application-dev.yml"
$mysqlHost, $mysqlPort = "127.0.0.1", 3306
if (Test-Path $cfg) {
    $c = Get-Content $cfg -Raw
    if ($c -match "jdbc:mysql://([^:]+):(\d+)") { $mysqlHost = $Matches[1]; $mysqlPort = $Matches[2] }
}
$tcp = Test-NetConnection -ComputerName $mysqlHost -Port $mysqlPort -WarningAction SilentlyContinue -InformationLevel Quiet
if ($tcp) { Write-Host "[DEPS] MySQL OK (${mysqlHost}:${mysqlPort})" -ForegroundColor Green }
else       { Write-Host "[DEPS] MySQL NOT reachable - startup may fail!" -ForegroundColor Yellow }

# =============================================================================
# 5. 编译
# =============================================================================
if (-not $SkipCompile -and $mvn) {
    Write-Host "[BUILD] Compiling..." -ForegroundColor Yellow
    Set-Location $ProjectRoot

    # booking-common
    & $mvn compile -pl booking-common -DskipTests -q 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[BUILD] FAILED - booking-common" -ForegroundColor Red
        exit 1
    }

    # booking-api (先尝试增量编译，避免 clean 文件锁定)
    & $mvn compile -pl booking-api -DskipTests -q 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[BUILD] Incremental compile failed, trying clean..." -ForegroundColor Yellow
        & $mvn clean compile -pl booking-api -DskipTests -q 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Host "[BUILD] FAILED - booking-api" -ForegroundColor Red
            exit 1
        }
    }
    Write-Host "[BUILD] Done" -ForegroundColor Green
}

# =============================================================================
# 6. 修复 target 中的 Swagger 配置 (逐行替换，稳健)
# =============================================================================
$targetCfg = "$BackendDir\target\classes\application-dev.yml"
if (Test-Path $targetCfg) {
    $lines  = Get-Content $targetCfg -Encoding UTF8
    $fixed  = $false
    $inSpringdoc = $false
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match "^springdoc:") { $inSpringdoc = $true }
        elseif ($lines[$i] -match "^(?!springdoc|  |\t)") { $inSpringdoc = $false }
        if ($inSpringdoc -and $lines[$i] -match "^\s+enabled:\s*false") {
            $lines[$i] = $lines[$i] -replace "false", "true"
            $fixed = $true
        }
    }
    if ($fixed) {
        Set-Content -Path $targetCfg -Value ($lines -join "`r`n") -Encoding UTF8
        Write-Host "[CFG] Swagger enabled=true (fixed in target)" -ForegroundColor Green
    } else {
        Write-Host "[CFG] Swagger already enabled" -ForegroundColor Green
    }
}

# =============================================================================
# 7. 启动
# =============================================================================
Write-Host @"

========================================
  Starting backend on port $Port ...
  (press Ctrl+C to stop)
========================================

  Swagger UI   : http://localhost:$Port/api/swagger-ui.html
  OpenAPI JSON : http://localhost:$Port/api/v3/api-docs
  Health Check : http://localhost:$Port/api/actuator/health

"@ -ForegroundColor Cyan

Set-Location $ProjectRoot
& $mvn spring-boot:run `
    -pl booking-api `
    -Dspring-boot.run.jvmArguments="-Dserver.port=$Port -Dspring.profiles.active=dev" `
    -DskipTests

# =============================================================================
# 8. (可选) 启动前端 — 在后端 Ctrl+C 后执行
# =============================================================================
if ($Frontend) {
    Write-Host ""
    Write-Host "[FRONTEND] Starting admin UI..." -ForegroundColor Yellow
    if (Test-Path "$FrontendDir\package.json") {
        Set-Location $FrontendDir
        npm run dev
    } else {
        Write-Host "[FRONTEND] frontend/package.json not found!" -ForegroundColor Red
    }
}
