@echo off
:: 관리자 권한 요청
net session >nul 2>&1
if %errorLevel% neq 0 (
    powershell -Command "Start-Process '%~f0' -Verb RunAs"
    exit /b
)

netsh advfirewall firewall add rule name="NFC School Backend HTTPS" dir=in action=allow protocol=TCP localport=8443
echo.
echo 포트 8443 방화벽 열기 완료!
pause
