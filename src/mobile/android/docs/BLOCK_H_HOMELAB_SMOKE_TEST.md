# Block H — Homelab Smoke Test (Authoritative)

This runbook validates the Android client against the authoritative homelab target (user PC) using the finalized Block H policy.

## Scope
- Android -> RemoteDexter -> homelab (user PC)
- Session negotiation completion
- Frame pipeline visibility and stability
- Input pipeline (mouse, keyboard, touch)
- Copy/Pasta (`Ctrl+C`, `Ctrl+V`)
- Bidirectional clipboard sync
- Reconnect behavior (manual and automatic tests)
- Transport reliability during network variability
- Multi-monitor behavior
- Loop safety (no recursion/runaway/retry storms)

## Authoritative Pass/Fail Thresholds
- Max reconnect attempts: `3`
- Acceptable frame drop: `<= 5%`
- Max session setup time: `<= 4 seconds`
- Logs: always collected

## Preconditions
1. Android APK (release) installed on test device.
2. Homelab test target set to user PC.
3. Device and target can reach each other.
4. USB debugging enabled (for log capture).
5. `adb` available on host machine.

## Test Sequence
1. Start log capture.
2. Launch app and begin session to user PC.
3. Validate negotiation completes successfully.
4. Validate remote frame is visible and stable.
5. Validate input pipeline:
   - pointer move + click
   - keyboard input
   - touch gestures
6. Validate Copy/Pasta actions from toolbar and hardware keyboard.
7. Validate clipboard sync both directions.
8. Validate reconnect behavior:
   - Manual reconnect flow
   - Automatic reconnect test flow
9. Validate network variability path:
   - Wi-Fi -> LTE -> Wi-Fi
10. Validate multi-monitor behavior on user PC.
11. End session and verify clean teardown.
12. Stop log capture and evaluate thresholds.

## Metrics Collection
Record these metrics per run:
- `session_setup_ms`
- `reconnect_attempts`
- `frame_drop_percent`
- `manual_reconnect_pass` (true/false)
- `automatic_reconnect_pass` (true/false)
- `clipboard_bidirectional_pass` (true/false)
- `multimonitor_pass` (true/false)
- `network_variability_pass` (true/false)
- `teardown_clean_pass` (true/false)

## Failure Conditions
Fail the run if any of the following is true:
- `session_setup_ms > 4000`
- `reconnect_attempts > 3`
- `frame_drop_percent > 5`
- Any required functional check is false
- Evidence of recursive loops, runaway render loops, or retry storms

## Loop/Storm Safety Checks
Explicitly verify in logs:
- No unbounded reconnect loops
- No recursive handshake retry loops
- No runaway frame render callback loops

## Artifacts
Per test run, store:
- Full `adb logcat` capture
- Structured metrics JSON (`block_h_metrics.json`)
- Evaluation result (`block_h_result.json`)

## Recommended Command Harness
Use the script:
- `scripts/homelab_smoke_test.ps1`

Example:
- `pwsh ./scripts/homelab_smoke_test.ps1 -LogDir ./logs/mobile -PackageName com.remotedexter.mobile -MetricsFile ./logs/mobile/block_h_metrics.json -ResultFile ./logs/mobile/block_h_result.json -StartLogcat`
- Run test actions manually on device/homelab.
- Stop and evaluate:
- `pwsh ./scripts/homelab_smoke_test.ps1 -LogDir ./logs/mobile -PackageName com.remotedexter.mobile -MetricsFile ./logs/mobile/block_h_metrics.json -ResultFile ./logs/mobile/block_h_result.json -StopLogcat -EvaluateOnly`
