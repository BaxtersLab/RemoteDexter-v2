PR update: ci/fix-lifeline-endblock-20260218

Summary
-------
- Adds deterministic END block handling and minimal telemetry hooks.
- Replaces string lifeline status values with a typed `LifelineStatus` enum.
- Implements in-run recovery semantics with bounded attempts and escalation.
- Adds contract tests covering: END termination, illegal-transition rejection,
  recovery path, interrupt routing, and recovery-escalation on exceeded attempts.

Key changes
-----------
- `core`: `LifelineStatus.kt`, `EndBlock.kt`, `Telemetry.kt` (new)
- `core`: `ExecutionEngine.kt`, `Constitution.kt`, `LifelineProtocol.kt` (modified)
- `core`: `ExecutionEngineContractTest.kt` — added/updated contract tests

Testing
-------
- Ran `:core:test` locally; all core contract tests passed including the
  interrupt routing test that now conforms to constitutional `noSlop` rules.

Notes for PR description
------------------------
- The interrupt contract test was updated to declare `outputs` and an explicit
  interrupt transition for `alert` so it is valid under `noSlop` in `Constitution`.
- `LifelineStatus` migration is complete and compiled successfully; no behavioral
  regressions observed in core tests.
- CI: please run full `./gradlew build` to validate across modules.

Request
-------
Please update the GitHub PR description with the above summary and testing notes.
