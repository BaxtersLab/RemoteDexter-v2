from dataclasses import dataclass, asdict
from typing import Any, Dict, List


@dataclass(frozen=True)
class DiffEntry:
    subsystem: str
    field: str
    before: Any
    after: Any
    change_type: str  # 'added'|'removed'|'modified'|'unchanged'


@dataclass(frozen=True)
class DiffResult:
    entries: List[DiffEntry]


def classify_change(before, after) -> str:
    if before is None and after is None:
        return 'unchanged'
    if before is None and after is not None:
        return 'added'
    if before is not None and after is None:
        return 'removed'
    if before != after:
        return 'modified'
    return 'unchanged'


def diff_subsystem_states(prev_state: Dict[str, Any], next_state: Dict[str, Any], subsystem_name: str) -> List[DiffEntry]:
    entries: List[DiffEntry] = []
    # deterministic ordering: keys from prev_state then keys only in next_state
    prev_keys = list(prev_state.keys()) if isinstance(prev_state, dict) else []
    next_keys = list(next_state.keys()) if isinstance(next_state, dict) else []

    for k in prev_keys:
        before = prev_state.get(k)
        after = next_state.get(k)
        change = classify_change(before, after)
        entries.append(DiffEntry(subsystem=subsystem_name, field=k, before=before, after=after, change_type=change))

    for k in next_keys:
        if k in prev_keys:
            continue
        before = prev_state.get(k) if isinstance(prev_state, dict) else None
        after = next_state.get(k)
        change = classify_change(before, after)
        entries.append(DiffEntry(subsystem=subsystem_name, field=k, before=before, after=after, change_type=change))

    return entries


def diff_snapshots(before: Dict[str, Any], after: Dict[str, Any]) -> DiffResult:
    """Compute deterministic structural diff between two serialized snapshots.

    `before` and `after` expected to be dicts in the form {'subsystems': {name: state_dict}}
    """
    entries: List[DiffEntry] = []
    b_subs = (before.get('subsystems', {}) if isinstance(before, dict) else {})
    a_subs = (after.get('subsystems', {}) if isinstance(after, dict) else {})

    # deterministic subsystem ordering: use keys from before then keys only in after
    keys = list(b_subs.keys())
    for k in keys:
        prev = b_subs.get(k, {}) or {}
        nxt = a_subs.get(k, {}) or {}
        entries.extend(diff_subsystem_states(prev, nxt, k))

    for k in a_subs.keys():
        if k in keys:
            continue
        prev = b_subs.get(k, {}) or {}
        nxt = a_subs.get(k, {}) or {}
        entries.extend(diff_subsystem_states(prev, nxt, k))

    return DiffResult(entries=entries)


def as_pure_data(diff: DiffResult) -> Dict[str, Any]:
    # Convert to JSON-safe dict with deterministic ordering
    out = {'entries': []}
    for e in diff.entries:
        out['entries'].append({'subsystem': e.subsystem, 'field': e.field, 'before': e.before, 'after': e.after, 'change_type': e.change_type})
    return out


__all__ = ["DiffEntry", "DiffResult", "diff_snapshots", "diff_subsystem_states", "classify_change", "as_pure_data"]
