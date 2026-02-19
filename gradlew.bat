@echo off
REM gradlew.bat - local-first wrapper
REM Behavior:
REM  - If 'gradle' is on PATH, invoke it with passed args.
REM  - Else, download Gradle 8.2.1 locally to %TEMP% and run it without installing.
REM  - This script does NOT modify git or commit files.

setlocal enabledelayedexpansion
set GRADLE_VERSION=8.2.1

where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)

REM No gradle on PATH — prepare local unpack
set DOWNLOAD_DIR=%TEMP%\gradle-%GRADLE_VERSION%
set ZIP_PATH=%TEMP%\gradle-%GRADLE_VERSION%-bin.zip
if not exist "%DOWNLOAD_DIR%\gradle-%GRADLE_VERSION%" (
  echo Downloading Gradle %GRADLE_VERSION% to %ZIP_PATH% ...
  powershell -Command "try { (New-Object System.Net.WebClient).DownloadFile('https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip','%ZIP_PATH%') } catch { exit 2 }"
  if %ERRORLEVEL% NEQ 0 (
    echo Failed to download Gradle. Please install Gradle or provide a proper gradlew wrapper.
    exit /b 2
  )
  echo Extracting to %DOWNLOAD_DIR% ...
  powershell -Command "try { Expand-Archive -Path '%ZIP_PATH%' -DestinationPath '%DOWNLOAD_DIR%' -Force } catch { exit 3 }"
  if %ERRORLEVEL% NEQ 0 (
    echo Failed to extract Gradle distribution.
    exit /b 3
  )
)

set GRADLE_HOME=%DOWNLOAD_DIR%\gradle-%GRADLE_VERSION%
set GRADLE_CMD=%GRADLE_HOME%\bin\gradle.bat
if not exist "%GRADLE_CMD%" (
  echo Expected gradle executable not found in %GRADLE_HOME%\bin
  exit /b 4
)

"%GRADLE_CMD%" %*
exit /b %ERRORLEVEL%
