$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Build the project first
Set-Location "D:\AIProjects\BookingApi"
Write-Host "Building project..."
& "D:\software\apache-maven-3.8.8-bin\apache-maven-3.8.8\bin\mvn.cmd" clean package -DskipTests -q

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed"
    exit 1
}

# Start the application
Set-Location "D:\AIProjects\BookingApi\booking-api"
Write-Host "Starting Spring Boot..."
$proc = Start-Process -FilePath "java" -ArgumentList "-jar", "target/booking-api-1.0.0-SNAPSHOT.jar", "--spring.profiles.active=dev", "--server.port=8088" -NoNewWindow -PassThru -WorkingDirectory "D:\AIProjects\BookingApi\booking-api"

# Wait for startup
Write-Host "Waiting for startup (PID: $($proc.Id))..."
Start-Sleep -Seconds 15

# Test if port is open
$portCheck = Test-NetConnection -ComputerName localhost -Port 8088 -WarningAction SilentlyContinue
if ($portCheck.TcpTestSucceeded) {
    Write-Host "Server is running on port 8088"
    
    # Run API tests
    Write-Host "`n=== API Tests ==="
    
    # Test 1: GET all tickets
    Write-Host "`n[Test 1] GET /api/tickets"
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8088/api/tickets" -UseBasicParsing -TimeoutSec 5
        Write-Host "Status: $($r.StatusCode)"
        Write-Host "Body: $($r.Content.Substring(0, [Math]::Min(300, $r.Content.Length)))"
    } catch { Write-Host "Error: $_" }
    
    # Test 2: POST new ticket
    Write-Host "`n[Test 2] POST /api/tickets"
    $json = '{"name":"测试票务","description":"Test","venue":"北京五棵松","eventTime":"2026-06-01 20:00:00","status":1,"totalTickets":500,"categoryId":1}'
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8088/api/tickets" -Method POST -Body $json -ContentType "application/json" -UseBasicParsing -TimeoutSec 5
        Write-Host "Status: $($r.StatusCode)"
        Write-Host "Body: $($r.Content.Substring(0, [Math]::Min(300, $r.Content.Length)))"
    } catch { Write-Host "Error: $_" }
    
    # Test 3: GET single ticket
    Write-Host "`n[Test 3] GET /api/tickets/1"
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8088/api/tickets/1" -UseBasicParsing -TimeoutSec 5
        Write-Host "Status: $($r.StatusCode)"
        Write-Host "Body: $($r.Content.Substring(0, [Math]::Min(300, $r.Content.Length)))"
    } catch { Write-Host "Error: $_" }
    
    Write-Host "`n=== Tests Complete ==="
} else {
    Write-Host "Server failed to start"
}

# Keep process running for manual testing
Write-Host "`nServer is running. Press Enter to stop..."
Read-Host
Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
