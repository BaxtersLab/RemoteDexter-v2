# RD Mobile Android Application

This directory contains the Android source code for the RD Mobile Application.

## Structure
- app/: Main app module
  - src/main/: Source code and resources
  - build.gradle: Build configuration

## Block H Smoke Test
- Runbook: [docs/BLOCK_H_HOMELAB_SMOKE_TEST.md](docs/BLOCK_H_HOMELAB_SMOKE_TEST.md)
- Harness script: [scripts/homelab_smoke_test.ps1](scripts/homelab_smoke_test.ps1)

Example flow:
- Start capture: `pwsh ./scripts/homelab_smoke_test.ps1 -LogDir ./logs/mobile -PackageName com.remotedexter.mobile -MetricsFile ./logs/mobile/block_h_metrics.json -ResultFile ./logs/mobile/block_h_result.json -StartLogcat`
- Perform manual smoke actions on device + homelab target.
- Stop + evaluate: `pwsh ./scripts/homelab_smoke_test.ps1 -LogDir ./logs/mobile -PackageName com.remotedexter.mobile -MetricsFile ./logs/mobile/block_h_metrics.json -ResultFile ./logs/mobile/block_h_result.json -StopLogcat -EvaluateOnly`