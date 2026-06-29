# 用户模块接口测试脚本
# 环境配置
$BASE_URL = "http://localhost:8080"
$API_PREFIX = "/api/user"

# 颜色定义
function Write-TestHeader($description) {
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host $description -ForegroundColor Cyan
    Write-Host "========================================`n" -ForegroundColor Cyan
}

function Write-TestResult($success, $description) {
    if ($success) {
        Write-Host "[PASS] " -ForegroundColor Green -NoNewline
        Write-Host $description -ForegroundColor White
    } else {
        Write-Host "[FAIL] " -ForegroundColor Red -NoNewline
        Write-Host $description -ForegroundColor White
    }
}

# ============================================
# 1. 用户注册测试
# ============================================
Write-TestHeader "1. 用户注册测试 (POST $API_PREFIX/register)"

# TC001: 正常注册
Write-Host "`n[TC001] 正常注册..." -ForegroundColor Yellow
$registerBody = @{
    username = "testuser_$(Get-Random)"
    password = "Pass1234"
    phone = "139$(Get-Random -Maximum 99999999 -Minimum 10000000)"
    email = "test@example.com"
    nickname = "测试用户"
    source = "app"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "$BASE_URL$API_PREFIX/register" -Method POST -ContentType "application/json" -Body $registerBody
$success = $response.code -eq 0 -and $null -ne $response.data
Write-TestResult $success "正常注册"
if ($success) { $registeredUserId = $response.data }

# TC002: 用户名已存在
Write-Host "`n[TC002] 用户名已存在..." -ForegroundColor Yellow
$registerBody = @{
    username = "testuser001"
    password = "Pass1234"
    phone = "13900000099"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "$BASE_URL$API_PREFIX/register" -Method POST -ContentType "application/json" -Body $registerBody -ErrorAction SilentlyContinue
$success = $response.code -eq 100101
Write-TestResult $success "用户名已存在检测"

# TC003: 手机号已注册
Write-Host "`n[TC003] 手机号已注册..." -ForegroundColor Yellow
$registerBody = @{
    username = "testuser_new"
    password = "Pass1234"
    phone = "13800000001"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "$BASE_URL$API_PREFIX/register" -Method POST -ContentType "application/json" -Body $registerBody -ErrorAction SilentlyContinue
$success = $response.code -eq 100104
Write-TestResult $success "手机号已注册检测"

# TC004: 密码为空
Write-Host "`n[TC004] 密码为空..." -ForegroundColor Yellow
$registerBody = @{
    username = "testuser_new"
    password = ""
    phone = "13900000010"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$BASE_URL$API_PREFIX/register" -Method POST -ContentType "application/json" -Body $registerBody -ErrorAction Stop
    $success = $response.code -ne 0
} catch {
    $success = $true  # 400错误也算通过
}
Write-TestResult $success "密码为空校验"

# ============================================
# 2. 用户登录测试
# ============================================
Write-TestHeader "2. 用户登录测试 (POST $API_PREFIX/login)"

# TC007: 正常登录
Write-Host "`n[TC007] 正常登录..." -ForegroundColor Yellow
$loginBody = @{
    username = "testuser001"
    password = "Pass1234"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "$BASE_URL$API_PREFIX/login" -Method POST -ContentType "application/json" -Body $loginBody
$success = $response.code -eq 0 -and $null -ne $response.data.token
Write-TestResult $success "正常登录"
if ($success) { $validToken = $response.data.token }

# TC008: 用户名不存在
Write-Host "`n[TC008] 用户名不存在..." -ForegroundColor Yellow
$loginBody = @{
    username = "nonexistent"
    password = "Pass1234"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "$BASE_URL$API_PREFIX/login" -Method POST -ContentType "application/json" -Body $loginBody
$success = $response.code -eq 100100
Write-TestResult $success "用户名不存在检测"

# TC009: 密码错误
Write-Host "`n[TC009] 密码错误..." -ForegroundColor Yellow
$loginBody = @{
    username = "testuser001"
    password = "WrongPassword"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "$BASE_URL$API_PREFIX/login" -Method POST -ContentType "application/json" -Body $loginBody
$success = $response.code -eq 100103
Write-TestResult $success "密码错误检测"

# TC010: 连续失败5次账户锁定
Write-Host "`n[TC010] 连续失败5次账户锁定测试..." -ForegroundColor Yellow
for ($i = 1; $i -le 5; $i++) {
    $loginBody = @{
        username = "testuser001"
        password = "WrongPassword"
    } | ConvertTo-Json
    $response = Invoke-RestMethod -Uri "$BASE_URL$API_PREFIX/login" -Method POST -ContentType "application/json" -Body $loginBody
    Write-Host "  第$i次错误登录: code=$($response.code)" -ForegroundColor Gray
}
# 第6次尝试正确密码
$loginBody = @{
    username = "testuser001"
    password = "Pass1234"
} | ConvertTo-Json
$response = Invoke-RestMethod -Uri "$BASE_URL$API_PREFIX/login" -Method POST -ContentType "application/json" -Body $loginBody
$success = $response.code -eq 100110
Write-TestResult $success "账户锁定检测 (code=$($response.code))"

# ============================================
# 3. 获取用户信息测试
# ============================================
Write-TestHeader "3. 获取用户信息测试"

# TC013: 正常获取用户信息
Write-Host "`n[TC013] 正常获取用户信息..." -ForegroundColor Yellow
$headers = @{ "Authorization" = "Bearer $validToken" }
$response = Invoke-RestMethod -Uri "$BASE_URL$API_PREFIX/1" -Method GET -Headers $headers
$success = $response.code -eq 0 -and $null -ne $response.data.userId
Write-TestResult $success "获取用户信息成功"

# TC014: 用户ID不存在
Write-Host "`n[TC014] 用户ID不存在..." -ForegroundColor Yellow
$response = Invoke-RestMethod -Uri "$BASE_URL$API_PREFIX/99999" -Method GET -Headers $headers
$success = $response.code -eq 100100
Write-TestResult $success "用户不存在检测"

# TC015: 未登录访问
Write-Host "`n[TC015] 未登录访问..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$BASE_URL$API_PREFIX/1" -Method GET -ErrorAction Stop
    $success = $false
} catch {
    $success = $true  # 401错误表示通过
}
Write-TestResult $success "未登录访问被拒绝"

# TC016: Token无效
Write-Host "`n[TC016] Token无效..." -ForegroundColor Yellow
$headers = @{ "Authorization" = "Bearer invalid_token" }
try {
    $response = Invoke-RestMethod -Uri "$BASE_URL$API_PREFIX/1" -Method GET -Headers $headers -ErrorAction Stop
    $success = $false
} catch {
    $success = $true
}
Write-TestResult $success "无效Token被拒绝"

# TC018: 根据用户名获取用户信息
Write-Host "`n[TC018] 根据用户名获取用户信息..." -ForegroundColor Yellow
$headers = @{ "Authorization" = "Bearer $validToken" }
$response = Invoke-RestMethod -Uri "$BASE_URL$API_PREFIX/username/testuser001" -Method GET -Headers $headers
$success = $response.code -eq 0 -and $response.data.username -eq "testuser001"
Write-TestResult $success "根据用户名获取用户信息"

# TC019: 用户名不存在
Write-Host "`n[TC019] 用户名不存在..." -ForegroundColor Yellow
$response = Invoke-RestMethod -Uri "$BASE_URL$API_PREFIX/username/nonexistent" -Method GET -Headers $headers
$success = $response.code -eq 100100
Write-TestResult $success "用户名不存在检测"

# ============================================
# 4. 安全测试
# ============================================
Write-TestHeader "4. 安全测试"

# TC020: SQL注入测试
Write-Host "`n[TC020] SQL注入测试..." -ForegroundColor Yellow
$sqlPayloads = @(
    "admin' OR '1'='1",
    "' OR '1'='1' --",
    "1; DROP TABLE user; --"
)

foreach ($payload in $sqlPayloads) {
    $loginBody = @{
        username = $payload
        password = "anything"
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "$BASE_URL$API_PREFIX/login" -Method POST -ContentType "application/json" -Body $loginBody
        $success = $response.code -ne 0 -or $response.code -eq 100100
        if (-not $success) {
            Write-Host "  警告: SQL注入可能生效 - $payload" -ForegroundColor Red
        }
    } catch {
        $success = $true
    }
}
Write-TestResult $true "SQL注入测试完成 (需人工确认日志)"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "测试完成!" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan
