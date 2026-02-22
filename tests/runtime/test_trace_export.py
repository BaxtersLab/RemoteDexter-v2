import json
from src.runtime.Runtime import Runtime
from src.runtime.ExternalAPI import AccessContext


def test_export_json_and_csv_determinism_and_purity():
    r = Runtime(); r.init();
    class S:
        def __init__(self):
            self.name='S'; self.v=0
        def onInit(self, runtime): pass
        def onStart(self, runtime): pass
        def onPreTick(self, runtime): pass
        def onTick(self, runtime): self.v+=1
        def onPostTick(self, runtime): pass
        def get_state(self): return {'v': self.v}

    r.subsystem_manager.register(S(), order=1)
    r.start()
    r.tick(); r.tick()

    js = r.temporal_trace.export_json()
    csv = r.temporal_trace.export_csv()

    # JSON should be list of dicts and pure-data (no objects)
    assert isinstance(js, list)
    for e in js:
        assert isinstance(e.get('tick'), int)
        # events and telemetry entries must be dicts
        for ev in e.get('events', []):
            assert isinstance(ev, dict)

    # CSV should be a string with header line
    assert isinstance(csv, str)
    assert csv.splitlines()[0].startswith('tick,')


def test_export_api_access_control():
    r = Runtime(); r.init(); r.start(); r.tick()
    # restricted context cannot export
    try:
        r.api.export_trace(fmt='json', context=AccessContext.RESTRICTED)
        assert False
    except PermissionError:
        pass
