@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d D:\AIProjects\BookingApi
echo Using Java: 
java -version
echo.
echo Starting Maven compile:
mvn clean compile
pause