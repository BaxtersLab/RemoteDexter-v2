@echo off
echo Creating RemoteDexter Distribution Package...

REM Create distribution directory
if exist "distribution" rmdir /s /q distribution
mkdir distribution

REM Copy APK
if exist "app\build\outputs\apk\release\app-release.apk" (
    copy "app\build\outputs\apk\release\app-release.apk" "distribution\RemoteDexter_v1.0.0.apk"
    echo ✅ APK copied
) else (
    echo ❌ APK not found! Run build-release.bat first
    exit /b 1
)

REM Copy documentation
copy "..\release-notes.txt" "distribution\"
copy "..\CHANGELOG.md" "distribution\"
copy "..\LICENSE" "distribution\"
copy "..\README.md" "distribution\"
copy "..\diagnostics-sample.json" "distribution\"

echo ✅ Documentation copied

REM Create installation instructions
echo # RemoteDexter Installation Guide > distribution\INSTALL.md
echo. >> distribution\INSTALL.md
echo ## Quick Start >> distribution\INSTALL.md
echo. >> distribution\INSTALL.md
echo 1. Enable "Install from Unknown Sources" in Android Settings >> distribution\INSTALL.md
echo 2. Install RemoteDexter_v1.0.0.apk >> distribution\INSTALL.md
echo 3. Launch the app and complete onboarding >> distribution\INSTALL.md
echo 4. Select your preferred transport method >> distribution\INSTALL.md
echo 5. Test the connection >> distribution\INSTALL.md
echo 6. Start using RemoteDexter! >> distribution\INSTALL.md
echo. >> distribution\INSTALL.md
echo ## Troubleshooting >> distribution\INSTALL.md
echo. >> distribution\INSTALL.md
echo - If installation fails, check available storage space >> distribution\INSTALL.md
echo - For connection issues, try different transport methods >> distribution\INSTALL.md
echo - Use the diagnostics panel for detailed troubleshooting >> distribution\INSTALL.md

echo ✅ Installation guide created

REM Create checksums
echo Generating checksums...
certutil -hashfile "distribution\RemoteDexter_v1.0.0.apk" SHA256 > distribution\checksums.txt
echo. >> distribution\checksums.txt
echo SHA256 checksums for verification: >> distribution\checksums.txt
echo RemoteDexter_v1.0.0.apk >> distribution\checksums.txt
certutil -hashfile "distribution\RemoteDexter_v1.0.0.apk" SHA256 >> distribution\checksums.txt

echo ✅ Checksums generated

REM List distribution contents
echo.
echo 📦 Distribution package created successfully!
echo Contents of distribution\:
dir /b distribution\

echo.
echo 🎉 RemoteDexter v1.0.0 is ready for distribution!
echo.
echo Next steps:
echo 1. Test the APK on a physical device
echo 2. Upload to distribution platform (GitHub, direct download, etc.)
echo 3. Share the installation guide with users