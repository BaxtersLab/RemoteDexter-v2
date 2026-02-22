from src.runtime.Runtime import Runtime


def test_timeline_query_filters_basic():
    r = Runtime(); r.init();
    class S:
        def __init__(self, name):
            self.name = name; self.v = 0
        def onInit(self, runtime): pass
        def onStart(self, runtime): pass
        def onPreTick(self, runtime): pass
        def onTick(self, runtime): self.v += 1
        def onPostTick(self, runtime): pass
        def onShutdown(self, runtime): pass
        def get_state(self): return {'v': self.v}

    r.subsystem_manager.register(S('A'), order=1)
    r.start()
    r.tick(); r.tick();
    # basic filter: tick range
    res = r.api.query_timeline_view(tick_min=1, tick_max=1)
    assert 'trace' in res and 'lifecycle' in res
    assert all(1 <= t.get('tick', 0) <= 1 for t in res['trace']) or len(res['trace']) == 0
