# Build iOS Mobile Application

iOS builds require Xcode on macOS. This cannot be scripted on Windows.

## Prerequisites
- macOS with Xcode installed
- iOS project files in /src/mobile

## Steps
1. Open the iOS project in Xcode.
2. Select a target device or simulator.
3. Go to Product > Build to build the app.
4. For distribution: Product > Archive, then distribute via TestFlight or App Store.

## Notes
- Ensure no hardcoded endpoints in the code.
- Verify permissions and entitlements match the security guarantees.