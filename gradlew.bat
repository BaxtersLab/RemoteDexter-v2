@echo off
REM Minimal placeholder gradlew.bat script for RemoteDexter-v2
REM This is a deterministic stub to satisfy CI and developer bootstrap checks.
REM To create the real Gradle wrapper (recommended), run:
REM   gradle wrapper --gradle-version 8.2.1
REM Then commit the generated gradlew, gradlew.bat, and gradle\wrapper\gradle-wrapper.jar files.









)  exit /b 1  echo No 'gradle' on PATH. Install Gradle or regenerate the proper Gradle wrapper.) else (  exit /b %ERRORLEVEL%  gradle %*if %ERRORLEVEL% EQU 0 (nwhere gradle >nul 2>nul