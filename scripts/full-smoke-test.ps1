$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($env:BOOKING_ADMIN_PASSWORD)) {
    throw "BOOKING_ADMIN_PASSWORD is required for the administrator smoke test."
}

$baseUrl = "http://127.0.0.1:3000/api"
$frontendHealthUrl = "http://127.0.0.1:3000"
$backendHealthUrl = "http://127.0.0.1:8088/api/actuator/health"

function Assert-ServiceAvailable {
    param(
        [string]$Name,
        [string]$Url
    )

    try {
        Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5 | Out-Null
    } catch {
        throw "$Name is not reachable at $Url. Please start the project with start-all.bat or the scripts under scripts/ before running smoke tests. Raw error: $($_.Exception.Message)"
    }
}

Assert-ServiceAvailable "Frontend dev server" $frontendHealthUrl
Assert-ServiceAvailable "Backend API" $backendHealthUrl

$loginBody = @{
    username = "admin"
    password = $env:BOOKING_ADMIN_PASSWORD
} | ConvertTo-Json

$login = Invoke-RestMethod -Uri "$baseUrl/admin/login" -Method POST -Body $loginBody -ContentType "application/json" -TimeoutSec 10
$token = $login.data.token

if (-not $token) {
    throw "Login succeeded but token is empty."
}

$headers = @{
    Authorization = "Bearer $token"
}

$script:failed = 0

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Url
    )

    try {
        $response = Invoke-RestMethod -Uri $Url -Headers $headers -TimeoutSec 10
        if ($response.code -ne 0) {
            $script:failed++
            Write-Host "[FAIL] $Name code=$($response.code) message=$($response.message)"
            return $null
        }

        Write-Host "[PASS] $Name"
        return $response
    } catch {
        $script:failed++
        Write-Host "[FAIL] $Name $($_.Exception.Message)"
        return $null
    }
}

$userList = Test-Endpoint "admin user list" "$baseUrl/admin/users?page=1&pageSize=5"
if ($userList -and $userList.data.list.Count -gt 0) {
    Test-Endpoint "admin user detail" "$baseUrl/admin/users/$($userList.data.list[0].id)" | Out-Null
}

$orderList = Test-Endpoint "admin order list" "$baseUrl/admin/orders?page=1&pageSize=5"
if ($orderList -and $orderList.data.list.Count -gt 0) {
    Test-Endpoint "admin order detail" "$baseUrl/admin/orders/$($orderList.data.list[0].id)" | Out-Null
}

$ticketList = Test-Endpoint "admin ticket list" "$baseUrl/admin/tickets?page=1&pageSize=5"
if ($ticketList -and $ticketList.data.list.Count -gt 0) {
    Test-Endpoint "admin ticket detail" "$baseUrl/admin/tickets/$($ticketList.data.list[0].id)" | Out-Null
}

Test-Endpoint "overview stats" "$baseUrl/admin/stats/overview" | Out-Null
Test-Endpoint "sales stats" "$baseUrl/admin/stats/sales" | Out-Null
Test-Endpoint "order stats" "$baseUrl/admin/stats/orders" | Out-Null
Test-Endpoint "user stats" "$baseUrl/admin/stats/users" | Out-Null
Test-Endpoint "roles" "$baseUrl/admin/roles" | Out-Null
Test-Endpoint "public ticket list" "$baseUrl/tickets?page=1&pageSize=5" | Out-Null
Test-Endpoint "categories" "$baseUrl/categories" | Out-Null

if ($failed -gt 0) {
    throw "$failed smoke test(s) failed."
}

Write-Host "All smoke tests passed."
