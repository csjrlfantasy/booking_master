# ================================================
# QA API自动化测试脚本
# 票务App API测试
# ================================================

$BASE_URL = "http://localhost:8080"
$API_PREFIX = "/api"

# 颜色输出
function Write-TestResult {
    param([string]$status, [string]$message)
    if ($status -eq "PASS") {
        Write-Host "[PASS] " -ForegroundColor Green -NoNewline
        Write-Host $message
    } elseif ($status -eq "FAIL") {
        Write-Host "[FAIL] " -ForegroundColor Red -NoNewline
        Write-Host $message
    } elseif ($status -eq "INFO") {
        Write-Host "[INFO] " -ForegroundColor Cyan -NoNewline
        Write-Host $message
    }
}

Write-Host ""
Write-Host "========== 票务App API测试 ==========" -ForegroundColor Yellow
Write-Host ""

# ================================================
# Step 1: 注册测试用户
# ================================================
Write-TestResult -status "INFO" -message "Step 1: 注册测试用户..."

$registerPayload = @{
    username = "testuser001"
    password = "Pass1234"
    phone = "13800000001"
} | ConvertTo-Json

$registerResponse = Invoke-RestMethod -Uri "$BASE_URL$API_PREFIX/user/register" `
    -Method Post `
    -ContentType "application/json" `
    -Body $registerPayload

if ($registerResponse.code -eq 0) {
    Write-TestResult -status "PASS" -message "用户注册成功 (userId: $($registerResponse.data))"
} elseif ($registerResponse.code -eq 100101) {
    Write-TestResult -status "INFO" -message "用户已存在，跳过注册"
} else {
    Write-TestResult -status "FAIL" -message "注册失败: $($registerResponse.message)"
}

# ================================================
# Step 2: 登录获取Token
# ================================================
Write-TestResult -status "INFO" -message "Step 2: 登录获取Token..."

$loginPayload = @{
    username = "testuser001"
    password = "Pass1234"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri "$BASE_URL$API_PREFIX/user/login" `
    -Method Post `
    -ContentType "application/json" `
    -Body $loginPayload

if ($loginResponse.code -eq 0) {
    $TOKEN = $loginResponse.data.token
    Write-TestResult -status "PASS" -message "登录成功，获取Token"
} else {
    Write-TestResult -status "FAIL" -message "登录失败: $($loginResponse.message)"
    exit 1
}

Write-Host ""

# ================================================
# Step 3: 测试票务相关API
# ================================================
Write-TestResult -status "INFO" -message "Step 3: 测试票务模块API..."
Write-Host ""

# 测试票务列表
Write-Host "--- GET /api/tickets ---" -ForegroundColor Gray
try {
    $headers = @{"Authorization"="Bearer $TOKEN"}
    $ticketsResponse = Invoke-RestMethod -Uri "$BASE_URL$API_PREFIX/tickets" `
        -Method Get `
        -Header $headers
    
    if ($ticketsResponse.code -eq 0) {
        $total = $ticketsResponse.data.total
        Write-TestResult -status "PASS" -message "票务列表: 共 $total 条记录"
    } else {
        Write-TestResult -status "FAIL" -message "错误: $($ticketsResponse.message)"
    }
} catch {
    Write-TestResult -status "FAIL" -message "请求失败: $($_.Exception.Message)"
}

# 测试票务详情
Write-Host ""
Write-Host "--- GET /api/tickets/{id} ---" -ForegroundColor Gray
try {
    $headers = @{"Authorization"="Bearer $TOKEN"}
    $ticketDetailResponse = Invoke-RestMethod -Uri "$BASE_URL$API_PREFIX/tickets/1" `
        -Method Get `
        -Header $headers
    
    if ($ticketDetailResponse.code -eq 0) {
        Write-TestResult -status "PASS" -message "票务详情: $($ticketDetailResponse.data.name)"
    } elseif ($ticketDetailResponse.code -eq 404) {
        Write-TestResult -status "INFO" -message "票务不存在(无测试数据): $($ticketDetailResponse.message)"
    } else {
        Write-TestResult -status "FAIL" -message "错误: $($ticketDetailResponse.message)"
    }
} catch {
    Write-TestResult -status "FAIL" -message "请求失败: $($_.Exception.Message)"
}

# 测试分类
Write-Host ""
Write-Host "--- GET /api/categories ---" -ForegroundColor Gray
try {
    $headers = @{"Authorization"="Bearer $TOKEN"}
    $categoriesResponse = Invoke-RestMethod -Uri "$BASE_URL$API_PREFIX/categories" `
        -Method Get `
        -Header $headers
    
    if ($categoriesResponse.code -eq 0) {
        Write-TestResult -status "PASS" -message "分类列表: $($categoriesResponse.data.Count) 个分类"
    } else {
        Write-TestResult -status "FAIL" -message "错误: $($categoriesResponse.message)"
    }
} catch {
    Write-TestResult -status "FAIL" -message "请求失败: $($_.Exception.Message)"
}

# ================================================
# 测试结果汇总
# ================================================
Write-Host ""
Write-Host "========== 测试完成 ==========" -ForegroundColor Yellow
Write-Host ""
Write-Host "Token: $($TOKEN.Substring(0, 20))..." -ForegroundColor Cyan
Write-Host ""
Write-Host "后续测试请使用以下命令:" -ForegroundColor Cyan
Write-Host "  curl -H 'Authorization: Bearer $TOKEN' $BASE_URL$API_PREFIX/tickets" -ForegroundColor Gray
Write-Host ""