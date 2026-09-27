@echo off
setlocal
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0bootstrap_wrapper.ps1"
if errorlevel 1 (
  echo.
  echo Bootstrap failed.
  pause
  exit /b 1
)
echo.
echo Bootstrap complete. You can now run BUILD.bat
pause
