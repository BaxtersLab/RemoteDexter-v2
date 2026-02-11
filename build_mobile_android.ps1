# Build Android Mobile Application

# Ensure Android SDK and JDK are available (assume Android Studio is installed)
# This script assumes gradlew is present in the mobile project

# Navigate to Android app directory
$androidDir = Join-Path $PSScriptRoot "src\mobile\android\app"
if (-not (Test-Path $androidDir)) {
    Write-Host "Android source directory not found."
    exit 1
}

Set-Location $androidDir

# Build APK
Write-Host "Building Android APK..."
.\gradlew assembleDebug

# Check for errors
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed."
    exit 1
}

Write-Host "Build successful. APK is in app/build/outputs/apk/debug/"