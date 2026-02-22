import pytest
import json

from src.runtime.Runtime import Runtime
from src.runtime.RuntimeState import capture, serialize, deserialize


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
        self.counter += 1

    def onPostTick(self, runtime):
        pass

    def onStop(self, runtime):
        pass

    def onShutdown(self, runtime):
        pass

    def get_state(self):
        return {'counter': self.counter}


def test_serialize_deserialize_determinism_and_stability():
    rt = Runtime()
    rt.init()
    sm = rt.subsystem_manager
    a = CountingSubsystem('A')
    sm.register(a, order=10)
    rt.start()
    rt.tick()
    snap = rt._last_snapshot_after

    s1 = serialize(snap)
    s2 = serialize(snap)
    # deterministic serialization: serialized dicts should be identical
    assert s1 == s2

    # round-trip
    ds = deserialize(s1)
    assert ds.subsystems.keys() == snap.subsystems.keys()


def test_state_history_and_temporal_navigation():
    rt = Runtime()
    rt.init()
    sm = rt.subsystem_manager
    a = CountingSubsystem('A')
    sm.register(a, order=10)
    rt.start()

    rt.tick()  # counter becomes 1
    rt.tick()  # counter becomes 2

    hist = rt.state_history
    latest = hist.latest()
    assert latest is not None
    assert hist.get(0) is not None
    assert hist.latest()['subsystems']['A']['counter'] == 2
    assert hist.previous()['subsystems']['A']['counter'] == 1


def test_rollback_scaffolding_rejects_malformed_snapshots():
    rt = Runtime()
    rt.init()
    sm = rt.subsystem_manager
    a = CountingSubsystem('A')
    sm.register(a, order=10)
    rt.start()
    rt.tick()

    # construct malformed snapshot with wrong keys
    bad = {'subsystems': {'X': {'counter': 1}}}
    assert not rt.rollback_to(bad)

    # construct compatible serialized snapshot
    good = serialize(rt._last_snapshot_after)
    assert rt.rollback_to(good) is True
