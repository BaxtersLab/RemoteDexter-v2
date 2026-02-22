from src.runtime.DiagnosticExport import (
    export_timeline,
    export_telemetry,
    export_health,
    export_snapshot,
    export_history,
)
from src.runtime.Telemetry import TelemetryBuffer
from src.runtime.Diagnostics import DiagnosticSnapshot
from src.runtime.DiagnosticHistory import DiagnosticHistory


def test_export_timeline_basic():
    provider = lambda: [{"ts": 1.0, "type": "tick"}]
    out = export_timeline(provider)
    assert isinstance(out, list)
    assert out[0]["ts"] == 1.0


def test_export_telemetry_conversion():
    tb = TelemetryBuffer(max_size=10)
    tb.record("c", "m", {"a": 1})
    out = export_telemetry(tb)
    assert isinstance(out, list)
    assert out[0]["category"] == "c"


def test_export_health_conversion():
    class FakeStatus:
        def __init__(self, status, details=None):
            self.status = status
            self.details = details or {}

    class FakeRegistry:
        def run_all(self):
            return {
                "runtime": FakeStatus("ok", {"uptime": 1}),
                "timeline": FakeStatus("warn", {"events": 2}),
            }

    registry = FakeRegistry()
    out = export_health(registry)
    assert isinstance(out, dict)
    assert "runtime" in out and "timeline" in out


def test_export_snapshot_structure():
    snap = DiagnosticSnapshot(timestamp=1.0, timeline=[{"ts":1}], telemetry=[], health={"runtime": {"status":"ok"}})
    out = export_snapshot(snap)
    assert set(out.keys()) == {"ts", "timeline", "telemetry", "health"}


def test_export_history_multiple_snapshots():
    h = DiagnosticHistory(max_size=10)
    s1 = DiagnosticSnapshot(timestamp=1.0, timeline=[{"ts":1}], telemetry=[], health={})
    s2 = DiagnosticSnapshot(timestamp=2.0, timeline=[{"ts":2}], telemetry=[], health={})
    h.add(s1)
    h.add(s2)
    out = export_history(h)
    assert isinstance(out, list)
    assert out[0]["ts"] == 1.0 and out[1]["ts"] == 2.0


def test_export_functions_never_raise():
    # pass None where possible
    assert export_timeline(lambda: None) == []
    assert export_telemetry(None) == []
    assert export_health(None) == {}
    assert export_snapshot(None) == {}
    assert export_history(None) == []
