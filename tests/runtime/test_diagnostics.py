import time
from src.runtime.Diagnostics import DiagnosticCollector, snapshot_to_dict, DiagnosticSnapshot
from src.runtime.Telemetry import TelemetrySink, TelemetryBuffer, TelemetryEvent, to_timeline_events


def simple_timeline_provider():
    # return a small deterministic timeline list
    return [
        {"ts": 1.0, "type": "tick", "detail": "start"},
        {"ts": 2.0, "type": "tick", "detail": "end"},
    ]


def test_collect_timeline_only():
    dc = DiagnosticCollector(timeline_provider=simple_timeline_provider, telemetry_source=TelemetryBuffer())
    t = dc.collect_timeline_only()
    assert isinstance(t, list)
    assert len(t) == 2


def test_collect_telemetry_only():
    tb = TelemetryBuffer(max_size=10)
    tb.record('cat', 'm1', {'a': 1})
    dc = DiagnosticCollector(timeline_provider=simple_timeline_provider, telemetry_source=tb)
    telem = dc.collect_telemetry_only()
    assert isinstance(telem, list)
    assert len(telem) == 1
    assert isinstance(telem[0], TelemetryEvent)


def test_collect_health_only():
    dc = DiagnosticCollector(timeline_provider=simple_timeline_provider, telemetry_source=TelemetryBuffer())
    h = dc.collect_health_only()
    assert h.get('runtime') == 'ok'


def test_collect_snapshot_combines_all_sources():
    tb = TelemetryBuffer(max_size=10)
    tb.record('cat', 'm1', {'a': 1})
    dc = DiagnosticCollector(timeline_provider=simple_timeline_provider, telemetry_source=tb)
    snap: DiagnosticSnapshot = dc.collect_snapshot()
    assert isinstance(snap, DiagnosticSnapshot)
    assert len(snap.timeline) == 2
    assert len(snap.telemetry) == 1
    assert snap.health['timeline'] == 'ok'


def test_snapshot_to_dict_structure():
    tb = TelemetryBuffer(max_size=10)
    tb.record('cat', 'm1', {'a': 1})
    dc = DiagnosticCollector(timeline_provider=simple_timeline_provider, telemetry_source=tb)
    snap = dc.collect_snapshot()
    d = snapshot_to_dict(snap)
    assert set(d.keys()) == {'ts', 'timeline', 'telemetry', 'health'}
    assert isinstance(d['telemetry'], list)
    assert isinstance(d['timeline'], list)


def test_snapshot_timestamp_monotonicity():
    dc = DiagnosticCollector(timeline_provider=simple_timeline_provider, telemetry_source=TelemetryBuffer())
    s1 = dc.collect_snapshot()
    time.sleep(0.001)
    s2 = dc.collect_snapshot()
    assert s2.timestamp >= s1.timestamp
