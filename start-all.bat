@echo off
setlocal
cd /d "%~dp0"

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-all.ps1"
if errorlevel 1 (
  echo.
  echo [ERROR] Project startup failed. Check the message above and logs directory.
  pause
  exit /b 1
)

echo.
echo Project started. Customer: http://127.0.0.1:3000/shop
exit /b 0

