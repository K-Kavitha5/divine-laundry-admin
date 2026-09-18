@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>&1
if errorlevel 1 (
  echo Java JDK 17 or newer is required. Install it and reopen this window.
  pause
  exit /b 1
)
where mvn >nul 2>&1
if errorlevel 1 (
  echo Apache Maven is required. Add its bin directory to PATH and reopen this window.
  echo Alternatively, open backend/pom.xml in IntelliJ and run with the demo Spring profile.
  pause
  exit /b 1
)
echo Starting LOCAL DEMO ONLY. Open http://localhost:8080 after Spring reports Started.
echo Login: admin   Password: DemoLaundry123!
echo Data is saved in backend/data. WhatsApp sending is disabled.
call mvn spring-boot:run -Dspring-boot.run.profiles=demo
pause
