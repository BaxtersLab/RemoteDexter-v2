@echo off
echo ========================================
echo RemoteDexter Complete Release Pipeline
echo A2.2.17 - APK Build, Signing, Validation
echo ========================================

echo.
echo Step 1: Setup Release Environment
call setup-release-env.bat
if %errorlevel% neq 0 goto :error

echo.
echo Step 2: Generate Keystore (if needed)
if not exist "keystore\remotedexter-release.jks" (
    echo Generating keystore...
    keytool -genkeypair -alias remotedexter-release -keyalg RSA -keysize 2048 -validity 10000 -keystore keystore/remotedexter-release.jks -storepass RemoteDexter2026 -keypass RemoteDexter2026 -dname "CN=RemoteDexter, OU=Engineering, O=RemoteDexter, L=San Francisco, ST=CA, C=US" -noprompt
    if %errorlevel% neq 0 goto :error
) else (
    echo Keystore already exists, skipping generation...
)

echo.
echo Step 3: Build Release APK
call build-release.bat
if %errorlevel% neq 0 goto :error

echo.
echo Step 4: Create Distribution Package
call create-distribution.bat
if %errorlevel% neq 0 goto :error

echo.
echo Step 5: Final Validation
call validate-release.bat
if %errorlevel% neq 0 goto :error

echo.
echo ========================================
echo RELEASE PIPELINE COMPLETED SUCCESSFULLY!
echo ========================================
echo.
echo RemoteDexter v1.0.0 is ready for distribution!
echo.
echo Distribution package location: distribution\
echo.
echo Manual testing checklist:
echo [ ] Install APK on physical Android device
echo [ ] Complete onboarding flow
echo [ ] Test all transport methods
echo [ ] Verify diagnostics panel functionality
echo [ ] Test streaming performance
echo [ ] Confirm no crashes or errors
echo.
echo Once manual testing is complete, proceed with distribution.
goto :end

:error
echo.
echo RELEASE PIPELINE FAILED!
echo Check the error messages above and resolve issues.
exit /b 1

:end