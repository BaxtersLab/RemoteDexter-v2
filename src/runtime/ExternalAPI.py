from dataclasses import asdict
from enum import Enum
from typing import Any, Dict, List, Optional
from . import Introspection


class AccessContext(Enum):
    INTERNAL = 'internal'
    EXTERNAL = 'external'
    RESTRICTED = 'restricted'


def _validate_context(ctx: Optional[Any]) -> AccessContext:
    if isinstance(ctx, AccessContext):
        return ctx
    if isinstance(ctx, str):
        try:
            return AccessContext(ctx)
        except Exception:
            pass
    # default to external
    return AccessContext.EXTERNAL


def query_status(runtime, context: Optional[Any] = None) -> Dict[str, Any]:
    ctx = _validate_context(context)
    if ctx == AccessContext.RESTRICTED:
        raise PermissionError('Restricted context cannot access status')
    # use introspection; returns serializable structures
    return Introspection.get_runtime_status(runtime)


def query_snapshots(runtime, limit: int = 1, context: Optional[Any] = None) -> Dict[str, Optional[Dict[str, Any]]]:
    ctx = _validate_context(context)
    if ctx == AccessContext.RESTRICTED:
        raise PermissionError('Restricted context cannot access snapshots')
    snaps = Introspection.get_latest_snapshots(runtime)
    # only return up to `limit` snapshots per side if available; here limit applies to history retrieval
    return snaps


def query_subsystems(runtime, context: Optional[Any] = None) -> List[Dict[str, Any]]:
    ctx = _validate_context(context)
    if ctx == AccessContext.RESTRICTED:
        raise PermissionError('Restricted context cannot access subsystems')
    return Introspection.get_subsystem_states(runtime)


def query_telemetry(runtime, limit: Optional[int] = None, context: Optional[Any] = None) -> List[Dict[str, Any]]:
    ctx = _validate_context(context)
    if ctx == AccessContext.RESTRICTED:
        raise PermissionError('Restricted context cannot access telemetry')
    return Introspection.get_telemetry(runtime, limit=limit)


def query_diff(runtime, context: Optional[Any] = None) -> Dict[str, Any]:
    ctx = _validate_context(context)
    if ctx == AccessContext.RESTRICTED:
        raise PermissionError('Restricted context cannot access diffs')
    # use introspection to obtain pure-data diff
    return Introspection.get_last_diff(runtime)


def query_trace(runtime, limit: Optional[int] = None, context: Optional[Any] = None) -> List[Dict[str, Any]]:
    ctx = _validate_context(context)
    if ctx == AccessContext.RESTRICTED:
        raise PermissionError('Restricted context cannot access trace')
    return Introspection.get_trace(runtime, limit=limit)


def query_lifecycle(runtime, subsystem: Optional[str] = None, phase: Optional[str] = None, limit: Optional[int] = None, context: Optional[Any] = None) -> List[Dict[str, Any]]:
    ctx = _validate_context(context)
    if ctx == AccessContext.RESTRICTED:
        raise PermissionError('Restricted context cannot access lifecycle events')
    return Introspection.get_lifecycle(runtime, subsystem=subsystem, phase=phase, limit=limit)


def query_timeline(runtime, limit: Optional[int] = None, context: Optional[Any] = None) -> Dict[str, Any]:
    ctx = _validate_context(context)
    if ctx == AccessContext.RESTRICTED:
        raise PermissionError('Restricted context cannot access timeline')
    # merged view: trace entries, lifecycle events, diffs, telemetry, invariants
    trace = Introspection.get_trace(runtime, limit=limit)
    lifecycle = Introspection.get_lifecycle(runtime, limit=None)
    diffs = Introspection.get_last_diff(runtime)
    telemetry = Introspection.get_telemetry(runtime, limit=limit)
    return {'trace': trace, 'lifecycle': lifecycle, 'last_diff': diffs, 'telemetry': telemetry}


