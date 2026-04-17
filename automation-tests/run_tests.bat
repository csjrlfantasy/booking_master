@echo off
REM 接口自动化测试运行脚本

echo ========================================
echo  BookingApi 接口自动化测试
echo ========================================
echo.

REM 检查Python环境
python --version >nul 2>&1
if errorlevel 1 (
    echo [Error] Python not found. Please install Python 3.10+
    pause
    exit /b 1
)

REM 检查依赖
python -c "import pytest" >nul 2>&1
if errorlevel 1 (
    echo [Info] Installing dependencies...
    pip install -r requirements.txt
)

REM 默认运行所有测试
if "%1"=="" (
    echo Running all tests...
    pytest api-tests/tests/ -v --html=report/test_report.html --self-contained-html --alluredir=report/allure-results
    goto :report
)

REM 根据参数运行不同测试
if "%1"=="smoke" goto :smoke
if "%1"=="user" goto :user
if "%1"=="ticket" goto :ticket
if "%1"=="order" goto :order
if "%1"=="cart" goto :cart
if "%1"=="coupon" goto :coupon
if "%1"=="report" goto :gen_report
if "%1"=="help" goto :help

echo [Error] Unknown command: %1
echo Run 'run_tests.bat help' for usage
pause
exit /b 1

:smoke
echo Running Smoke Tests...
pytest api-tests/tests/ -v -m smoke --html=report/test_report.html --self-contained-html --alluredir=report/allure-results
goto :report

:user
echo Running User Module Tests...
pytest api-tests/tests/test_user.py -v --html=report/test_report.html --self-contained-html --alluredir=report/allure-results
goto :report

:ticket
echo Running Ticket Module Tests...
pytest api-tests/tests/test_ticket.py -v --html=report/test_report.html --self-contained-html --alluredir=report/allure-results
goto :report

:order
echo Running Order Module Tests...
pytest api-tests/tests/test_order.py -v --html=report/test_report.html --self-contained-html --alluredir=report/allure-results
goto :report

:cart
echo Running Cart Module Tests...
pytest api-tests/tests/test_cart.py -v --html=report/test_report.html --self-contained-html --alluredir=report/allure-results
goto :report

:coupon
echo Running Coupon Module Tests...
pytest api-tests/tests/test_coupon.py -v --html=report/test_report.html --self-contained-html --alluredir=report/allure-results
goto :report

:gen_report
echo Generating Allure Report...
if exist report\allure-results (
    allure generate report\allure-results -o report\allure-report --clean
    if exist report\allure-report\index.html (
        echo Report generated: report\allure-report\index.html
        allure open report\allure-report
    ) else (
        echo [Error] Failed to generate report
    )
) else (
    echo [Error] No allure results found. Run tests first.
)
goto :end

:help
echo.
echo Usage: run_tests.bat [command]
echo.
echo Commands:
echo   smoke     - Run smoke tests (冒烟测试)
echo   user      - Run user module tests (用户模块)
echo   ticket    - Run ticket module tests (票务模块)
echo   order     - Run order module tests (订单模块)
echo   cart      - Run cart module tests (购物车模块)
echo   coupon    - Run coupon module tests (优惠券模块)
echo   report    - Generate Allure report (生成报告)
echo   help      - Show this help message
echo.
echo Examples:
echo   run_tests.bat           - Run all tests
echo   run_tests.bat smoke     - Run smoke tests
echo   run_tests.bat report    - Generate test report
echo.
goto :end

:report
echo.
echo ========================================
echo Test execution completed
echo ========================================
echo.

:end
pause