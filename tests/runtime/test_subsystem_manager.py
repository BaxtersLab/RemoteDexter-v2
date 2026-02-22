import pytest

from src.runtime.SubsystemManager import SubsystemManager
from src.runtime.Runtime import Runtime
from src.runtime.RuntimeState import RuntimeState
from src.runtime.ErrorBoundary import FatalError


class SimpleSubsystem:
    def __init__(self, name, recorder):
        self.name = name
        self.recorder = recorder

    def onInit(self, runtime):
        self.recorder.append(f"{self.name}.onInit")

    def onStart(self, runtime):
        self.recorder.append(f"{self.name}.onStart")

    def onPreTick(self, runtime):
        self.recorder.append(f"{self.name}.onPreTick")

    def onTick(self, runtime):
        self.recorder.append(f"{self.name}.onTick")

    def onPostTick(self, runtime):
        self.recorder.append(f"{self.name}.onPostTick")

    def onStop(self, runtime):
        self.recorder.append(f"{self.name}.onStop")

    def onShutdown(self, runtime):
        self.recorder.append(f"{self.name}.onShutdown")


class CrashingSubsystem(SimpleSubsystem):
    def onTick(self, runtime):
        raise FatalError('subsystem crash')


def test_subsystem_ordering_and_lifecycle():
    rt = Runtime()
    rt.init()
    # replace subsystem_manager with one we control
    sm = rt.subsystem_manager
    rec = []
    a = SimpleSubsystem('A', rec)
    b = SimpleSubsystem('B', rec)
    sm.register(a, order=10)
    sm.register(b, order=20)

    # run lifecycle phases
    sm.init_all(rt)
    sm.start_all(rt)
    sm.pre_tick_all(rt)
    sm.tick_all(rt)
    sm.post_tick_all(rt)
    sm.stop_all(rt)
    sm.shutdown_all(rt)

    # ordering should be A then B for each phase
    expected = [
        'A.onInit', 'B.onInit',
        'A.onStart', 'B.onStart',
        'A.onPreTick', 'B.onPreTick',
        'A.onTick', 'B.onTick',
        'A.onPostTick', 'B.onPostTick',
        'A.onStop', 'B.onStop',
        'A.onShutdown', 'B.onShutdown',
    ]

    assert rec == expected


def test_fatal_error_propagates_to_runtime_shutdown():
    rt = Runtime()
    rt.init()
    sm = rt.subsystem_manager
    rec = []
    good = SimpleSubsystem('G', rec)
    bad = CrashingSubsystem('X', rec)
    sm.register(good, order=10)
    sm.register(bad, order=20)

    # start runtime and run runLoop which will execute tick phases
    rt.start()
    # dispatch none; ensure that subsystem tick executes and crashes
    processed = rt.runLoop()

    assert rt.state == RuntimeState.SHUTDOWN
    # ensure shutdown invoked; ErrorBoundary triggers runtime.shutdown
    assert rt.state == RuntimeState.SHUTDOWN
