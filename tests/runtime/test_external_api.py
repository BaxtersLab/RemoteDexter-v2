import pytest

from src.runtime.Runtime import Runtime
from src.runtime.ExternalAPI import ExternalAPIFacade, AccessContext


def test_external_api_readonly_and_json_safe():
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

    api = rt.api
    status = api.query_status(context=AccessContext.EXTERNAL)
    snaps = api.query_snapshots(limit=1, context=AccessContext.EXTERNAL)
    subs = api.query_subsystems(context=AccessContext.EXTERNAL)
    telem = api.query_telemetry(context=AccessContext.EXTERNAL)

    assert isinstance(status, dict)
    assert isinstance(snaps, dict)
    assert isinstance(subs, list)
    assert isinstance(telem, list)

    # ensure no live object references are returned
    for s in subs:
        assert not hasattr(s['state'], '__call__')

    # ensure API does not mutate state
    assert rt.subsystem_manager._subsystems[0][1].counter == 1


def test_external_api_access_control_rejects_restricted():
    rt = Runtime()
    rt.init()
    api = rt.api
    with pytest.raises(PermissionError):
        api.query_status(context=AccessContext.RESTRICTED)
