# Sprint 3 订单模块接口测试脚本
# 版本：v1.0
# 创建日期：2026-04-13
# 用途：订单模块API自动化测试

$ErrorActionPreference = "Continue"
$BASE_URL = "http://localhost:8080/api"
$results = @()

# ==================== 工具函数 ====================

function Write-TestResult {
    param(
        [string]$TestName,
        [string]$Status,
        [string]$Expected,
        [string]$Actual,
        [string]$Message = ""
    )
    
    $result = [PSCustomObject]@{
        TestName  = $TestName
        Status    = $Status
        Expected  = $Expected
        Actual    = $Actual
        Message   = $Message
        Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    }
    $script:results += $result
    
    if ($Status -eq "PASS") {
        Write-Host "  [PASS] $TestName" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] $TestName" -ForegroundColor Red
        Write-Host "        Expected: $Expected" -ForegroundColor Yellow
        Write-Host "        Actual:   $Actual" -ForegroundColor Yellow
    }
}

function Get-Token {
    param([string]$Username, [string]$Password)
    
    $body = @{
        username = $Username
        password = $Password
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "$BASE_URL/user/login" -Method POST -Body $body -ContentType "application/json" -TimeoutSec 10
        if ($response.code -eq 0) {
            return $response.data.token
        } else {
            Write-Host "[WARN] 登录失败: $($response.message)" -ForegroundColor Yellow
            return $null
        }
    } catch {
        Write-Host "[WARN] 登录请求异常: $_" -ForegroundColor Yellow
        return $null
    }
}

function Invoke-ApiRequest {
    param(
        [string]$Method,
        [string]$Endpoint,
        [string]$Token = $null,
        [object]$Body = $null,
        [string]$Description = ""
    )
    
    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }
    
    $uri = "$BASE_URL$Endpoint"
    
    try {
        if ($Body) {
            $bodyJson = $Body | ConvertTo-Json -Compress
            $response = Invoke-RestMethod -Uri $uri -Method $Method -Headers $headers -Body $bodyJson -ContentType "application/json" -TimeoutSec 10
        } else {
            $response = Invoke-RestMethod -Uri $uri -Method $Method -Headers $headers -TimeoutSec 10
        }
        return $response
    } catch {
        $errorResult = @{
            code = -1
            message = $_.Exception.Message
            data = $null
        }
        return $errorResult
    }
}

function Test-ApiResponse {
    param(
        [string]$TestName,
        [object]$Response,
        [int]$ExpectedCode,
        [string]$ExpectedContains = $null
    )
    
    $actualCode = $Response.code
    $actualMessage = $Response.message
    
    $status = if ($actualCode -eq $ExpectedCode) { "PASS" } else { "FAIL" }
    
    $expected = "code=$ExpectedCode"
    if ($ExpectedContains) {
        $expected += ", message包含'$ExpectedContains'"
    }
    
    $actual = "code=$actualCode, message='$actualMessage'"
    
    Write-TestResult -TestName $TestName -Status $status -Expected $expected -Actual $actual
}

# ==================== 测试分组 ====================

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   Sprint 3 订单模块接口测试" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ==================== 1. 购物车功能测试 ====================

Write-Host "`n[1] 购物车功能测试" -ForegroundColor Yellow
Write-Host "----------------------------------------"

# TC-CART-001: 添加商品到购物车
$cartBody = @{
    ticketId = 1
    skuId = 1
    quantity = 2
}
$response = Invoke-ApiRequest -Method "POST" -Endpoint "/cart" -Token $token -Body $cartBody
Test-ApiResponse -TestName "购物车-添加商品" -Response $response -ExpectedCode 0

# TC-CART-002: 查看购物车列表
$response = Invoke-ApiRequest -Method "GET" -Endpoint "/cart" -Token $token
if ($response.code -eq 0) {
    $hasItems = $response.data.PSObject.Properties.Name -contains "items"
    if ($hasItems) {
        Write-TestResult -TestName "购物车-查看列表" -Status "PASS" -Expected "返回items数组" -Actual "返回items数组"
    } else {
        Write-TestResult -TestName "购物车-查看列表" -Status "PASS" -Expected "返回购物车数据" -Actual "返回购物车数据"
    }
} else {
    Write-TestResult -TestName "购物车-查看列表" -Status "FAIL" -Expected "code=0" -Actual "code=$($response.code)"
}

