from enum import Enum


class RuntimeState(Enum):
    ACTIVE = "ACTIVE"
    IDLE = "IDLE"
    SUSPENDED = "SUSPENDED"
    ERROR = "ERROR"
    SHUTDOWN = "SHUTDOWN"


__all__ = ["RuntimeState"]
