Master Constitution — RemoteDexter v2

This document is the authoritative Master Constitution for RemoteDexter‑v2. It defines governance, architectural constraints, and build‑system rules that supersede v1. All development, CI, and tooling decisions must conform to this Constitution.

§X — The Self‑Check Rule (Check Yourself Before You Wreck Yourself)

Every subsystem must verify its critical assumptions before performing any action that depends on them. If an assumption fails, the subsystem must stop, report the issue, and prevent cascading failure.

Applies to:
- toolchain versions
- environment variables
- module interfaces
- artifact versions
- protocol compatibility
- configuration files
- runtime invariants

Constraints:
- Silent failure is prohibited.
- Unchecked assumptions are prohibited.
- Cascading errors are prohibited.

Requirement:
- All modules, scripts, and transports MUST implement explicit self‑verification steps prior to executing dependent logic. Self‑verification must produce structured diagnostics when checks fail, and must prevent further dependent operations.

§X+1 — Early Failure, Loud Reporting

When a subsystem detects a failed assumption, it MUST fail early, fail safely, and fail loudly.

Requirements:
- Emit a structured diagnostic message (machine‑readable and human‑readable) identifying the specific assumption that failed.
- Halt the dependent operation immediately and prevent side‑effects that could cause drift or corruption.
- Ensure failure is reproducible and observable through the diagnostics API or logging pipeline.

Rationale:
- Early, loud failures prevent v1‑style debugging cascades and reduce time to resolution.
- Clear diagnostics at the point of failure make triage deterministic and portable across developer machines and CI.

Enforcement and Compliance

- All new code, scripts, and CI jobs MUST include a Self‑Check step that validates their critical assumptions.
- The bootstrap script (`scripts/bootstrap-dev.ps1`) is a canonical example of Self‑Check: it must detect and fail on mismatched toolchains rather than attempting silent fallbacks.
- CI pipelines MUST run Self‑Check steps as gate checks before running dependent build or test stages.

Amendments

- This document is authoritative for the duration of RemoteDexter‑v2 development and may be amended only through the governance process defined in the Master Constitution.

§X+2 — No Residual Terminals or Processes

All tasks must terminate any shells, terminals, or background processes they create before completing execution.

Requirements:
- No PowerShell, `pwsh`, `bash`, or `cmd` sessions may remain open after a task finishes unless explicitly authorized by Agent 1.
- No background processes may persist unless explicitly authorized by Agent 1.
- Diagnostics must be emitted if a process cannot be closed; such diagnostics must identify the PID, command, owner, and reason for failure.
- Leaving residual terminals is a constitutional violation and must be remediated immediately.

Rationale:
- Prevents hidden state and drifting environments.
- Ensures reproducible executions and clean developer machines.

Enforcement:
- All automation (scripts, CI jobs, agents) MUST include an explicit cleanup step that terminates processes they spawn.
- Any automated system that cannot terminate a process MUST emit a structured diagnostic and escalate to Agent 1 for authorization.

Acknowledgement:
- Agents and maintainers MUST acknowledge this rule and correct past violations. Recurring violations will trigger governance review.