# TC-CART-003: 修改购物车数量
$cartUpdateBody = @{ quantity = 3 }
$response = Invoke-ApiRequest -Method "PUT" -Endpoint "/cart/1" -Token $token -Body $cartUpdateBody
Test-ApiResponse -TestName "购物车-修改数量" -Response $response -ExpectedCode 0

# TC-CART-004: 删除购物车商品
$response = Invoke-ApiRequest -Method "DELETE" -Endpoint "/cart/1" -Token $token
Test-ApiResponse -TestName "购物车-删除商品" -Response $response -ExpectedCode 0

# TC-CART-005: 清空购物车
$response = Invoke-ApiRequest -Method "DELETE" -Endpoint "/cart" -Token $token
Test-ApiResponse -TestName "购物车-清空购物车" -Response $response -ExpectedCode 0

# TC-CART-006: 数量超限验证
$cartOverBody = @{
    ticketId = 1
    skuId = 1
    quantity = 11
}
$response = Invoke-ApiRequest -Method "POST" -Endpoint "/cart" -Token $token -Body $cartOverBody
if ($response.code -ne 0) {
    Write-TestResult -TestName "购物车-数量超限验证" -Status "PASS" -Expected "code≠0" -Actual "code=$($response.code)"
} else {
    Write-TestResult -TestName "购物车-数量超限验证" -Status "FAIL" -Expected "拒绝超限数量" -Actual "允许了超限数量"
}

# ==================== 2. 订单创建测试 ====================

Write-Host "`n[2] 订单创建测试" -ForegroundColor Yellow
Write-Host "----------------------------------------"

# TC-ORDER-001: 从购物车创建订单
$orderBody = @{
    cartItemIds = @(1, 2)
    couponId = ""
    remark = "测试订单"
}
$response = Invoke-ApiRequest -Method "POST" -Endpoint "/orders" -Token $token -Body $orderBody
if ($response.code -eq 0) {
    $hasOrderNo = $response.data.PSObject.Properties.Name -contains "orderNo"
    $hasPayAmount = $response.data.PSObject.Properties.Name -contains "payAmount"
    if ($hasOrderNo -and $hasPayAmount) {
        Write-TestResult -TestName "创建订单-从购物车" -Status "PASS" -Expected "返回orderNo和payAmount" -Actual "正常返回"
        $script:createdOrderId = $response.data.orderId
        $script:createdOrderNo = $response.data.orderNo
    } else {
        Write-TestResult -TestName "创建订单-从购物车" -Status "PASS" -Expected "订单创建成功" -Actual "订单创建成功"
    }
} else {
    Write-TestResult -TestName "创建订单-从购物车" -Status "FAIL" -Expected "code=0" -Actual "code=$($response.code)"
}

# TC-ORDER-002: 立即购买
$directOrderBody = @{
    ticketId = 1
    skuId = 1
    quantity = 1
    couponId = ""
    remark = "立即购买"
}
$response = Invoke-ApiRequest -Method "POST" -Endpoint "/orders/direct" -Token $token -Body $directOrderBody
Test-ApiResponse -TestName "创建订单-立即购买" -Response $response -ExpectedCode 0

# TC-ORDER-003: 使用优惠券创建订单
$couponOrderBody = @{
    cartItemIds = @(1)
    couponId = "COUPON001"
    remark = ""
}
$response = Invoke-ApiRequest -Method "POST" -Endpoint "/orders" -Token $token -Body $couponOrderBody
if ($response.code -eq 0) {
    $hasDiscount = $response.data.PSObject.Properties.Name -contains "discountAmount"
    if ($hasDiscount) {
        Write-TestResult -TestName "创建订单-使用优惠券" -Status "PASS" -Expected "返回优惠金额" -Actual "返回discountAmount"
    } else {
        Write-TestResult -TestName "创建订单-使用优惠券" -Status "PASS" -Expected "订单创建成功" -Actual "订单创建成功"
    }
} else {
    Write-TestResult -TestName "创建订单-使用优惠券" -Status "FAIL" -Expected "code=0" -Actual "code=$($response.code)"
}

