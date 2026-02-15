@echo off
echo Setting up RemoteDexter release environment...

REM Set environment variables for release signing
set RD_KEYSTORE_PATH=%~dp0keystore\remotedexter-release.jks
set RD_KEYSTORE_ALIAS=remotedexter-release
set RD_KEYSTORE_PASSWORD=RemoteDexter2026
set RD_KEY_PASSWORD=RemoteDexter2026

REM Set deterministic build timestamp
set SOURCE_DATE_EPOCH=1706745600

REM Resolve Android SDK path
set SDK_PATH=
if defined ANDROID_HOME if exist "%ANDROID_HOME%" set SDK_PATH=%ANDROID_HOME%
if not defined SDK_PATH if defined ANDROID_SDK_ROOT if exist "%ANDROID_SDK_ROOT%" set SDK_PATH=%ANDROID_SDK_ROOT%
if not defined SDK_PATH if exist "%LOCALAPPDATA%\Android\Sdk" set SDK_PATH=%LOCALAPPDATA%\Android\Sdk
if not defined SDK_PATH if exist "%USERPROFILE%\AppData\Local\Android\Sdk" set SDK_PATH=%USERPROFILE%\AppData\Local\Android\Sdk
if not defined SDK_PATH if exist "C:\Program Files\Android\android-sdk" set SDK_PATH=C:\Program Files\Android\android-sdk
if not defined SDK_PATH if exist "C:\Program Files\Android\Sdk" set SDK_PATH=C:\Program Files\Android\Sdk
if not defined SDK_PATH if exist "C:\Program Files (x86)\Android\android-sdk" set SDK_PATH=C:\Program Files (x86)\Android\android-sdk
if not defined SDK_PATH if exist "C:\Program Files (x86)\Android\Sdk" set SDK_PATH=C:\Program Files (x86)\Android\Sdk
if not defined SDK_PATH if exist "C:\Android\Sdk" set SDK_PATH=C:\Android\Sdk

if not defined SDK_PATH (
	echo ERROR: Android SDK location not found.
	echo Set ANDROID_HOME or ANDROID_SDK_ROOT, or install SDK at %%LOCALAPPDATA%%\Android\Sdk.
	exit /b 1
)

set ANDROID_HOME=%SDK_PATH%
set ANDROID_SDK_ROOT=%SDK_PATH%
set SDK_ESCAPED=%SDK_PATH:\=\\%
> "%~dp0local.properties" echo sdk.dir=%SDK_ESCAPED%

echo Environment variables set:
echo RD_KEYSTORE_PATH=%RD_KEYSTORE_PATH%
echo RD_KEYSTORE_ALIAS=%RD_KEYSTORE_ALIAS%
echo RD_KEYSTORE_PASSWORD=%RD_KEYSTORE_PASSWORD%
echo RD_KEY_PASSWORD=%RD_KEY_PASSWORD%
echo SOURCE_DATE_EPOCH=%SOURCE_DATE_EPOCH%
echo ANDROID_HOME=%ANDROID_HOME%

echo.
echo Ready to build release APK!
exit /b 0