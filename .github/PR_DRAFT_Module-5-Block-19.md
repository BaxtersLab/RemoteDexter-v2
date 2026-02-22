```copyable
Module-5 Block-19: Timeline Aggregation
Branch: module-5-block-17-pr-draft
Files: TimelineQuery, LifecycleMap, TemporalTrace, SubsystemManager, viewer, tests
```

# PR Draft: Module-5 — Big Block 19 (Timeline Aggregation)

Summary
-------
- This PR implements timeline aggregation and paging features for the Module‑5 runtime observability work.
- It extends the `TemporalTrace` exports with richer query support and integrates lifecycle references recorded by `LifecycleMap`.

Key changes
-----------
- Add `src/runtime/TimelineQuery.py`: filtering, pagination, aggregation (by tick, subsystem, event type), and summary helpers.
- Extend runtime components to expose timeline data to the External API and Introspection layers.
- Add viewer UI: `docs/timeline_viewer/index.html` with pagination controls for local static review.
- Add unit tests: `tests/runtime/test_timeline_query.py`, `tests/runtime/test_timeline_aggregation.py` and related runtime tests to validate behavior and determinism.

Files (high-level)
-------------------
- `src/runtime/TimelineQuery.py` — core query and aggregation logic.
- `src/runtime/LifecycleMap.py` — lifecycle event IDs included in trace entries.
- `src/runtime/SubsystemManager.py` — records lifecycle events around lifecycle hooks.
- `src/runtime/TemporalTrace.py` — trace entries now include `lifecycle_refs` and export helpers.
- `docs/timeline_viewer/index.html` — static viewer for quick inspection.
- Tests under `tests/runtime/` covering trace export, lifecycle correlation, query pagination and aggregation.

Rationale
---------
- Aggregation and paging reduce the visualization surface and enable scalable timeline inspection for long runs.
- Lifecycle correlation allows tracing subsystem lifecycle events to specific trace ticks for improved debugging and replay.
- Tests validate deterministic ordering and guard External API access.

Notes
-----
- The workspace was cleaned of `__pycache__`/`.pyc` artifacts before drafting this PR; unrelated untracked fixture files remain uncommitted.
- This PR is staged on branch: `module-5-block-17-pr-draft` (no remote push performed).

Next steps
----------
1. Optional: polish timeline viewer UX and add end-to-end export/viewer tests.
2. Prepare a formal PR description and open the PR when ready (I can draft the PR body and checklist).

Signed-off-by: Module-5 implementer
