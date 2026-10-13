@echo off
setlocal
REM Compatibility entry point. The PowerShell suite runner is the single source of truth.
set "TEST_ROOT=%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%TEST_ROOT%scripts\run-suite.ps1" %*
exit /b %ERRORLEVEL%
