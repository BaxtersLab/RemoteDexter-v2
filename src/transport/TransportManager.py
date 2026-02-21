from src.transport.TransportRegistry import TransportRegistry
from src.transport.Transport import Result
from src.session.SessionManager import SessionManager
from typing import Optional


class TransportManager:
    def __init__(self, registry: TransportRegistry, dispatcher, session_manager: SessionManager):
        self.registry = registry
        self.dispatcher = dispatcher
        self.session_manager = session_manager

    def resolve(self, transportType: str):
        return self.registry.getTransport(transportType)

    def connectSession(self, session) -> Result:
        transport = self.resolve(session.transportType)
        if not transport:
            session.markFailed('no-transport')
            self.dispatcher.dispatch({"type": "SESSION_FAILED", "sessionId": session.id, "reason": "no-transport"})
            self.dispatcher.dispatch({"type": "SESSION_STATE_CHANGED", "sessionId": session.id, "newState": session.state.value})
            return Result(False, 'no-transport')

        session.markConnecting()
        self.dispatcher.dispatch({"type": "SESSION_CONNECTING", "sessionId": session.id})
        res = transport.connect(session)
        if res.success:
            session.markActive()
            self.dispatcher.dispatch({"type": "SESSION_ACTIVE", "sessionId": session.id})
            self.dispatcher.dispatch({"type": "SESSION_STATE_CHANGED", "sessionId": session.id, "newState": session.state.value})
        else:
            session.markFailed(res.message)
            self.dispatcher.dispatch({"type": "SESSION_FAILED", "sessionId": session.id, "reason": res.message})
            self.dispatcher.dispatch({"type": "SESSION_STATE_CHANGED", "sessionId": session.id, "newState": session.state.value})
        return res

    def disconnectSession(self, session) -> Result:
        transport = self.resolve(session.transportType)
        if transport:
            res = transport.disconnect(session)
        else:
            res = Result(False, 'no-transport')
        session.markTerminated()
        self.dispatcher.dispatch({"type": "SESSION_TERMINATED", "sessionId": session.id})
        self.dispatcher.dispatch({"type": "SESSION_STATE_CHANGED", "sessionId": session.id, "newState": session.state.value})
        return res

    def queryStatus(self, session):
        transport = self.resolve(session.transportType)
        if not transport:
            return None
        return transport.getStatus(session)


__all__ = ["TransportManager"]
