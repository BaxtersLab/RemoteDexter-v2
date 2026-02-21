import pytest
from src.transport.TransportRegistry import TransportRegistry
from src.transport.Transport import Transport, Result
from src.transport.TransportType import TransportType
from src.transport.TransportManager import TransportManager
from src.session.SessionManager import SessionManager
from src.runtime.EventDispatcher import EventDispatcher


class SuccessTransport(Transport):
    def connect(self, session):
        return Result(True, 'ok')

    def disconnect(self, session):
        return Result(True, 'disconnected')

    def getStatus(self, session):
        return {'status': 'ok'}


class FailureTransport(Transport):
    def connect(self, session):
        return Result(False, 'failed')

    def disconnect(self, session):
        return Result(True, 'disconnected')

    def getStatus(self, session):
        return {'status': 'failed'}


def test_transport_registry_and_manager():
    dispatcher = EventDispatcher()
    sm = SessionManager(dispatcher)
    registry = TransportRegistry()
    # register success for RDP
    s = SuccessTransport('s1', TransportType.RDP.value)
    assert registry.registerTransport(s) is True
    # second registration for same type fails
    assert registry.registerTransport(SuccessTransport('s2', TransportType.RDP.value)) is False

    tm = TransportManager(registry, dispatcher, sm)

    session = sm.createSession(TransportType.RDP.value)
    res = tm.connectSession(session)
    assert res.success is True
    assert session.state.value == 'ACTIVE'

    # test failure transport
    reg2 = TransportRegistry()
    f = FailureTransport('f1', TransportType.RUSTDESK.value)
    assert reg2.registerTransport(f) is True
    tm2 = TransportManager(reg2, dispatcher, sm)
    s2 = sm.createSession(TransportType.RUSTDESK.value)
    r2 = tm2.connectSession(s2)
    assert r2.success is False
    assert s2.state.value == 'FAILED'
