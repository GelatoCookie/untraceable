@echo off
REM ============================================================================
REM Automated Build, Clean, and Run Script for G2V2 RFID Project (Windows)
REM 
REM Usage: build-and-run.bat [command]
REM
REM Commands:
REM   clean       - Clean build artifacts
REM   build       - Build the project (debug or release)
REM   run         - Run on connected device/emulator
REM   all         - Clean, build, and run (default)
REM   test        - Run unit tests
REM   release     - Build release APK
REM   help        - Show this help message
REM ============================================================================

setlocal enabledelayedexpansion

REM Get project root directory
set "PROJECT_ROOT=%~dp0"
set "GRADLE_WRAPPER=%PROJECT_ROOT%gradlew.bat"

REM Check if gradlew.bat exists
if not exist "%GRADLE_WRAPPER%" (
    echo Error: gradlew.bat not found in %PROJECT_ROOT%
    exit /b 1
)

REM Set command (default to 'all')
set "COMMAND=%1"
if "%COMMAND%"=="" set "COMMAND=all"

REM Process command
if /i "%COMMAND%"=="clean" (
    call :clean_build
) else if /i "%COMMAND%"=="build" (
    call :build_debug
) else if /i "%COMMAND%"=="run" (
    call :run_on_device
) else if /i "%COMMAND%"=="all" (
    call :clean_build
    if errorlevel 1 exit /b 1
    call :build_debug
    if errorlevel 1 exit /b 1
    call :run_on_device
    echo.
    echo [SUCCESS] Clean, build, and run completed!
    echo.
) else if /i "%COMMAND%"=="test" (
    call :run_tests
) else if /i "%COMMAND%"=="release" (
    call :clean_build
    if errorlevel 1 exit /b 1
    call :build_release
) else if /i "%COMMAND%"=="help" (
    call :show_help
) else if /i "%COMMAND%"=="-h" (
    call :show_help
) else if /i "%COMMAND%"=="--help" (
    call :show_help
) else (
    echo [ERROR] Unknown command: %COMMAND%
    call :show_help
    exit /b 1
)

exit /b 0

REM ============================================================================
REM Functions
REM ============================================================================

:clean_build
echo.
echo [INFO] Cleaning build artifacts...
call "%GRADLE_WRAPPER%" -q clean
if errorlevel 1 (
    echo [ERROR] Clean failed
    exit /b 1
)
echo [SUCCESS] Clean completed
exit /b 0

:build_debug
echo.
echo [INFO] Building debug APK...
call "%GRADLE_WRAPPER%" build --info
if errorlevel 1 (
    echo [ERROR] Build failed
    exit /b 1
)
echo [SUCCESS] Debug build completed
exit /b 0

:build_release
echo.
echo [INFO] Building release APK...
call "%GRADLE_WRAPPER%" assembleRelease --info
if errorlevel 1 (
    echo [ERROR] Release build failed
    exit /b 1
)
echo [SUCCESS] Release build completed
echo [INFO] Release APK: app\build\outputs\apk\release\app-release.apk
exit /b 0

:run_tests
echo.
echo [INFO] Running unit tests...
call "%GRADLE_WRAPPER%" test
if errorlevel 1 (
    echo [ERROR] Tests failed
    exit /b 1
)
echo [SUCCESS] Tests completed
exit /b 0

:run_on_device
echo.
echo [INFO] Installing debug APK on device...
call "%GRADLE_WRAPPER%" installDebug
if errorlevel 1 (
    echo [ERROR] Installation failed
    exit /b 1
)
echo [SUCCESS] Installation completed
echo.
echo [INFO] Launching app...
for /f "tokens=*" %%a in ('adb shell getprop ro.build.version.release') do set "ANDROID_VERSION=%%a"
adb shell am start -n com.zebra.rfid.demo.sdksample/com.zebra.rfid.demo.sdksample.MainActivity
if errorlevel 1 (
    echo [WARNING] Failed to launch app - check if device is connected
    exit /b 1
)
echo [SUCCESS] App launched
exit /b 0

:show_help
echo.
echo ============================================================================
echo  Automated Build, Clean, and Run Script for G2V2 RFID Project
echo ============================================================================
echo.
echo Usage: build-and-run.bat [command]
echo.
echo Commands:
echo   clean       - Clean build artifacts
echo   build       - Build the project (debug or release^)
echo   run         - Run on connected device/emulator
echo   all         - Clean, build, and run (default^)
echo   test        - Run unit tests
echo   release     - Build release APK
echo   help        - Show this help message
echo.
echo Examples:
echo   build-and-run.bat
echo   build-and-run.bat all
echo   build-and-run.bat build
echo   build-and-run.bat release
echo   build-and-run.bat test
echo.
echo Requirements:
echo   - Android SDK installed and ANDROID_HOME set
echo   - Device/emulator connected for 'run' command
echo.
exit /b 0
