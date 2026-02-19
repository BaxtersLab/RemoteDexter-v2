Title: Add telemetry contract tests to CI and stabilize telemetry runs

Background
Extended telemetry unit tests were added to `core` to validate idempotence, concurrency separation, non-blocking behavior, strict ordering, and escalation/error handling of the telemetry contract (`onTransition`, `onRecoveryAttempt`, `onEnd`). They pass locally but are not yet enforced in CI.

Goal
Ensure telemetry contract tests run in CI reliably to prevent regressions and provide actionable failure artifacts.

Proposed approach
- Add a targeted CI job that runs only the telemetry tests to keep runtime small:

	- Gradle command: `./gradlew :core:test --tests "*TelemetryTest*"`

	- Place the job in the existing test matrix or as a separate job that runs on push/PR to `main`.

- Increase test timeouts for the CI job or mark the slow test with a longer timeout so it doesn't flake.
- Publish test reports and attach them to job artifacts for debugging.
- Add a docs page (or extend `docs/HANDOFF.md`) describing the telemetry contract and test expectations.

Checklist
- [ ] Add a GitHub Actions job YAML snippet that runs the telemetry tests and uploads test reports.
- [ ] Configure a timeout long enough for SlowTelemetry patterns (suggest 10m job timeout for this job).
- [ ] Add documentation of the telemetry contract to `docs/HANDOFF.md` or `docs/telemetry.md`.
- [ ] Monitor CI for one week and address any flakes.

Acceptance criteria
- Telemetry tests run in CI and pass on `main` for the telemetry job.
- Test artifacts (JUnit report) are uploaded when failures occur.

Labels: testing, ci, enhance
Assignee: @maintainer

Optional: I can prepare the GitHub Actions job YAML and the exact Gradle invocation to paste into `.github/workflows/ci.yml`.