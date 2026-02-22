```
Module-5 Block 17 — Temporal Trace Export & Viewer
```

Branch: `module-5-block-17-pr-draft`

## Summary

This draft documents the work for Module‑5 Block 17: adding deterministic temporal trace export and a minimal offline viewer. The goal is to provide reproducible, pure-data trace exports (JSON and CSV) from the runtime's `TemporalTrace` and to surface an External API endpoint for controlled export access. A tiny static viewer has been added for quick offline inspection.

## Architectural intent

- Record correlated runtime artifacts per tick (events, telemetry, invariants, diffs, subsystem snapshots) into a `TemporalTrace`.
- Provide deterministic, pure-data exports (JSON list of dicts, and CSV) suitable for offline analysis and ingestion by other tools.
- Surface a read-only External API method to request trace exports with `AccessContext` enforcement; restricted contexts are blocked.
- Ship a minimal `docs/trace_viewer/index.html` that loads a JSON trace file and renders entries for manual inspection.

## Files added / modified

- `src/runtime/TemporalTrace.py` — TraceEntry, TemporalTrace, `export_json()`, `export_csv()`
- `src/runtime/ExternalAPI.py` — `export_trace()` facade and `AccessContext` enforcement
- `src/runtime/SnapshotDiff.py` — (previous block) deterministic diff helpers, integrated into runtime
- `src/runtime/Runtime.py` — (integrations) temporal trace recording and diff publication
- `docs/trace_viewer/index.html` — static file-input based trace viewer
- `tests/runtime/test_trace_export.py` — unit tests validating JSON/CSV export determinism/purity and API access control

## Tests

- `tests/runtime/test_trace_export.py` — ensures `TemporalTrace.export_json()` returns a pure-data list and `export_csv()` returns CSV with predictable header; also validates API access control blocks restricted contexts.

## Determinism & purity notes

- The trace export output contains only built-in types (ints, strings, lists, dicts); no live objects or unserializable references are present.
- Ordering is deterministic: ticks are recorded in numeric order and snapshot/diff entries use deterministic key ordering to ensure repeatable exports across runs on the same deterministic runtime.

## API exposure & security

- `ExternalAPI.export_trace(fmt, context)` enforces `AccessContext`. Calls with `AccessContext.RESTRICTED` will raise `PermissionError`.
- The API is intentionally read-only; no runtime state mutation occurs via export calls.

## How to review

1. Checkout branch `module-5-block-17-pr-draft`.
2. Run tests: `pytest -q`
3. Open `docs/trace_viewer/index.html` in a browser and load the JSON export produced by the running system (or use a saved JSON exported via `TemporalTrace.export_json()`).

## Notes / follow-ups

- Consider expanding the trace viewer to include timeline scrubbing and filtering for larger traces (Block‑18 candidate).
- Add explicit non-regression tests for export size / schema stability if downstream consumers are planned.

---
Generated locally as a PR draft; no remote push performed.
