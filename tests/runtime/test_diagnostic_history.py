import time
from src.runtime.DiagnosticHistory import DiagnosticHistory
from src.runtime.Diagnostics import DiagnosticSnapshot


def make_snapshot(ts: float) -> DiagnosticSnapshot:
    return DiagnosticSnapshot(timestamp=ts, timeline=[{"ts": ts}], telemetry=[], health={})


def test_add_and_latest():
    h = DiagnosticHistory(max_size=5)
    s1 = make_snapshot(1.0)
    s2 = make_snapshot(2.0)
    h.add(s1)
    h.add(s2)
    latest = h.latest()
    assert latest is not None
    assert latest.timestamp == 2.0


def test_ring_buffer_overwrite():
    h = DiagnosticHistory(max_size=3)
    for i in range(4):
        h.add(make_snapshot(float(i)))
    all_snap = h.all()
    assert len(all_snap) == 3
    # oldest (0.0) should be dropped
    assert [s.timestamp for s in all_snap] == [1.0, 2.0, 3.0]


def test_all_returns_copy():
    h = DiagnosticHistory(max_size=3)
    h.add(make_snapshot(1.0))
    arr = h.all()
    arr.append(make_snapshot(9.0))
    # original history must not be affected
    assert len(h.all()) == 1


def test_window_filters_by_timestamp():
    h = DiagnosticHistory(max_size=5)
    h.add(make_snapshot(1.0))
    h.add(make_snapshot(2.0))
    h.add(make_snapshot(3.0))
    res = h.window(2.0)
    assert [s.timestamp for s in res] == [2.0, 3.0]


def test_clear_empties_history():
    h = DiagnosticHistory(max_size=3)
    h.add(make_snapshot(1.0))
    assert h.latest() is not None
    h.clear()
    assert h.latest() is None
    assert h.all() == []
