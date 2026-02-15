# Changelog

All notable changes to RemoteDexter will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/SemVer).

## [1.0.0] - 2026-02-14

### Added
- **Core Protocol Implementation**
  - 4-byte big-endian framing for all message types
  - CommandRequest/CommandResponse serialization
  - Noise protocol handshake implementation
  - Deterministic message routing

- **Multi-Transport Support**
  - USB transport with bulk transfer optimization
  - Bluetooth transport with RFCOMM sockets
  - Wi-Fi Direct transport with socket streams
  - Automatic transport priority fallback
  - Exponential backoff reconnection

- **Streaming Pipeline**
  - Screen capture via MediaProjection API
  - Frame encoding with RustDesk integration
  - 60 FPS streaming with frame drop detection
  - Buffered I/O with dedicated threads

- **Productization Layer**
  - Complete onboarding flow with transport selection
  - Always-visible connection status indicator
  - Comprehensive diagnostics panel
  - Developer mode with chaos testing tools
  - Professional UI with consistent styling

- **Observability & Telemetry**
  - MetricsRegistry with atomic counters
  - Structured JSON logging
  - Transport health dashboard
  - Real-time performance monitoring
  - Alert system for connection issues

- **Stability Features**
  - Watchdog monitoring for all transports
  - Chaos testing framework for stress validation
  - Memory leak prevention
  - Thread lifecycle management
  - Graceful error recovery

### Technical Details
- **Protocol**: Custom framed protocol with Noise encryption
- **Platform**: Android API 24+ (7.0+)
- **Architecture**: Kotlin with JNI integration
- **Build System**: Gradle with release optimization
- **Security**: ProGuard obfuscation, signed releases

### Performance
- Startup time: <3 seconds
- Memory usage: ~100MB baseline
- Streaming: 60 FPS target
- RTT: <50ms local connections

## [0.1.0] - 2026-01-01
### Added
- Initial project structure
- Basic protocol simulation
- Transport abstraction framework
- Core Android integration