# Changelog — 2026-02-18

## ci/fix-lifeline-endblock-20260218
  - Ensure `END` block yields `Status.terminated` and is allowed to have empty transitions.
 - Tests: expanded telemetry unit tests to cover idempotence, concurrency separation, non-blocking telemetry behavior, strict ordering, and escalation/error-path visibility (test-only changes).
   - These tests assert that `Telemetry` hooks (`onTransition`, `onRecoveryAttempt`, `onEnd`) are invoked deterministically and do not affect lifeline semantics even when telemetry handlers are slow or concurrent.

Signed-off-by: GitHub Copilot (agent)
