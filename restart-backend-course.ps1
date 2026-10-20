# Restart BookingApi backend with env strictly loaded from .env (file wins over inherited shell env).
$ErrorActionPreference = 'Stop'
$envFile = 'D:\AIProjects\BookingApi\.env'
foreach ($line in Get-Content -LiteralPath $envFile) {
    $t = $line.Trim()
    if (-not $t -or $t.StartsWith('#')) { continue }
    $p = $t.Split('=', 2)
    if ($p.Count -eq 2) {
        [Environment]::SetEnvironmentVariable($p[0].Trim(), $p[1].Trim(), 'Process')
    }
}
$jar = 'D:\AIProjects\BookingApi\booking-api\target\booking-api-1.0.0-SNAPSHOT.jar'
$java = 'C:\Program Files\Java\jdk-17\bin\java.exe'
Start-Process -FilePath $java `
    -ArgumentList '-jar', "`"$jar`"", '--spring.profiles.active=dev', '--server.port=8088' `
    -WorkingDirectory 'D:\AIProjects\BookingApi' `
    -RedirectStandardOutput 'D:\AIProjects\BookingApi\logs\backend.out.log' `
    -RedirectStandardError 'D:\AIProjects\BookingApi\logs\backend.err.log' `
    -WindowStyle Hidden
Write-Host 'backend starting...'