def query_timeline_view(runtime, subsystem: Optional[str] = None, phase: Optional[str] = None, tick_min: Optional[int] = None, tick_max: Optional[int] = None, limit: Optional[int] = None, context: Optional[Any] = None) -> Dict[str, Any]:
    ctx = _validate_context(context)
    if ctx == AccessContext.RESTRICTED:
        raise PermissionError('Restricted context cannot access timeline view')
    # defer to TimelineQuery module
    try:
        from .TimelineQuery import filter_timeline
        return filter_timeline(runtime, subsystem=subsystem, phase=phase, tick_min=tick_min, tick_max=tick_max, limit=limit)
    except Exception:
        # fallback to merged timeline
        return query_timeline(runtime, limit=limit, context=context)


def export_trace(runtime, fmt: str = 'json', context: Optional[Any] = None):
    ctx = _validate_context(context)
    if ctx == AccessContext.RESTRICTED:
        raise PermissionError('Restricted context cannot export trace')
    fmt = (fmt or 'json').lower()
    if not hasattr(runtime, 'temporal_trace') or runtime.temporal_trace is None:
        return None
    if fmt == 'json':
        return runtime.temporal_trace.export_json()
    if fmt == 'csv':
        return runtime.temporal_trace.export_csv()
    raise ValueError('Unsupported format')


def subscribe_to_events(runtime, callback, context: Optional[Any] = None) -> bool:
    ctx = _validate_context(context)
    if ctx == AccessContext.RESTRICTED:
        raise PermissionError('Restricted context cannot subscribe to events')
    if not hasattr(runtime, 'event_stream') or runtime.event_stream is None:
        return False
    return runtime.event_stream.subscribe(callback)


def unsubscribe_from_events(runtime, callback) -> bool:
    if not hasattr(runtime, 'event_stream') or runtime.event_stream is None:
        return False
    return runtime.event_stream.unsubscribe(callback)


class ExternalAPIFacade:
    def __init__(self, runtime):
        self._runtime = runtime

    def query_status(self, context: Optional[Any] = None):
        return query_status(self._runtime, context=context)

    def query_snapshots(self, limit: int = 1, context: Optional[Any] = None):
        return query_snapshots(self._runtime, limit=limit, context=context)

    def query_subsystems(self, context: Optional[Any] = None):
        return query_subsystems(self._runtime, context=context)

    def query_telemetry(self, limit: Optional[int] = None, context: Optional[Any] = None):
        return query_telemetry(self._runtime, limit=limit, context=context)

    def query_diff(self, context: Optional[Any] = None):
        return query_diff(self._runtime, context=context)

    def query_trace(self, limit: Optional[int] = None, context: Optional[Any] = None):
        return query_trace(self._runtime, limit=limit, context=context)

    def query_lifecycle(self, subsystem: Optional[str] = None, phase: Optional[str] = None, limit: Optional[int] = None, context: Optional[Any] = None):
        return query_lifecycle(self._runtime, subsystem=subsystem, phase=phase, limit=limit, context=context)

    def query_timeline(self, limit: Optional[int] = None, context: Optional[Any] = None):
        return query_timeline(self._runtime, limit=limit, context=context)

    def query_timeline_view(self, subsystem: Optional[str] = None, phase: Optional[str] = None, tick_min: Optional[int] = None, tick_max: Optional[int] = None, limit: Optional[int] = None, context: Optional[Any] = None):
        return query_timeline_view(self._runtime, subsystem=subsystem, phase=phase, tick_min=tick_min, tick_max=tick_max, limit=limit, context=context)

    def export_trace(self, fmt: str = 'json', context: Optional[Any] = None):
        return export_trace(self._runtime, fmt=fmt, context=context)

    def subscribe_to_events(self, callback, context: Optional[Any] = None):
        return subscribe_to_events(self._runtime, callback, context=context)

    def unsubscribe_from_events(self, callback):
        return unsubscribe_from_events(self._runtime, callback)

    def subscribe_to_events(self, callback, context: Optional[Any] = None):
        return subscribe_to_events(self._runtime, callback, context=context)

    def unsubscribe_from_events(self, callback):
        return unsubscribe_from_events(self._runtime, callback)


__all__ = ["AccessContext", "ExternalAPIFacade", "query_status", "query_snapshots", "query_subsystems", "query_telemetry"]
