import pytest

from src.runtime.Runtime import Runtime


def test_introspection_readonly_and_determinism():
    rt = Runtime()
    rt.init()

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

    # introspection queries
    status = rt.introspect.get_runtime_status()
    snaps = rt.introspect.get_latest_snapshots()
    subs = rt.introspect.get_subsystem_states()
    telemetry = rt.introspect.get_telemetry()

    # deterministic ordering and type checks
    assert 'state' in status
    assert 'before' in snaps and 'after' in snaps
    assert isinstance(subs, list) and subs[0]['name'] == 'S'
    assert isinstance(telemetry, list)

    # introspection calls must be side-effect free
    assert rt.subsystem_manager._subsystems[0][1].counter == 1
