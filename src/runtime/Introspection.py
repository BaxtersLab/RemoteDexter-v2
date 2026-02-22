from dataclasses import asdict
from typing import Any, Dict, List, Optional
from .RuntimeState import serialize, RuntimeSnapshot


def get_runtime_status(runtime) -> Dict[str, Any]:
    """Return runtime mode, last tick snapshots present, and last invariant results."""
    status = {
        'state': getattr(runtime, 'state').name if hasattr(runtime, 'state') else None,
        'last_snapshot_before': None,
        'last_snapshot_after': None,
        'last_check_results': None,
    }
    if hasattr(runtime, '_last_snapshot_before') and runtime._last_snapshot_before is not None:
        status['last_snapshot_before'] = serialize(runtime._last_snapshot_before)
    if hasattr(runtime, '_last_snapshot_after') and runtime._last_snapshot_after is not None:
        status['last_snapshot_after'] = serialize(runtime._last_snapshot_after)
    if hasattr(runtime, '_last_check_results') and runtime._last_check_results is not None:
        # produce simple serializable summary
        status['last_check_results'] = [
            {'name': r.name, 'passed': r.passed, 'severity': r.severity.value} for r in runtime._last_check_results
        ]
    return status


def get_latest_snapshots(runtime) -> Dict[str, Optional[Dict[str, Any]]]:
    """Return before/after snapshots as serialized dicts (read-only)."""
    before = None
    after = None
    if hasattr(runtime, '_last_snapshot_before') and runtime._last_snapshot_before is not None:
        before = serialize(runtime._last_snapshot_before)
    if hasattr(runtime, '_last_snapshot_after') and runtime._last_snapshot_after is not None:
        after = serialize(runtime._last_snapshot_after)
    return {'before': before, 'after': after}


def get_subsystem_states(runtime) -> List[Dict[str, Any]]:
    """Return list of subsystem state snapshots in deterministic registration order."""
    out = []
    if not hasattr(runtime, 'subsystem_manager') or runtime.subsystem_manager is None:
        return out
    for _, s in runtime.subsystem_manager._subsystems:
        name = getattr(s, 'name', s.__class__.__name__)
        try:
            st = None
            if hasattr(s, 'get_state'):
                st = s.get_state()
            out.append({'name': name, 'state': st})
        except Exception:
            out.append({'name': name, 'state': None})
    return out


def get_last_diff(runtime) -> Dict[str, Any]:
    """Return the last computed diff as a pure-data dict, or None."""
    if hasattr(runtime, '_last_diff_result'):
        return runtime._last_diff_result
    return None


def get_trace(runtime, limit: Optional[int] = None) -> List[Dict[str, Any]]:
    if not hasattr(runtime, 'temporal_trace') or runtime.temporal_trace is None:
        return []
    try:
        return runtime.temporal_trace.get_trace(limit=limit)
    except Exception:
        return []


def get_lifecycle(runtime, subsystem: Optional[str] = None, phase: Optional[str] = None, limit: Optional[int] = None) -> List[Dict[str, Any]]:
    if not hasattr(runtime, 'lifecycle_map') or runtime.lifecycle_map is None:
        return []
    try:
        return runtime.lifecycle_map.get_events(subsystem=subsystem, phase=phase, limit=limit)
    except Exception:
        return []


def get_telemetry(runtime, limit: Optional[int] = None) -> List[Dict[str, Any]]:
    """Return recent telemetry events as serializable dicts in deterministic order (oldest->newest)."""
    if not hasattr(runtime, 'telemetry') or runtime.telemetry is None:
        return []
    evs = runtime.telemetry.all()
    if limit is not None and isinstance(limit, int):
        evs = evs[-limit:]
    # convert dataclass to dicts
    out = []
    for e in evs:
        try:
            d = asdict(e)
        except Exception:
            # fallback
            d = {'timestamp': getattr(e, 'timestamp', None), 'category': getattr(e, 'category', None), 'message': getattr(e, 'message', None), 'payload': getattr(e, 'payload', None)}
        out.append(d)
    return out


__all__ = [
    'get_runtime_status',
    'get_latest_snapshots',
    'get_subsystem_states',
    'get_telemetry',
    'get_last_diff',
    'get_trace',
    'get_lifecycle',
]
