@echo off
setlocal enabledelayedexpansion
title AEGIS - Automated Erasure and Guarded Information Suite

cd /d "%~dp0"

:: Check for Administrator elevation (required for Win32 direct block IOCTL)
net session >nul 2>&1
if %errorLevel% equ 0 goto :admin_ok

echo [INFO] Requesting Administrator Privileges for Direct Hardware Access...
powershell -NoProfile -Command "Start-Process '%~f0' -ArgumentList '%*' -Verb RunAs" 2>nul
if %errorLevel% equ 0 exit /b
echo [NOTICE] Administrator elevation bypassed. Running in standard user mode...
goto :start_app

:admin_ok
echo [OK] Running with Administrator Privileges (Hardware IOCTL enabled).

:start_app
echo.
echo =========================================================================
echo    AEGIS: Automated Erasure and Guarded Information Suite
echo    Digital Forensics, Block Sanitization ^& Cryptographic Recovery
echo =========================================================================
echo.

:: Detect Java Runtime (JDK 17 or JDK 21)
if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-21"
) else (
    if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
    )
)

if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

:: Locate Maven Command
set "MVN_CMD=mvn"
where mvn >nul 2>&1
if %errorLevel% equ 0 goto :check_mode

if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" (
    set "MVN_CMD=C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd"
    goto :check_mode
)

if exist "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" (
    set "MVN_CMD=C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd"
    goto :check_mode
)

:check_mode
:: Parse launch mode from argument or prompt user
set "MODE=%~1"
if /i "%MODE%"=="--cli" goto :launch_cli
if /i "%MODE%"=="cli" goto :launch_cli
if /i "%MODE%"=="--gui" goto :launch_gui
if /i "%MODE%"=="gui" goto :launch_gui

echo Choose Launch Interface:
echo   [1] Desktop Graphical User Interface (JavaFX GUI) -- Default
echo   [2] Forensic Interactive Command Line Interface (CLI)
echo.
set /p "CHOICE=Enter selection [1/2, default: 1]: "
if "%CHOICE%"=="2" goto :launch_cli

:launch_gui
echo [LAUNCH] Starting AEGIS Desktop GUI Application...
call "%MVN_CMD%" compile exec:java -Dexec.mainClass="com.aegis.ui.GuiLauncher"
goto :done

:launch_cli
echo [LAUNCH] Starting AEGIS Forensic CLI Terminal...
call "%MVN_CMD%" compile exec:java -Dexec.mainClass="com.aegis.Main" -Dexec.args="--cli"
goto :done

:done
if %errorLevel% neq 0 (
    echo.
    echo [ERROR] Application exited with error code %errorLevel%.
    pause
)
