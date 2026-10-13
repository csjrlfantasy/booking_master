$ErrorActionPreference = "Stop"

$baseUrl = "http://127.0.0.1:8088/api"
$frontendHealthUrl = "http://127.0.0.1:3000"
$backendHealthUrl = "http://127.0.0.1:8088/api/actuator/health"
$adminUsername = "admin"
$adminPassword = "Admin@123456"
$validIdCard = "11010519491231002X"
$targetTicketId = 7

function Assert-ServiceAvailable {
    param(
        [string]$Name,
        [string]$Url
    )

    try {
        Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5 | Out-Null
    } catch {
        throw "$Name is not reachable at $Url. Raw error: $($_.Exception.Message)"
    }
}

function To-JsonBody {
    param([hashtable]$Data)
    return $Data | ConvertTo-Json -Depth 10
}

Assert-ServiceAvailable "Frontend dev server" $frontendHealthUrl
Assert-ServiceAvailable "Backend API" $backendHealthUrl

$stamp = [DateTimeOffset]::Now.ToUnixTimeSeconds()
$username = "flowsmoke$stamp"
$phone = "137$($stamp.ToString().Substring($stamp.ToString().Length - 8))"

$adminLogin = Invoke-RestMethod -Uri "$baseUrl/admin/login" -Method POST -Body (To-JsonBody @{
    username = $adminUsername
    password = $adminPassword
}) -ContentType "application/json"

if ($adminLogin.code -ne 0 -or -not $adminLogin.data.token) {
    throw "Admin login failed."
}

$adminHeaders = @{
    Authorization = "Bearer $($adminLogin.data.token)"
}

$couponCreate = Invoke-RestMethod -Uri "$baseUrl/admin/coupons" -Method POST -Headers $adminHeaders -Body (To-JsonBody @{
    name = "Flow Smoke Coupon $stamp"
    couponType = 1
    couponValue = 30.00
    minOrderAmount = 100.00
    totalCount = 50
    remainCount = 50
    perLimit = 1
    receiveType = 1
    validityType = 1
    validStartDate = "2026-07-01"
    validEndDate = "2026-12-31"
    applicableScope = 1
    description = "Customer flow smoke coupon"
    notice = "Created by smoke script"
    receiveStartTime = "2026-07-01T00:00:00"
    receiveEndTime = "2026-12-31T23:59:59"
    status = 1
}) -ContentType "application/json"

if ($couponCreate.code -ne 0 -or -not $couponCreate.data) {
    throw "Coupon creation failed."
}

$couponId = $couponCreate.data

$register = Invoke-RestMethod -Uri "$baseUrl/user/register" -Method POST -Body (To-JsonBody @{
    username = $username
    password = "Pass@123456"
    phone = $phone
    nickname = "Flow Smoke"
    source = "h5"
}) -ContentType "application/json"

if ($register.code -ne 0) {
    throw "User registration failed."
}

$login = Invoke-RestMethod -Uri "$baseUrl/user/login" -Method POST -Body (To-JsonBody @{
    username = $username
    password = "Pass@123456"
}) -ContentType "application/json"

if ($login.code -ne 0 -or -not $login.data.token) {
    throw "User login failed."
}

$userHeaders = @{
    Authorization = "Bearer $($login.data.token)"
}

$identityCreate = Invoke-RestMethod -Uri "$baseUrl/user/identities" -Method POST -Headers $userHeaders -Body (To-JsonBody @{
    realName = "Flow User"
    idCardNo = $validIdCard
    phone = $phone
    self = $true
    defaultIdentity = $true
}) -ContentType "application/json"

if ($identityCreate.code -ne 0 -or -not $identityCreate.data.id) {
    throw "Identity creation failed."
}

$identityId = $identityCreate.data.id

$couponCenter = Invoke-RestMethod -Uri "$baseUrl/coupons/center?page=1&pageSize=20" -Headers $userHeaders
$targetCoupon = $couponCenter.data.list | Where-Object { $_.couponId -eq $couponId } | Select-Object -First 1
if (-not $targetCoupon) {
    throw "Coupon was not visible in customer coupon center."
}

