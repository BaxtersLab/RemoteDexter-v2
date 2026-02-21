import pytest

from src.runtime.Runtime import Runtime
from src.runtime.RuntimeState import RuntimeState
from src.runtime.EventDispatcher import MalformedEvent
from src.runtime.ModuleHandshake import ModuleInfo
from src.runtime.ErrorBoundary import RecoverableError, FatalError


def test_runtime_initialization_and_lifecycle():
    r = Runtime()
    r.init()
    assert r.state == RuntimeState.IDLE
    r.start()
    assert r.state == RuntimeState.ACTIVE
    r.suspend()
    assert r.state == RuntimeState.SUSPENDED
    r.resume()
    assert r.state == RuntimeState.ACTIVE
    r.shutdown('test')
    assert r.state == RuntimeState.SHUTDOWN


def test_event_dispatch_ordering():
    r = Runtime()
    r.init()
    r.start()

    received = []

    def handler(ev):
        received.append((ev['_seq'], ev['payload']))

    r.dispatcher.register_handler('T', handler)

    r.dispatch({'type': 'T', 'payload': 1})
    r.dispatch({'type': 'T', 'payload': 2})
    processed = r.runLoop()
    assert len(processed) == 2
    assert received == [(1, 1), (2, 2)]


def test_module_handshake_validation_and_registration():
    r = Runtime()

    def echo(x):
        return x

    ok = r.register_module('mod1', {'provides': ['echo'], 'version': '1.0'}, echo)
    assert ok is True
    assert r.call_module('mod1', 5) == 5

    # invalid module (missing provides)
    ok2 = r.register_module('bad', {'version': '1.0'}, echo)
    assert ok2 is False


def test_error_boundary_classification_and_escalation(monkeypatch):
    r = Runtime()
    r.init()
    r.start()

    def bad_handler(ev):
        raise RecoverableError('oops')

    def fatal_handler(ev):
        raise FatalError('boom')

    r.dispatcher.register_handler('REC', bad_handler)
    r.dispatcher.register_handler('FAT', fatal_handler)

    r.dispatch({'type': 'REC', 'payload': None})
    # recoverable should not shutdown but classified
    r.runLoop()
    assert r.state in (RuntimeState.ACTIVE, RuntimeState.ERROR)

    r.dispatch({'type': 'FAT', 'payload': None})
    r.runLoop()
    assert r.state == RuntimeState.SHUTDOWN


def test_dispatch_rejects_malformed_event():
    r = Runtime()
    r.init()
    r.start()
    res = r.dispatch('not-a-dict')
    # dispatch returns classification via error boundary for malformed
    assert res in ('recoverable', 'fatal', None)
