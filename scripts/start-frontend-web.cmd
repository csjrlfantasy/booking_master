@echo off
setlocal
cd /d "%~dp0..\frontend"
if not defined VITE_DEV_PORT set "VITE_DEV_PORT=3000"
if not defined VITE_PROXY_TARGET set "VITE_PROXY_TARGET=http://127.0.0.1:8088"
npm.cmd run dev -- --host 127.0.0.1 --port %VITE_DEV_PORT%
exit /b %ERRORLEVEL%