$receiveCoupon = Invoke-RestMethod -Uri "$baseUrl/coupons/receive?couponId=$couponId" -Method POST -Headers $userHeaders
if ($receiveCoupon.code -ne 0 -or -not $receiveCoupon.data) {
    throw "Coupon receive failed."
}

$userCouponId = $receiveCoupon.data

$ticketDetail = Invoke-RestMethod -Uri "$baseUrl/tickets/$targetTicketId"
if ($ticketDetail.code -ne 0 -or -not $ticketDetail.data.sessions -or -not $ticketDetail.data.priceLevels) {
    throw "Ticket detail lookup failed."
}

$session = $ticketDetail.data.sessions[0]
$priceLevel = $ticketDetail.data.priceLevels[0]

$availableCoupons = Invoke-RestMethod -Uri "$baseUrl/coupons?orderAmount=$($priceLevel.price)&ticketId=$targetTicketId&categoryId=$($ticketDetail.data.categoryId)" -Headers $userHeaders
$matchedCoupon = $availableCoupons.data | Where-Object { $_.id -eq $userCouponId } | Select-Object -First 1
if (-not $matchedCoupon) {
    throw "Received coupon was not available during checkout."
}

$couponValidation = Invoke-RestMethod -Uri "$baseUrl/coupons/validate" -Method POST -Headers $userHeaders -Body (To-JsonBody @{
    userCouponId = $userCouponId
    orderAmount = $priceLevel.price
    ticketId = $targetTicketId
    categoryId = $ticketDetail.data.categoryId
}) -ContentType "application/json"

if ($couponValidation.code -ne 0 -or -not $couponValidation.data.usable) {
    throw "Coupon validation failed."
}

$orderCreate = Invoke-RestMethod -Uri "$baseUrl/orders" -Method POST -Headers $userHeaders -Body (To-JsonBody @{
    ticketId = $targetTicketId
    sessionId = $session.id
    userCouponId = $userCouponId
    contactName = "Flow User"
    contactPhone = $phone
    contactIdCard = $validIdCard
    userRemark = "customer flow smoke order"
    source = "h5"
    items = @(
        @{
            priceLevelId = $priceLevel.id
            priceLevelName = $priceLevel.name
            quantity = 1
            unitPrice = $priceLevel.price
            attendees = @(
                @{
                    identityId = $identityId
                    realName = "Flow User"
                    idCardNo = $validIdCard
                }
            )
        }
    )
}) -ContentType "application/json"

if ($orderCreate.code -ne 0 -or -not $orderCreate.data.orderNo) {
    throw "Order creation failed."
}

$orderNo = $orderCreate.data.orderNo
$orderId = $orderCreate.data.orderId
$payAmount = $orderCreate.data.payAmount

$mockPay = Invoke-RestMethod -Uri "$baseUrl/payment/mock-pay" -Method POST -Headers $userHeaders -Body (To-JsonBody @{
    orderNo = $orderNo
    amount = $payAmount
    payChannel = "wechat"
    paymentScene = "H5"
}) -ContentType "application/json"

if ($mockPay.code -ne 0 -or $mockPay.data.status -ne "SUCCESS") {
    throw "Mock payment failed."
}

$orderDetail = Invoke-RestMethod -Uri "$baseUrl/orders/$orderId" -Headers $userHeaders
if ($orderDetail.code -ne 0 -or $orderDetail.data.payStatus -ne 1) {
    throw "Paid order detail check failed."
}

$refund = Invoke-RestMethod -Uri "$baseUrl/payment/refund" -Method POST -Headers $userHeaders -Body (To-JsonBody @{
    orderNo = $orderNo
    reason = "customer flow smoke refund"
}) -ContentType "application/json"

if ($refund.code -ne 0 -or -not $refund.data.status) {
    throw "Refund request failed."
}

Write-Host ("[PASS] customer flow user={0} couponId={1} orderNo={2} payAmount={3} payStatus={4} refundStatus={5}" -f `
    $username, $couponId, $orderNo, $payAmount, $orderDetail.data.payStatus, $refund.data.status)
