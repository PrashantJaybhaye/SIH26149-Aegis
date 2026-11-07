@echo off
setlocal enabledelayedexpansion
title Forensix - Data Recovery & Forensic Sanitizer

:: Auto-elevate to Administrator privileges if not already running as Admin
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [INFO] Requesting Administrator Privileges...
    powershell -Command "Start-Process -FilePath '%~f0' -WorkingDirectory '%~dp0' -Verb RunAs"
    exit /b
)

cd /d "%~dp0"

echo =========================================================================
echo    FORENSIX: Data Recovery & Deep Forensic Carver Platform
echo =========================================================================
echo [OK] Running with Administrator Privileges (Raw Block IOCTL enabled)
echo.

:: Detect and configure JDK (Prefer JDK 21 matching JavaFX 21, fallback to JDK 17)
if exist "C:\Program Files\Java\jdk-21" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-21"
    set "PATH=C:\Program Files\Java\jdk-21\bin;!PATH!"
) else if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
    set "PATH=C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot\bin;!PATH!"
)

set "MVN_CMD=mvn"
where mvn >nul 2>&1
if %errorLevel% neq 0 (
    if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" (
        set "MVN_CMD=C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd"
    ) else (
        if exist "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" (
            set "MVN_CMD=C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd"
        )
    )
)

echo Starting Forensix Desktop Application (JavaFX GUI)...
"%MVN_CMD%" exec:java
if %errorLevel% neq 0 (
    echo.
    echo [ERROR] Application exited with code %errorLevel%.
    pause
)
