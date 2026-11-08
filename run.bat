@echo off
title Forensix - Data Recovery and Forensic Sanitizer

cd /d "%~dp0"

net session >nul 2>&1
if %errorLevel% equ 0 goto :admin_ok

echo [INFO] Requesting Administrator Privileges...
powershell -NoProfile -Command "Start-Process '%~f0' -Verb RunAs" 2>nul
if %errorLevel% equ 0 exit /b
echo [NOTICE] Administrator elevation bypassed. Running in standard user mode...
goto :start_app

:admin_ok
echo [OK] Running with Administrator Privileges (Raw Block IOCTL enabled).

:start_app
echo.
echo =========================================================================
echo    FORENSIX: Data Recovery and Deep Forensic Carver Platform
echo =========================================================================
echo.

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

set "MVN_CMD=mvn"
where mvn >nul 2>&1
if %errorLevel% equ 0 goto :launch

if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" (
    set "MVN_CMD=C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd"
    goto :launch
)

if exist "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" (
    set "MVN_CMD=C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd"
    goto :launch
)

:launch
echo Starting Forensix Desktop Application (JavaFX GUI)...
call "%MVN_CMD%" compile exec:java
if %errorLevel% neq 0 (
    echo.
    echo [ERROR] Application exited with code %errorLevel%.
    pause
)
