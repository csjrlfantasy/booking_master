@echo off
setlocal
cd /d "%~dp0.."
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0..\start-all.ps1" -SkipFrontend
exit /b %ERRORLEVEL%
