# Set Java 17
$env:PATH = "C:\Program Files\Java\jdk-17\bin;" + $env:PATH
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"

# Build
Set-Location "D:\AIProjects\BookingApi\booking-api"
Write-Host "Building project..."
& "D:\software\apache-maven-3.8.8-bin\apache-maven-3.8.8\bin\mvn.cmd" clean package -DskipTests -q

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed"
    exit 1
}

Write-Host "Build successful"

# Start server
Write-Host "Starting Spring Boot..."
$proc = Start-Process -FilePath "java" -ArgumentList "-jar", "target\booking-api-1.0.0-SNAPSHOT.jar", "--spring.profiles.active=dev", "--server.port=8088" -NoNewWindow -PassThru -WorkingDirectory "D:\AIProjects\BookingApi\booking-api"

Write-Host "Waiting for startup (PID: $($proc.Id))..."
Start-Sleep -Seconds 20

$portCheck = Test-NetConnection -ComputerName localhost -Port 8088 -WarningAction SilentlyContinue
if ($portCheck.TcpTestSucceeded) {
    Write-Host "Server is running on port 8088"
    Write-Host "PID: $($proc.Id)"
} else {
    Write-Host "Server may not have started properly"
}

# Test API
Write-Host "`n=== API Tests ==="
Write-Host "GET /api/tickets"
$r = Invoke-WebRequest -Uri "http://localhost:8088/api/tickets" -UseBasicParsing -TimeoutSec 10
Write-Host "Status: $($r.StatusCode)"

Write-Host "Done"
