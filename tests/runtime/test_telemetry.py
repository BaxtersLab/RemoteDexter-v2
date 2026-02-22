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
