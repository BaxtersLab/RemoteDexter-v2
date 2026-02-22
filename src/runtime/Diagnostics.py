from dataclasses import dataclass
from typing import List, Dict, Callable, Any
import time

from src.runtime.Telemetry import TelemetryEvent, TelemetrySink, TelemetryBuffer, to_timeline_events


@dataclass(frozen=True)
class DiagnosticSnapshot:
    timestamp: float
    timeline: List[Dict[str, Any]]
    telemetry: List[TelemetryEvent]
    health: Dict[str, str]


class DiagnosticCollector:
    """Collects diagnostics from timeline, telemetry, and health providers."""

    def __init__(
        self,
        timeline_provider: Callable[[], List[Dict[str, Any]]],
        telemetry_source: Any = None,
        health_provider: Callable[[], Dict[str, str]] = None,
    ):
        self.timeline_provider = timeline_provider
        # telemetry_source may be a TelemetrySink or TelemetryBuffer or similar
        self.telemetry_source = telemetry_source if telemetry_source is not None else TelemetryBuffer()
        self.health_provider = health_provider if health_provider is not None else self._default_health

    def _default_health(self) -> Dict[str, str]:
        return {"runtime": "ok", "timeline": "ok", "telemetry": "ok"}

    def collect_timeline_only(self) -> List[Dict[str, Any]]:
        return list(self.timeline_provider() or [])

    def collect_telemetry_only(self) -> List[TelemetryEvent]:
        src = self.telemetry_source
        if hasattr(src, "export_snapshot"):
            return src.export_snapshot()
        if hasattr(src, "snapshot"):
            return src.snapshot()
        if hasattr(src, "all"):
            return src.all()
        # best-effort empty
        return []

    def collect_health_only(self) -> Dict[str, str]:
        return dict(self.health_provider() or {})

    def collect_snapshot(self) -> DiagnosticSnapshot:
        ts = time.time()
        timeline = self.collect_timeline_only()
        telemetry = self.collect_telemetry_only()
        health = self.collect_health_only()
        return DiagnosticSnapshot(timestamp=ts, timeline=timeline, telemetry=telemetry, health=health)


def snapshot_to_dict(snapshot: DiagnosticSnapshot) -> Dict[str, Any]:
    # telemetry events -> serializable dicts
    telem = []
    for e in snapshot.telemetry:
        telem.append({
            "timestamp": e.timestamp,
            "category": e.category,
            "message": e.message,
            "payload": e.payload or {},
        })
    return {
        "ts": snapshot.timestamp,
        "timeline": list(snapshot.timeline),
        "telemetry": telem,
        "health": dict(snapshot.health),
    }


__all__ = ["DiagnosticSnapshot", "DiagnosticCollector", "snapshot_to_dict"]