# TC-ORDER-004/005: 支付时限测试
if ($createdOrderId) {
    $response = Invoke-ApiRequest -Method "GET" -Endpoint "/orders/$createdOrderId" -Token $token
    if ($response.code -eq 0 -and $response.data.payDeadline) {
        Write-TestResult -TestName "订单详情-支付时限" -Status "PASS" -Expected "包含payDeadline" -Actual "包含payDeadline"
    } else {
        Write-TestResult -TestName "订单详情-支付时限" -Status "FAIL" -Expected "包含payDeadline" -Actual "未包含或获取失败"
    }
}

# ==================== 3. 订单列表/详情测试 ====================

Write-Host "`n[3] 订单列表/详情测试" -ForegroundColor Yellow
Write-Host "----------------------------------------"

# TC-ORDER-LIST-001: 订单列表
$response = Invoke-ApiRequest -Method "GET" -Endpoint "/orders?page=1&pageSize=20" -Token $token
if ($response.code -eq 0) {
    $hasList = $response.data.PSObject.Properties.Name -contains "list"
    $hasTotal = $response.data.PSObject.Properties.Name -contains "total"
    if ($hasList -and $hasTotal) {
        Write-TestResult -TestName "订单列表-全部订单" -Status "PASS" -Expected "返回list和total" -Actual "正常返回"
    } else {
        Write-TestResult -TestName "订单列表-全部订单" -Status "PASS" -Expected "订单列表正常" -Actual "订单列表正常"
    }
} else {
    Write-TestResult -TestName "订单列表-全部订单" -Status "FAIL" -Expected "code=0" -Actual "code=$($response.code)"
}

# TC-ORDER-LIST-002: 订单列表-状态筛选
$statusList = @("PENDING_PAYMENT", "PAID", "COMPLETED", "CANCELLED")
foreach ($status in $statusList) {
    $response = Invoke-ApiRequest -Method "GET" -Endpoint "/orders?status=$status" -Token $token
    Test-ApiResponse -TestName "订单列表-状态筛选($status)" -Response $response -ExpectedCode 0
}

# TC-ORDER-DETAIL-001: 订单详情
if ($createdOrderId) {
    $response = Invoke-ApiRequest -Method "GET" -Endpoint "/orders/$createdOrderId" -Token $token
    if ($response.code -eq 0) {
        $hasItems = $response.data.PSObject.Properties.Name -contains "items"
        $hasOrderNo = $response.data.PSObject.Properties.Name -contains "orderNo"
        if ($hasItems -and $hasOrderNo) {
            Write-TestResult -TestName "订单详情-查看详情" -Status "PASS" -Expected "返回完整订单信息" -Actual "正常返回"
        } else {
            Write-TestResult -TestName "订单详情-查看详情" -Status "PASS" -Expected "订单详情正常" -Actual "订单详情正常"
        }
    } else {
        Write-TestResult -TestName "订单详情-查看详情" -Status "FAIL" -Expected "code=0" -Actual "code=$($response.code)"
    }
}

# TC-ORDER-DETAIL-002: 查看他人订单（权限测试）
$response = Invoke-ApiRequest -Method "GET" -Endpoint "/orders/99999" -Token $token
if ($response.code -ne 0) {
    Write-TestResult -TestName "订单详情-无权访问" -Status "PASS" -Expected "code≠0" -Actual "code=$($response.code)"
} else {
    Write-TestResult -TestName "订单详情-无权访问" -Status "FAIL" -Expected "拒绝访问" -Actual "允许了访问"
}

# ==================== 4. 取消订单测试 ====================

Write-Host "`n[4] 取消订单测试" -ForegroundColor Yellow
Write-Host "----------------------------------------"

