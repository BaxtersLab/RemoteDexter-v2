from dataclasses import dataclass
from typing import Dict


@dataclass(frozen=True)
class TailDataPacket:
    session_id: str
    timestamp: float
    payload: Dict

    def __post_init__(self):
        if not self.session_id or not isinstance(self.session_id, str):
            raise ValueError("session_id must be a non-empty string")
        if not isinstance(self.timestamp, float):
            raise ValueError("timestamp must be a float")
        if not isinstance(self.payload, dict):
            raise ValueError("payload must be a dict")


__all__ = ["TailDataPacket"]
