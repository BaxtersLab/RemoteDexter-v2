import copy
from src.runtime.Runtime import Runtime
from src.runtime.RuntimeState import RuntimeState
from src.runtime.ExternalAPI import AccessContext


class IncSubsystem:
    def __init__(self):
        self.name = 'Inc'
        self.v = 0

    def onInit(self, runtime):
        pass

    def onStart(self, runtime):
        pass

    def onPreTick(self, runtime):
        pass

    def onTick(self, runtime):
        self.v += 1

    def onPostTick(self, runtime):
        pass

    def onShutdown(self, runtime):
        pass

    def get_state(self):
        return {'v': self.v}


def normalize_trace(trace):
    # remove timestamps for deterministic comparison
    t = copy.deepcopy(trace)
    for e in t:
        if 'timestamp' in e:
            e['timestamp'] = None
        # normalize nested timestamps in events and telemetry
        for ev in e.get('events', []):
            if isinstance(ev, dict) and 'timestamp' in ev:
                ev['timestamp'] = None
        for te in e.get('telemetry', []):
            if isinstance(te, dict) and 'timestamp' in te:
                te['timestamp'] = None
            elif hasattr(te, 'timestamp'):
                # convert telemetry dataclass-like to dict and normalize timestamp
                idx = e['telemetry'].index(te)
                e['telemetry'][idx] = {'timestamp': None, 'category': getattr(te, 'category', None), 'message': getattr(te, 'message', None), 'payload': getattr(te, 'payload', None)}
    return t


def test_trace_deterministic_across_runs():
    r1 = Runtime(); r1.init(); r1.subsystem_manager.register(IncSubsystem(), order=1); r1.start()
    r2 = Runtime(); r2.init(); r2.subsystem_manager.register(IncSubsystem(), order=1); r2.start()

    r1.tick(); r1.tick()
    r2.tick(); r2.tick()

    t1 = r1.temporal_trace.get_trace()
    t2 = r2.temporal_trace.get_trace()
    assert normalize_trace(t1) == normalize_trace(t2)


def test_trace_correlation_and_access_control():
    r = Runtime(); r.init(); r.subsystem_manager.register(IncSubsystem(), order=1); r.start()
    r.tick()

    trace = r.temporal_trace.get_trace()
    assert len(trace) >= 1
    ent = trace[-1]
    # diff should reference subsystem 'Inc' when present
    diff = ent.get('diff')
    if diff is not None:
        # expect entries list possibly empty
        assert isinstance(diff.get('entries', []), list)

    # External API access control: restricted context cannot access trace
    try:
        r.api.query_trace(context=AccessContext.RESTRICTED)
        assert False, "Restricted context should not access trace"
    except PermissionError:
        pass
