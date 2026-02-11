# Build Instructions

This document outlines how to build the RD Desktop Application and RD Mobile Application from source.

## Prerequisites

- Go 1.21+ for desktop builds
- Android Studio and JDK for Android mobile builds
- Xcode and macOS for iOS mobile builds

## RD Desktop Application (Go)

The desktop application is built using Go.

### Build Script

Run `build_desktop.ps1` to build the executable.

### Manual Build

1. Navigate to the root directory.
2. Run `go mod tidy` to download dependencies.
3. Run `go build ./...` to build all modules.
4. The executable will be in the current directory.

## RD Mobile Application (Android)

The Android app is built using Gradle.

### Build Script

Run `build_mobile_android.ps1` to build the APK.

### Manual Build

1. Open the Android project in Android Studio.
2. Sync Gradle files.
3. Build > Build Bundle(s)/APK(s) > Build APK(s).

## RD Mobile Application (iOS)

The iOS app requires Xcode on macOS.

### Build Instructions

1. Open the iOS project in Xcode.
2. Select the target device/simulator.
3. Product > Build.

Note: iOS builds cannot be scripted on Windows. Use a Mac for iOS development.

## Packaging

- Desktop: Use the executable directly or package with tools like Inno Setup.
- Android: Use the generated APK.
- iOS: Archive and distribute via TestFlight or App Store.

## Security Notes

- Builds do not include any hardcoded endpoints.
- No telemetry or tracking is added during build.
- All dependencies are user-verifiable.