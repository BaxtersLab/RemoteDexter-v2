from src.runtime.SnapshotDiff import diff_snapshots, as_pure_data, DiffResult
from src.runtime.SnapshotDiff import classify_change
from src.runtime.Runtime import Runtime
from src.runtime.RuntimeState import RuntimeState


def test_diff_determinism_same_input():
    before = {'subsystems': {'A': {'x': 1, 'y': 2}}}
    after = {'subsystems': {'A': {'x': 1, 'y': 3}}}
    d1 = diff_snapshots(before, after)
    d2 = diff_snapshots(before, after)
    assert as_pure_data(d1) == as_pure_data(d2)


def test_classify_added_removed_modified():
    before = {'subsystems': {'S': {'a': 1, 'b': 2}}}
    after = {'subsystems': {'S': {'b': 2, 'c': 3}}}
    d = diff_snapshots(before, after)
    pd = as_pure_data(d)
    types = { (e['subsystem'], e['field']): e['change_type'] for e in pd['entries'] }
    assert types[('S', 'a')] == 'removed'
    assert types[('S', 'b')] == 'unchanged'
    assert types[('S', 'c')] == 'added'


def test_runtime_integration_emits_diff_event_and_stores_last_diff():
    # Create runtime with a tiny subsystem that changes state on tick
    class TSub:
        def __init__(self):
            self.name = 'T'
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

    r = Runtime()
    r.init()
    r.subsystem_manager.register(TSub(), order=5)
    r.start()

    # subscribe to event stream to capture diff events
    diffs = []
    def cb(ev):
        diffs.append(ev)

    if hasattr(r, 'event_stream') and r.event_stream is not None:
        r.event_stream.subscribe(cb)

    # run one tick to generate diff
    r.tick()
    # last diff should be present and event published
    assert hasattr(r, '_last_diff_result') and r._last_diff_result is not None
    # event stream should have captured a diff payload
    if hasattr(r, 'event_stream') and r.event_stream is not None:
        assert any(isinstance(d, dict) and d.get('type') == 'diff' and isinstance(d.get('payload'), dict) and 'entries' in d.get('payload') for d in diffs)
