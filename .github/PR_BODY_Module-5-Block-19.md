```copyable
Module-5 Block-19: Timeline Aggregation — formal PR body
Branch: module-5-block-17-pr-draft
Files: TimelineQuery, LifecycleMap, TemporalTrace, SubsystemManager, viewer, tests
```

**Title**: Module-5 — Big Block 19: Timeline Aggregation, Paging, and Summary

**Summary**
- Adds timeline aggregation, pagination, and summary helpers to the runtime observability stack.
- Correlates lifecycle events (via `LifecycleMap`) into `TemporalTrace` entries to enable lifecycle-aware timeline queries.
- Exposes timeline query helpers to `Introspection` and `ExternalAPI` for read-only inspection.

**Scope / Motivation**
- Long runtime traces become unwieldy; aggregation and paging let tools and reviewers focus on relevant windows, top subsystems, and event-type distributions.
- Lifecycle correlation is necessary to tie subsystem lifecycle transitions to runtime trace ticks for replay and diagnostics.

**High-level Changes**
- `src/runtime/TimelineQuery.py`: filtering, pagination, aggregation by tick/subsystem/event-type, and summary metrics.
- `src/runtime/LifecycleMap.py`: deterministic lifecycle event IDs and normalized timestamps.
- `src/runtime/TemporalTrace.py`: include `lifecycle_refs` on `TraceEntry` and update JSON/CSV export helpers.
- `src/runtime/SubsystemManager.py`: record lifecycle events around subsystem lifecycle hooks.
- `src/runtime/Introspection.py` & `src/runtime/ExternalAPI.py`: query surface for timeline and lifecycle data.
- `docs/timeline_viewer/index.html`: small static viewer with pagination controls for local inspection.
- Tests added under `tests/runtime/` validating aggregation, paging, lifecycle correlation, and exports.

**Files Changed (representative)**
- `src/runtime/TimelineQuery.py`
- `src/runtime/LifecycleMap.py`
- `src/runtime/TemporalTrace.py`
- `src/runtime/SubsystemManager.py`
- `src/runtime/Introspection.py`
- `src/runtime/ExternalAPI.py`
- `docs/timeline_viewer/index.html`
- `tests/runtime/test_timeline_query.py`
- `tests/runtime/test_timeline_aggregation.py`

**Testing Performed**
- Full test suite run: `pytest -q` → 44 passed, 17 warnings.
- New tests validate deterministic aggregation and paging behaviors and that `lifecycle_refs` are attached to trace entries.

**Checklist (for PR)**
- [x] Branch contains only intended source & test files (no bytecode).
- [x] All tests pass locally (`pytest -q`).
- [x] PR draft committed with copyable metadata block.
- [ ] Add PR reviewers and link related Module-5 PRs.
- [ ] Optional: polish `docs/timeline_viewer` UX and add e2e viewer tests.

**Risks / Notes**
- Changes are primarily additive; API is read-only and gated by `AccessContext`.
- The viewer is static and intended for local inspection only; no server-side changes were made.

**How to review**
1. Inspect `src/runtime/TimelineQuery.py` for filtering and aggregation semantics.
2. Verify `tests/runtime/test_timeline_aggregation.py` for expected behavior and determinism.
3. Spot-check `TemporalTrace` exports for `lifecycle_refs` presence.

**Next steps (post-merge)**
- Improve viewer UX; add visual aggregation summary charts.
- Add telemetry tests and continuous trace export validation.

Signed-off-by: Module-5 implementer
