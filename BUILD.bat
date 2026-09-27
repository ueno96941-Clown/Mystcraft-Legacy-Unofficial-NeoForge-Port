@echo off
setlocal EnableExtensions
cd /d "%~dp0"

call VERIFY_ENVIRONMENT.bat
if errorlevel 1 (
  echo.
  echo Environment/pre-build check failed. Fix the message above before building.
  pause
  exit /b 1
)

echo.
echo === Mystcraft Gradle build ===
echo Full output will also be saved to BUILD_LAST.log
set "LOCAL_GRADLE=%~dp0tools\gradle-9.2.1\bin\gradle.bat"
if exist "%LOCAL_GRADLE%" (
  echo Using embedded Gradle 9.2.1: %LOCAL_GRADLE%
  powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -Command "& { & '%LOCAL_GRADLE%' clean build --stacktrace 2>&1 | Tee-Object -FilePath 'BUILD_LAST.log'; exit $LASTEXITCODE }"
) else (
  echo Embedded Gradle not found; falling back to Gradle Wrapper.
  powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -Command "& { & .\gradlew.bat clean build --stacktrace 2>&1 | Tee-Object -FilePath 'BUILD_LAST.log'; exit $LASTEXITCODE }"
)
if errorlevel 1 (
  echo.
  if exist BUILD_LAST.log (
    echo Build failed. BUILD_LAST.log contains the Gradle output.
  ) else (
    echo Build failed before BUILD_LAST.log could be created.
  )
  pause
  exit /b 1
)
echo.
echo Build complete. Check build\libs\
echo Log: BUILD_LAST.log
pause
