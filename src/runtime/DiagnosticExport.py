from typing import List, Dict, Any

from src.runtime.Diagnostics import DiagnosticSnapshot
from src.runtime.Telemetry import TelemetryEvent
try:
    from src.runtime.Health import HealthStatus
except Exception:
    HealthStatus = None


def export_timeline(timeline_provider) -> List[Dict[str, Any]]:
    try:
        data = list(timeline_provider() or [])
        # Ensure all elements are dict-like
        return [dict(d) for d in data]
    except Exception:
        return []


def _telemetry_to_dict(ev: TelemetryEvent) -> Dict[str, Any]:
    try:
        return {
            "timestamp": float(ev.timestamp),
            "category": str(ev.category),
            "message": str(ev.message),
            "payload": dict(ev.payload or {}),
        }
    except Exception:
        return {"timestamp": 0.0, "category": "", "message": "", "payload": {}}


def export_telemetry(telemetry_source) -> List[Dict[str, Any]]:
    try:
        if telemetry_source is None:
            return []
        if hasattr(telemetry_source, "export_snapshot"):
            items = telemetry_source.export_snapshot()
        elif hasattr(telemetry_source, "snapshot"):
            items = telemetry_source.snapshot()
        elif hasattr(telemetry_source, "all"):
            items = telemetry_source.all()
        else:
            return []
        return [_telemetry_to_dict(e) for e in list(items or [])]
    except Exception:
        return []


def export_health(health_registry) -> Dict[str, Dict[str, Any]]:
    try:
        if health_registry is None:
            return {}
        raw = health_registry.run_all()
        out: Dict[str, Dict[str, Any]] = {}
        for name, hs in (raw or {}).items():
            try:
                if HealthStatus is not None and isinstance(hs, HealthStatus):
                    out[name] = {"status": hs.status, "details": dict(hs.details or {})}
                else:
                    out[name] = {"status": getattr(hs, "status", "error"), "details": dict(getattr(hs, "details", {}) or {})}
            except Exception:
                out[name] = {"status": "error", "details": {}}
        return out
    except Exception:
        return {}


def export_snapshot(snapshot: DiagnosticSnapshot) -> Dict[str, Any]:
    try:
        return {
            "ts": float(snapshot.timestamp),
            "timeline": list(snapshot.timeline or []),
            "telemetry": [ _telemetry_to_dict(e) for e in list(snapshot.telemetry or []) ],
            "health": dict(snapshot.health or {}),
        }
    except Exception:
        return {}


def export_history(history) -> List[Dict[str, Any]]:
    try:
        if history is None:
            return []
        snaps = history.all() if hasattr(history, "all") else []
        return [export_snapshot(s) for s in list(snaps or [])]
    except Exception:
        return []


__all__ = [
    "export_timeline",
    "export_telemetry",
    "export_health",
    "export_snapshot",
    "export_history",
]
