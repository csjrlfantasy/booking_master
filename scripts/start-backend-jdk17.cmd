@echo off
setlocal
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d D:\AIProjects\BookingApi
set "BOOT_LIB_DIR=D:\AIProjects\BookingApi\booking-api\target\boot-libs\BOOT-INF\lib"
if not exist "%BOOT_LIB_DIR%" (
  mkdir D:\AIProjects\BookingApi\booking-api\target\boot-libs
  pushd D:\AIProjects\BookingApi\booking-api\target\boot-libs
  jar xf ..\booking-api-1.0.0-SNAPSHOT.jar BOOT-INF/lib
  popd
)
set "APP_CP=D:\AIProjects\BookingApi\booking-api\target\classes;D:\AIProjects\BookingApi\booking-common\target\classes;%BOOT_LIB_DIR%\*"
java -cp "%APP_CP%" com.ticket.app.BookingApiApplication --spring.profiles.active=dev --server.port=8088 1>D:\AIProjects\BookingApi\logs\backend.out.log 2>D:\AIProjects\BookingApi\logs\backend.err.log
