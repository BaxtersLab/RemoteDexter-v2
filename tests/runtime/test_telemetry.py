from src.runtime.Telemetry import (
    TelemetryBuffer,
    TelemetryEvent,
    TelemetrySink,
    to_timeline_events,
)
import time


def test_append_and_snapshot():
    tb = TelemetryBuffer(max_size=5)
    e1 = TelemetryEvent(timestamp=time.time(), category='c1', message='m1', payload=None)
    e2 = TelemetryEvent(timestamp=time.time(), category='c2', message='m2', payload={'x': 1})
    tb.append(e1)
    tb.append(e2)
    snap = tb.snapshot()
    assert len(snap) == 2
    assert snap[0].message == 'm1' and snap[1].message == 'm2'


def test_flush_clears_buffer():
    tb = TelemetryBuffer(max_size=4)
    tb.append(TelemetryEvent(time.time(), 'c', 'a', None))
    tb.append(TelemetryEvent(time.time(), 'c', 'b', None))
    fl = tb.flush()
    assert len(fl) == 2
    assert len(tb) == 0


def test_ring_buffer_rollover():
    tb = TelemetryBuffer(max_size=3)
    for i in range(4):
        tb.append(TelemetryEvent(time.time(), 'c', f'm{i}', None))
    snap = tb.snapshot()
    assert len(snap) == 3
    # oldest (m0) should be dropped
    assert [e.message for e in snap] == ['m1', 'm2', 'm3']


def test_event_ordering_preserved():
    tb = TelemetryBuffer(max_size=10)
    for i in range(5):
        tb.append(TelemetryEvent(time.time(), 'c', f'm{i}', None))
    snap = tb.snapshot()
    assert [e.message for e in snap] == [f'm{i}' for i in range(5)]


def test_sink_emit_creates_events():
    sink = TelemetrySink()
    sink.emit('cat', 'hello', {'k': 'v'})
    sink.emit('cat', 'world')
    snap = sink.export_snapshot()
    assert len(snap) == 2
    assert all(isinstance(e, TelemetryEvent) for e in snap)


def test_export_helpers_produce_timeline_dicts():
    sink = TelemetrySink()
    sink.emit('cat', 'one', {'a': 1})
    sink.emit('cat', 'two')
    snap = sink.export_snapshot()
    tlist = to_timeline_events(snap)
    assert isinstance(tlist, list)
    assert all(d['type'] == 'telemetry' for d in tlist)
    assert tlist[1]['payload'] == {}
import pytest

from src.runtime.Runtime import Runtime
from src.runtime.Telemetry import TelemetryBuffer, TelemetryEvent


def test_telemetry_ordering_and_buffer_behavior():
    tb = TelemetryBuffer()
    tb.record('cat', 'one', {'v': 1})
    tb.record('cat', 'two', {'v': 2})
    events = tb.all()
    assert isinstance(events[0], TelemetryEvent)
    assert events[0].message == 'one'
    assert events[1].message == 'two'

    # bounded behavior
    tb2 = TelemetryBuffer(max_size=2)
    tb2.record('c', 'a')
    tb2.record('c', 'b')
    tb2.record('c', 'c')
    assert len(tb2.all()) == 2
    assert tb2.all()[0].message == 'b'


def test_runtime_emits_telemetry_for_ticks_and_invariants():
    rt = Runtime()
    rt.init()

    # add a simple subsystem that does not mutate state during telemetry
    class S:
        def __init__(self):
            self.name = 'S'
            self.counter = 0

        def onInit(self, runtime): pass
        def onStart(self, runtime): pass
        def onPreTick(self, runtime): pass
        def onTick(self, runtime): self.counter += 1
        def onPostTick(self, runtime): pass
        def onStop(self, runtime): pass
        def onShutdown(self, runtime): pass
        def get_state(self): return {'counter': self.counter}

    rt.subsystem_manager.register(S(), order=10)
    rt.start()
    rt.tick()
    # telemetry should have recorded tick start, events.flush, snapshot, invariant results
    evs = rt.telemetry.all()
    msgs = [e.message for e in evs]
    assert 'start' in msgs or any(e.category == 'tick' for e in evs)
    assert any(e.category == 'snapshot' for e in evs)

    # ensure subsystem state not mutated by telemetry record
    assert rt.subsystem_manager._subsystems[0][1].counter == 1
