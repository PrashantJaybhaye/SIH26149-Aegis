@echo off
setlocal enabledelayedexpansion
title Forensix - Secure Data Erasure ^& File Recovery Platform

:: Auto-elevate to Administrator privileges if not already running as Admin
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [INFO] Requesting Administrator Privileges...
    powershell -Command "Start-Process -FilePath '%~f0' -WorkingDirectory '%~dp0' -Verb RunAs"
    exit /b
)

cd /d "%~dp0"

echo =========================================================================
echo    FORENSIX: Secure Data Erasure ^& Advanced File Recovery Platform
echo    National Technical Research Organisation (NTRO) - SIH26149
echo =========================================================================
echo [OK] Running with Administrator Privileges (Raw Block IOCTL enabled)
echo.

:: Detect Maven executable
where mvn >nul 2>&1
if %errorLevel% == 0 (
    set "MVN_CMD=mvn"
) else (
    if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" (
        set "MVN_CMD=C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd"
    ) else if exist "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" (
        set "MVN_CMD=C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd"
    ) else (
        echo [ERROR] Maven not found on PATH or in standard IntelliJ locations.
        echo         Please install Apache Maven or run the project directly from IntelliJ IDEA.
        pause
        exit /b 1
    )
)

echo Starting Forensix Application...
echo.
"!MVN_CMD!" exec:java -Dexec.mainClass="com.forensix.Main"
pause
