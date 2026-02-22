import pytest

from src.runtime.Runtime import Runtime
from src.runtime.RuntimeState import RuntimeState
from src.runtime.ErrorBoundary import FatalError


class HookRecorder:
    def __init__(self):
        self.events = []

    def onInit(self, runtime):
        self.events.append('onInit')

    def onStart(self, runtime):
        self.events.append('onStart')

    def onPreTick(self, runtime):
        self.events.append('onPreTick')

    def onPostTick(self, runtime):
        self.events.append('onPostTick')

    def onStop(self, runtime):
        self.events.append('onStop')

    def onShutdown(self, runtime):
        self.events.append('onShutdown')


def test_unified_loop_advances_deterministically():
    rt = Runtime(hooks=HookRecorder())
    rt.init()
    rt.start()

    seen = []

    def handler(ev):
        seen.append(ev.get('payload'))

    rt.dispatcher.register_handler('PING', handler)

    # dispatch across two ticks: one event now, one after first tick
    rt.dispatch({'type': 'PING', 'payload': 'A'})

    # run the unified loop; should process the single event then exit idle
    processed = rt.runLoop()

    assert len(processed) == 1
    assert seen == ['A']
    # lifecycle hooks sequence should include init, start, pre/post tick, stop
    assert rt.hooks.events[0] == 'onInit'
    assert 'onStart' in rt.hooks.events
    assert 'onPreTick' in rt.hooks.events
    assert 'onPostTick' in rt.hooks.events
    assert 'onStop' in rt.hooks.events


def test_fatal_error_in_phase_triggers_controlled_shutdown():
    hooks = HookRecorder()
    rt = Runtime(hooks=hooks)
    rt.init()
    rt.start()

    # register handler that raises fatal error
    def bad(ev):
        raise FatalError('fatal during handler')

    rt.dispatcher.register_handler('CRASH', bad)
    rt.dispatch({'type': 'CRASH'})

    rt.runLoop()

    assert rt.state == RuntimeState.SHUTDOWN
    # onShutdown should have been invoked via ErrorBoundary
    assert 'onShutdown' in hooks.events
