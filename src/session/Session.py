from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional
from enum import Enum


class SessionState(Enum):
    PENDING = "PENDING"
    CONNECTING = "CONNECTING"
    ACTIVE = "ACTIVE"
    FAILED = "FAILED"
    TERMINATED = "TERMINATED"


@dataclass
class Session:
    id: str
    transportType: str
    state: SessionState = field(default=SessionState.PENDING)
    createdAt: datetime = field(default_factory=datetime.utcnow)
    updatedAt: datetime = field(default_factory=datetime.utcnow)
    failureReason: Optional[str] = None

    def _touch(self):
        self.updatedAt = datetime.utcnow()

    def markConnecting(self):
        self.state = SessionState.CONNECTING
        self._touch()

    def markActive(self):
        self.state = SessionState.ACTIVE
        self._touch()

    def markFailed(self, reason: str = None):
        self.state = SessionState.FAILED
        self.failureReason = reason
        self._touch()

    def markTerminated(self):
        self.state = SessionState.TERMINATED
        self._touch()


__all__ = ["Session", "SessionState"]
