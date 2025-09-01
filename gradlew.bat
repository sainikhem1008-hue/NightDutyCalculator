@echo off
rem -----------------------------------------------------------------------------
rem Gradle Startup Script for Windows
rem -----------------------------------------------------------------------------

setlocal

set SCRIPT_DIR=%~dp0
set WRAPPER_JAR=%SCRIPT_DIR%gradle\wrapper\gradle-wrapper.jar

if not exist "%WRAPPER_JAR%" (
    echo Gradle wrapper jar not found: %WRAPPER_JAR%
    exit /b 1
)

if "%JAVA_HOME%" == "" (
    set JAVA_CMD=java
) else (
    set JAVA_CMD=%JAVA_HOME%\bin\java
)

"%JAVA_CMD%" -jar "%WRAPPER_JAR%" %*
endlocal

