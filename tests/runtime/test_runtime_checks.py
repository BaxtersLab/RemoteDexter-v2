import pytest

from src.runtime.Runtime import Runtime
from src.runtime.RuntimeChecks import Invariant, Severity, RuntimeChecker


def inv_always_ok(snapshot, history):
    return True


def inv_warn_if_zero(snapshot, history):
    # warn if any subsystem counter is zero
    subs = snapshot.subsystems
    for name, st in subs.items():
        if st is None:
            continue
        if st.get('counter', 0) == 0:
            return False
    return True


def inv_fatal_if_negative(snapshot, history):
    subs = snapshot.subsystems
    for name, st in subs.items():
        if st is None:
            continue
        if st.get('counter', 0) < 0:
            return False
    return True


def test_invariant_evaluation_and_classification():
    rt = Runtime()
    rt.init()
    # register invariants
    rt.runtime_checker.register(Invariant('always_ok', Severity.INFO, inv_always_ok))
    rt.runtime_checker.register(Invariant('warn_if_zero', Severity.WARN, inv_warn_if_zero))
    rt.runtime_checker.register(Invariant('fatal_if_negative', Severity.FATAL, inv_fatal_if_negative))

    # attach a counting subsystem
    class C:
        def __init__(self):
            self.name = 'C'
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

    sm = rt.subsystem_manager
    sm.register(C(), order=10)
    rt.start()
    # first tick: counter becomes 1, warn_if_zero should pass
    rt.tick()
    results = getattr(rt, '_last_check_results', [])
    assert any(r.name == 'always_ok' and r.passed for r in results)
    assert any(r.name == 'warn_if_zero' and r.passed for r in results)


def test_fatal_invariant_triggers_shutdown():
    rt = Runtime()
    rt.init()
    # fatal invariant that fails immediately
    def fail_now(snapshot, history):
        return False

    rt.runtime_checker.register(Invariant('fail_now', Severity.FATAL, fail_now))

    # subsystem required for snapshot keys
    class Dummy:
        def __init__(self):
            self.name = 'D'
        def onInit(self, runtime): pass
        def onStart(self, runtime): pass
        def onPreTick(self, runtime): pass
        def onTick(self, runtime): pass
        def onPostTick(self, runtime): pass
        def onStop(self, runtime): pass
        def onShutdown(self, runtime): pass
        def get_state(self):
            return {}

    rt.subsystem_manager.register(Dummy(), order=10)
    rt.start()
    rt.tick()
    assert rt.state.name == 'SHUTDOWN'
