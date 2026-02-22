from src.runtime.Runtime import Runtime
from src.runtime.TimelineQuery import aggregate_by_tick, aggregate_by_subsystem, paginate_trace, timeline_summary


def test_aggregation_and_paging():
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

    # register two subsystems
    r.subsystem_manager.register(S('X'), order=1)
    r.subsystem_manager.register(S('Y'), order=2)
    r.start()
    # run 5 ticks
    for _ in range(5):
        r.tick()

    per_tick = aggregate_by_tick(r)
    assert isinstance(per_tick, list)
    assert len(per_tick) >= 5

    per_sub = aggregate_by_subsystem(r)
    assert any(s['subsystem'] == 'X' for s in per_sub)

    page1 = paginate_trace(r, page=1, page_size=2)
    assert page1['page'] == 1
    assert len(page1['items']) <= 2

    summary = timeline_summary(r, window=3)
    assert 'per_tick' in summary and 'top_subsystems' in summary
