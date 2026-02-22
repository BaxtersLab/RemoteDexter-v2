import pytest
from src.runtime.Runtime import Runtime
from src.runtime.ExternalAPI import AccessContext


class DummySubsystem:
    def __init__(self, name):
        self.name = name
        self.v = 0

    def onInit(self, runtime):
        self.v = 0

    def onStart(self, runtime):
        self.v = 0

    def onPreTick(self, runtime):
        self.v += 0

    def onTick(self, runtime):
        self.v += 1

    def onPostTick(self, runtime):
        self.v += 0

    def onShutdown(self, runtime):
        self.v = -1

    def get_state(self):
        return {'v': self.v}


def test_lifecycle_and_timeline_correlation_and_purity():
    r = Runtime(); r.init();
    # register three subsystems in deterministic order
    r.subsystem_manager.register(DummySubsystem('A'), order=10)
    r.subsystem_manager.register(DummySubsystem('B'), order=20)
    r.subsystem_manager.register(DummySubsystem('C'), order=30)
    r.start()
    # run two ticks
    r.tick(); r.tick()
    # shutdown
    r.shutdown('test')

    # lifecycle events should exist for init/start/tick/post_tick/shutdown
    lifecycle = r.introspect.get_lifecycle()
    assert isinstance(lifecycle, list)
    phases = set([e['phase'] for e in lifecycle])
    assert 'init' in phases
    assert 'start' in phases
    assert 'tick' in phases
    assert 'post_tick' in phases
    assert 'shutdown' in phases

    # trace entries should reference lifecycle events for the tick
    trace = r.introspect.get_trace()
    assert isinstance(trace, list)
    # each trace entry should include lifecycle_refs list
    for t in trace:
        assert 'lifecycle_refs' in t
        assert isinstance(t['lifecycle_refs'], list)

    # API access control: restricted context cannot query lifecycle
    with pytest.raises(PermissionError):
        r.api.query_lifecycle(context=AccessContext.RESTRICTED)
