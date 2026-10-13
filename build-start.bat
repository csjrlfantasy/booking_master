@echo off
setlocal
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MAVEN_CMD=D:\software\apache-maven-3.8.8-bin\apache-maven-3.8.8\bin\mvn.cmd"
cd /d %~dp0

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo [ERROR] JDK 17 not found at %JAVA_HOME%
  pause
  exit /b 1
)

if not exist "%MAVEN_CMD%" (
  set "MAVEN_CMD=mvn.cmd"
)

echo Using Java:
java -version
echo.
echo Building...
call "%MAVEN_CMD%" -pl booking-api -am package -DskipTests -q
if errorlevel 1 (
  echo [ERROR] Build failed.
  pause
  exit /b 1
)
echo.
echo Starting Booking API on port 8088...
java -jar booking-api\target\booking-api-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev --server.port=8088
pause