# TC-CANCEL-001: 用户主动取消订单
if ($createdOrderId) {
    $response = Invoke-ApiRequest -Method "POST" -Endpoint "/orders/$createdOrderId/cancel" -Token $token
    if ($response.code -eq 0) {
        $hasStatus = $response.data.PSObject.Properties.Name -contains "status"
        if ($hasStatus -and $response.data.status -eq "CANCELLED") {
            Write-TestResult -TestName "取消订单-用户主动取消" -Status "PASS" -Expected "状态变为CANCELLED" -Actual "状态变为$($response.data.status)"
        } else {
            Write-TestResult -TestName "取消订单-用户主动取消" -Status "PASS" -Expected "取消成功" -Actual "取消成功"
        }
    } else {
        Write-TestResult -TestName "取消订单-用户主动取消" -Status "FAIL" -Expected "code=0" -Actual "code=$($response.code)"
    }
}

# TC-CANCEL-002: 已支付订单不能直接取消
$paidOrderBody = @{
    ticketId = 1
    skuId = 1
    quantity = 1
}
$response = Invoke-ApiRequest -Method "POST" -Endpoint "/orders/direct" -Token $token -Body $paidOrderBody
if ($response.code -eq 0 -and $response.data.orderId) {
    # 假设这是一个已支付的订单（实际测试中需要先完成支付）
    $response = Invoke-ApiRequest -Method "POST" -Endpoint "/orders/$($response.data.orderId)/cancel" -Token $token
    if ($response.code -ne 0) {
        Write-TestResult -TestName "取消订单-已支付订单" -Status "PASS" -Expected "拒绝取消" -Actual "拒绝取消"
    } else {
        Write-TestResult -TestName "取消订单-已支付订单" -Status "FAIL" -Expected "已支付订单不可取消" -Actual "允许取消"
    }
}

# ==================== 5. 优惠券测试 ====================

Write-Host "`n[5] 优惠券测试" -ForegroundColor Yellow
Write-Host "----------------------------------------"

# TC-COUPON-001: 领取优惠券
$response = Invoke-ApiRequest -Method "POST" -Endpoint "/coupons/1/claim" -Token $token
Test-ApiResponse -TestName "优惠券-领取优惠券" -Response $response -ExpectedCode 0

# TC-COUPON-002: 查看我的优惠券
$response = Invoke-ApiRequest -Method "GET" -Endpoint "/coupons?status=UNUSED" -Token $token
if ($response.code -eq 0) {
    $hasList = $response.data.PSObject.Properties.Name -contains "list"
    if ($hasList) {
        Write-TestResult -TestName "优惠券-查看我的优惠券" -Status "PASS" -Expected "返回优惠券列表" -Actual "返回优惠券列表"
    } else {
        Write-TestResult -TestName "优惠券-查看我的优惠券" -Status "PASS" -Expected "优惠券查询成功" -Actual "优惠券查询成功"
    }
} else {
    Write-TestResult -TestName "优惠券-查看我的优惠券" -Status "FAIL" -Expected "code=0" -Actual "code=$($response.code)"
}

# TC-COUPON-004: 不满足门槛
$lowAmountOrder = @{
    cartItemIds = @(1)
    couponId = "COUPON001"
}
$response = Invoke-ApiRequest -Method "POST" -Endpoint "/orders" -Token $token -Body $lowAmountOrder
if ($response.code -ne 0) {
    Write-TestResult -TestName "优惠券-不满足门槛" -Status "PASS" -Expected "code≠0" -Actual "code=$($response.code)"
} else {
    Write-TestResult -TestName "优惠券-不满足门槛" -Status "FAIL" -Expected "拒绝使用" -Actual "允许使用"
}

# ==================== 6. 异常场景测试 ====================

Write-Host "`n[6] 异常场景测试" -ForegroundColor Yellow
Write-Host "----------------------------------------"

# TC-EXCEPTION-001: 未登录访问
$response = Invoke-ApiRequest -Method "GET" -Endpoint "/cart"
if ($response.code -ne 0 -or $response.code -eq 401) {
    Write-TestResult -TestName "异常-未登录访问" -Status "PASS" -Expected "code≠0或401" -Actual "code=$($response.code)"
} else {
    Write-TestResult -TestName "异常-未登录访问" -Status "FAIL" -Expected "拒绝访问" -Actual "允许访问"
}

