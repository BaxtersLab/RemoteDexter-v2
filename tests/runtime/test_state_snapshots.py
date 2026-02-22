import pytest

from src.runtime.Runtime import Runtime
from src.runtime.RuntimeState import capture, diff


class CountingSubsystem:
    def __init__(self, name):
        self.name = name
        self.counter = 0

    def onInit(self, runtime):
        pass

    def onStart(self, runtime):
        pass

    def onPreTick(self, runtime):
        pass

    def onTick(self, runtime):
        # increment counter each tick
        self.counter += 1

    def onPostTick(self, runtime):
        pass

    def onStop(self, runtime):
        pass

    def onShutdown(self, runtime):
        pass

    def get_state(self):
        # return pure-data snapshot
        return {'counter': self.counter}


def test_snapshot_determinism_across_ticks():
    rt = Runtime()
    rt.init()
    sm = rt.subsystem_manager
    a = CountingSubsystem('A')
    b = CountingSubsystem('B')
    sm.register(a, order=10)
    sm.register(b, order=20)

    rt.start()

    # first tick
    rt.dispatch({'type': 'NOOP'})  # no-op event
    rt.tick()
    snap1 = rt._last_snapshot_after

    # second tick
    rt.tick()
    snap2 = rt._last_snapshot_after

    # snapshots should reflect counters
    assert snap1.subsystems['A']['counter'] == 1
    assert snap1.subsystems['B']['counter'] == 1
    assert snap2.subsystems['A']['counter'] == 2
    assert snap2.subsystems['B']['counter'] == 2

    # diffs should detect changes
    d = diff(snap1, snap2)
    assert 'A' in d and d['A']['before']['counter'] == 1 and d['A']['after']['counter'] == 2
    assert 'B' in d and d['B']['before']['counter'] == 1 and d['B']['after']['counter'] == 2


def test_snapshot_no_side_effects_on_capture():
    rt = Runtime()
    rt.init()
    sm = rt.subsystem_manager
    a = CountingSubsystem('A')
    sm.register(a, order=10)

    rt.start()
    # capture snapshot without running tick
    s0 = capture(rt)
    assert s0.subsystems['A']['counter'] == 0
    # ensure capture did not change subsystem state
    assert a.counter == 0
