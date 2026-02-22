from typing import Any, Dict, List, Optional, Tuple
from . import Introspection


def _in_tick_range(value: int, tick_min: Optional[int], tick_max: Optional[int]) -> bool:
    if tick_min is not None and value < tick_min:
        return False
    if tick_max is not None and value > tick_max:
        return False
    return True


def filter_timeline(runtime, subsystem: Optional[str] = None, phase: Optional[str] = None, tick_min: Optional[int] = None, tick_max: Optional[int] = None, limit: Optional[int] = None, offset: Optional[int] = None) -> Dict[str, Any]:
    """Return a merged, filtered timeline view containing trace entries and lifecycle events.

    Supports offset/limit paging and deterministic ordering.
    """
    trace = Introspection.get_trace(runtime, limit=None)
    lifecycle = Introspection.get_lifecycle(runtime, subsystem=subsystem, phase=phase, limit=None)

    # filter
    filtered_lifecycle = [e for e in lifecycle if _in_tick_range(e.get('tick', 0), tick_min, tick_max)]
    filtered_trace = [t for t in trace if _in_tick_range(t.get('tick', 0), tick_min, tick_max)]

    # deterministic ordering
    filtered_lifecycle.sort(key=lambda e: (e.get('tick', 0), e.get('id', 0)))
    filtered_trace.sort(key=lambda t: (t.get('tick', 0), t.get('timestamp', 0)))

    # apply offset + limit (paging) to trace
    if offset is None:
        offset = 0
    if limit is not None and isinstance(limit, int):
        filtered_trace = filtered_trace[offset: offset + limit]
    else:
        filtered_trace = filtered_trace[offset:]

    return {'trace': filtered_trace, 'lifecycle': filtered_lifecycle}


def paginate_trace(runtime, page: int = 1, page_size: int = 10, **filters) -> Dict[str, Any]:
    """Return a paged slice of the timeline trace with metadata.

    Page numbers are 1-based.
    """
    if page < 1:
        page = 1
    offset = (page - 1) * page_size
    res = filter_timeline(runtime, offset=offset, limit=page_size, **filters)
    # include paging metadata
    full = Introspection.get_trace(runtime, limit=None)
    total = len([t for t in full if _in_tick_range(t.get('tick', 0), filters.get('tick_min', None), filters.get('tick_max', None))])
    return {'page': page, 'page_size': page_size, 'total': total, 'items': res['trace']}


def aggregate_by_tick(runtime, tick_range: Optional[Tuple[int, int]] = None) -> List[Dict[str, Any]]:
    """Return aggregation per tick: counts of events, telemetry, invariants, diffs, subsystems active."""
    trace = Introspection.get_trace(runtime, limit=None)
    agg = {}
    for t in trace:
        tk = t.get('tick', 0)
        if tick_range is not None:
            if tk < tick_range[0] or tk > tick_range[1]:
                continue
        ent = agg.setdefault(tk, {'tick': tk, 'events': 0, 'telemetry': 0, 'invariants': 0, 'diff_entries': 0, 'subsystems': 0})
        ent['events'] += len(t.get('events', []) or [])
        ent['telemetry'] += len(t.get('telemetry', []) or [])
        ent['invariants'] += len(t.get('invariants', []) or [])
        diff = t.get('diff') or {}
        ent['diff_entries'] += len(diff.get('entries', [])) if isinstance(diff, dict) else 0
        ent['subsystems'] = max(ent['subsystems'], len(t.get('subsystems', []) or []))

    # deterministic ordered list
    out = [agg[k] for k in sorted(agg.keys())]
    return out


def aggregate_by_subsystem(runtime, tick_min: Optional[int] = None, tick_max: Optional[int] = None) -> List[Dict[str, Any]]:
    """Return aggregation per subsystem across specified ticks."""
    trace = Introspection.get_trace(runtime, limit=None)
    counts = {}
    for t in trace:
        tk = t.get('tick', 0)
        if not _in_tick_range(tk, tick_min, tick_max):
            continue
        for s in t.get('subsystems', []) or []:
            name = s.get('name')
            if name is None:
                continue
            ent = counts.setdefault(name, {'subsystem': name, 'ticks': 0, 'state_changes': 0})
            ent['ticks'] += 1
            # crude state change heuristic: if diff mentions the subsystem
            diff = t.get('diff') or {}
            entries = diff.get('entries', []) if isinstance(diff, dict) else []
            for e in entries:
                # if change field path contains subsystem name or before/after indicates change for that subsystem
                if e.get('field') and name in str(e.get('field')):
                    ent['state_changes'] += 1

    out = [counts[k] for k in sorted(counts.keys())]
    return out


def aggregate_event_types(runtime, tick_min: Optional[int] = None, tick_max: Optional[int] = None) -> List[Dict[str, Any]]:
    trace = Introspection.get_trace(runtime, limit=None)
    counts = {}
    for t in trace:
        tk = t.get('tick', 0)
        if not _in_tick_range(tk, tick_min, tick_max):
            continue
        for ev in t.get('events', []) or []:
            et = ev.get('type') or ev.get('event') or 'unknown'
            counts[et] = counts.get(et, 0) + 1
    out = [{'type': k, 'count': counts[k]} for k in sorted(counts.keys())]
    return out


def timeline_summary(runtime, window: int = 10) -> Dict[str, Any]:
    """Return a lightweight visualization-friendly summary: recent per-tick aggregates and top subsystems."""
    per_tick = aggregate_by_tick(runtime)
    recent = per_tick[-window:] if window is not None else per_tick
    by_sub = aggregate_by_subsystem(runtime)
    top_subs = sorted(by_sub, key=lambda s: s['ticks'], reverse=True)[:10]
    return {'per_tick': recent, 'top_subsystems': top_subs}


__all__ = ['filter_timeline', 'paginate_trace', 'aggregate_by_tick', 'aggregate_event_types', 'aggregate_by_subsystem', 'timeline_summary']
