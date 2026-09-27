@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo === Java ===
for /f "tokens=3" %%V in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VERSION_RAW=%%~V
if not defined JAVA_VERSION_RAW goto :java_fail
set JAVA_VERSION_RAW=%JAVA_VERSION_RAW:"=%
for /f "tokens=1 delims=." %%M in ("%JAVA_VERSION_RAW%") do set JAVA_MAJOR=%%M
if "%JAVA_MAJOR%"=="1" (
  for /f "tokens=2 delims=." %%M in ("%JAVA_VERSION_RAW%") do set JAVA_MAJOR=%%M
)
java -version
if not "%JAVA_MAJOR%"=="21" (
  echo.
  echo WRONG JAVA: Mystcraft 1.21.1 port requires Java 21, found %JAVA_VERSION_RAW%.
  echo Set JAVA_HOME/PATH to a 64-bit Java 21 JDK before building.
  exit /b 1
)

echo.
echo === Wrapper ===
if not exist gradle\wrapper\gradle-wrapper.jar (
  echo MISSING: gradle\wrapper\gradle-wrapper.jar
  echo Run BOOTSTRAP_WINDOWS.bat first.
  exit /b 2
)
if not exist gradle\wrapper\gradle-wrapper.properties (
  echo MISSING: gradle\wrapper\gradle-wrapper.properties
  echo Run BOOTSTRAP_WINDOWS.bat first.
  exit /b 2
)
if not exist gradlew.bat (
  echo MISSING: gradlew.bat
  echo Run BOOTSTRAP_WINDOWS.bat first.
  exit /b 2
)

echo.
echo === Gradle ===
call gradlew.bat --version
if errorlevel 1 exit /b 3

echo.
echo Environment looks ready for BUILD.bat / RUN_SERVER.bat / RUN_CLIENT.bat.
exit /b 0

:java_fail
echo.
echo Java was not found. Install a 64-bit Java 21 JDK and make java available on PATH.
exit /b 1
