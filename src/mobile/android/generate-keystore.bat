@echo off
echo Generating RemoteDexter release keystore...

REM Generate keystore for RemoteDexter release builds
keytool -genkeypair ^
  -alias remotedexter-release ^
  -keyalg RSA ^
  -keysize 2048 ^
  -validity 10000 ^
  -keystore keystore/remotedexter-release.jks ^
  -storepass RemoteDexter2026 ^
  -keypass RemoteDexter2026 ^
  -dname "CN=RemoteDexter, OU=Engineering, O=RemoteDexter, L=San Francisco, ST=CA, C=US"

echo Keystore generated successfully!
echo.
echo IMPORTANT: In production, use secure passwords and backup this keystore!
echo.
echo Environment variables to set for signing:
echo RD_KEYSTORE_PATH=%CD%\keystore\remotedexter-release.jks
echo RD_KEYSTORE_ALIAS=remotedexter-release
echo RD_KEYSTORE_PASSWORD=RemoteDexter2026
echo RD_KEY_PASSWORD=RemoteDexter2026