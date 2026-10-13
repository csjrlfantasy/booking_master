@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d %~dp0
echo Using Java: 
java -version
echo.
echo Starting Booking API on port 8088...
java -jar booking-api\target\booking-api-1.0.0-SNAPSHOT.jar --server.port=8088
pause