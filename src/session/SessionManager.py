from typing import Dict, List, Optional
from uuid import uuid4
from src.session.Session import Session, SessionState


class SessionManager:
    def __init__(self, dispatcher):
        self._sessions: Dict[str, Session] = {}
        self.dispatcher = dispatcher

    def createSession(self, transportType: str) -> Session:
        sid = str(uuid4())
        s = Session(id=sid, transportType=transportType)
        self._sessions[sid] = s
        # emit created
        self.dispatcher.dispatch({"type": "SESSION_CREATED", "session": s})
        self.dispatcher.dispatch({"type": "SESSION_STATE_CHANGED", "sessionId": s.id, "newState": s.state.value})
        return s

    def getSession(self, id: str) -> Optional[Session]:
        return self._sessions.get(id)

    def listSessions(self) -> List[Session]:
        return list(self._sessions.values())

    def terminateSession(self, id: str):
        s = self._sessions.get(id)
        if not s:
            return
        s.markTerminated()
        self.dispatcher.dispatch({"type": "SESSION_TERMINATED", "sessionId": s.id})
        self.dispatcher.dispatch({"type": "SESSION_STATE_CHANGED", "sessionId": s.id, "newState": s.state.value})


__all__ = ["SessionManager"]
