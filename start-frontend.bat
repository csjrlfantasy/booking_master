@echo off
cd /d %~dp0booking-app
echo Installing dependencies...
call npm install
echo.
echo Starting Metro bundler...
start "Metro" cmd /c "npm start"
echo.
echo To run on Android: npm run android
echo To run on iOS: npm run ios
pause