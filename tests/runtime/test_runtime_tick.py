import pytest

from src.runtime.Runtime import Runtime
from src.runtime.RuntimeState import RuntimeState
from src.runtime.ErrorBoundary import FatalError


def test_deterministic_event_ordering():
    rt = Runtime()
    rt.init()
    rt.start()

    seen = []

    def handler(ev):
        seen.append(ev.get('payload'))

    rt.dispatcher.register_handler('E1', handler)

    rt.dispatch({'type': 'E1', 'payload': 'first'})
    rt.dispatch({'type': 'E1', 'payload': 'second'})

    processed = rt.tick()

    assert seen == ['first', 'second']
    assert len(processed) == 2


def test_fatal_handler_triggers_shutdown():
    rt = Runtime()
    rt.init()
    rt.start()

    def bad(ev):
        raise FatalError('boom')

    rt.dispatcher.register_handler('BAD', bad)
    rt.dispatch({'type': 'BAD'})

    # run a tick; the handler should cause a shutdown via ErrorBoundary
    rt.tick()

    assert rt.state == RuntimeState.SHUTDOWN