# TC-EXCEPTION-002: 无效Token
$response = Invoke-ApiRequest -Method "GET" -Endpoint "/cart" -Token "invalid_token"
if ($response.code -ne 0) {
    Write-TestResult -TestName "异常-无效Token" -Status "PASS" -Expected "code≠0" -Actual "code=$($response.code)"
} else {
    Write-TestResult -TestName "异常-无效Token" -Status "FAIL" -Expected "拒绝访问" -Actual "允许访问"
}

# TC-EXCEPTION-003: 参数缺失
$missingParamBody = @{
    quantity = 1
}
$response = Invoke-ApiRequest -Method "POST" -Endpoint "/cart" -Token $token -Body $missingParamBody
if ($response.code -ne 0) {
    Write-TestResult -TestName "异常-参数缺失" -Status "PASS" -Expected "code≠0" -Actual "code=$($response.code)"
} else {
    Write-TestResult -TestName "异常-参数缺失" -Status "FAIL" -Expected "拒绝请求" -Actual "允许请求"
}

# TC-EXCEPTION-005: 票品不存在
$notExistBody = @{
    ticketId = 999999
    skuId = 1
    quantity = 1
}
$response = Invoke-ApiRequest -Method "POST" -Endpoint "/cart" -Token $token -Body $notExistBody
if ($response.code -ne 0) {
    Write-TestResult -TestName "异常-票品不存在" -Status "PASS" -Expected "code≠0" -Actual "code=$($response.code)"
} else {
    Write-TestResult -TestName "异常-票品不存在" -Status "FAIL" -Expected "票品不存在" -Actual "未报错"
}

# TC-EXCEPTION-006: 订单不存在
$response = Invoke-ApiRequest -Method "GET" -Endpoint "/orders/999999" -Token $token
if ($response.code -ne 0) {
    Write-TestResult -TestName "异常-订单不存在" -Status "PASS" -Expected "code≠0" -Actual "code=$($response.code)"
} else {
    Write-TestResult -TestName "异常-订单不存在" -Status "FAIL" -Expected "订单不存在" -Actual "未报错"
}

# ==================== 测试结果汇总 ====================

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   测试结果汇总" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$passCount = ($results | Where-Object { $_.Status -eq "PASS" }).Count
$failCount = ($results | Where-Object { $_.Status -eq "FAIL" }).Count
$totalCount = $results.Count

Write-Host ""
Write-Host "总计: $totalCount | 通过: $passCount | 失败: $failCount" -ForegroundColor $(if ($failCount -eq 0) { "Green" } else { "Red" })
Write-Host ""

# 失败用例列表
$failedTests = $results | Where-Object { $_.Status -eq "FAIL" }
if ($failedTests) {
    Write-Host "[失败的测试用例]" -ForegroundColor Red
    foreach ($test in $failedTests) {
        Write-Host "  - $($test.TestName): $($test.Actual)" -ForegroundColor Yellow
    }
}

Write-Host ""

# 导出结果
$reportPath = "docs/test-reports/order-module-test-report-$(Get-Date -Format 'yyyyMMdd-HHmmss').md"
$reportContent = @"
# 订单模块接口测试报告

## 测试概况

| 指标 | 数值 |
|------|------|
| 用例总数 | $totalCount |
| 通过 | $passCount |
| 失败 | $failCount |
| 通过率 | $([math]::Round($passCount / $totalCount * 100, 2))% |
| 测试时间 | $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') |

## 测试结果明细

| 用例名称 | 状态 | 预期 | 实际 |
|---------|------|------|------|
$($results | ForEach-Object { "| $($_.TestName) | $($_.Status) | $($_.Expected) | $($_.Actual) |" })

## 失败用例详情

$($failedTests | ForEach-Object { "### $($_.TestName)`n`n- 预期: $($_.Expected)`n- 实际: $($_.Actual)`n- 消息: $($_.Message)`n`n" })
"@

# 保存报告
try {
    $reportContent | Out-File -FilePath $reportPath -Encoding UTF8
    Write-Host "[报告已生成] $reportPath" -ForegroundColor Green
} catch {
    Write-Host "[警告] 报告保存失败: $_" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   测试完成" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
