@echo off
echo Building RemoteDexter Release APK...

REM Setup environment
call setup-release-env.bat
if %errorlevel% neq 0 exit /b 1

REM Clean previous builds
echo Cleaning previous builds...
call gradlew clean
if %errorlevel% neq 0 exit /b 1

REM Build release APK
echo Building release APK...
call gradlew assembleRelease
if %errorlevel% neq 0 exit /b 1

REM Check if APK was generated
if exist "app\build\outputs\apk\release\app-release.apk" (
    echo.
    echo ✅ SUCCESS: Release APK generated!
    echo Location: app\build\outputs\apk\release\app-release.apk
    echo.
    echo APK Details:
    dir "app\build\outputs\apk\release\app-release.apk"
) else (
    echo.
    echo ❌ FAILED: Release APK not found
    echo Check build logs above for errors
    exit /b 1
)

echo.
echo Ready for distribution!