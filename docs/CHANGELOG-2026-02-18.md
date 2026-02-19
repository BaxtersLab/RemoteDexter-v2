# Changelog — 2026-02-18

## ci/fix-lifeline-endblock-20260218
- Fix: deterministic END handling and lifeline recovery semantics
  - Ensure `END` block yields `Status.terminated` and is allowed to have empty transitions.
  - Make lifeline decisions deterministic for `fail`, `deadEnd`, and `escalate` paths.
  - ExecutionEngine now respects `deadEnd:*` and `escalated` as terminal states and will perform in-run recovery when lifeline sets a recovery pointer.
- Tests: contract tests updated to assert strict termination and recovery counts.
- Telemetry: lightweight hooks added (`Telemetry` interface) for transitions, recovery attempts, and END emission.

Signed-off-by: GitHub Copilot (agent)
