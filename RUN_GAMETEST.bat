@echo off
setlocal
cd /d "%~dp0"
if not exist gradle\wrapper\gradle-wrapper.jar (
  echo Gradle wrapper is missing. Run BOOTSTRAP_WINDOWS.bat first.
  pause
  exit /b 1
)
call gradlew.bat runGameTestServer
if errorlevel 1 (
  echo.
  echo GameTest server failed. Preserve logs and console output.
  pause
  exit /b 1
)
