from dataclasses import dataclass
from typing import Any


@dataclass
class Result:
    success: bool
    message: str = ""


@dataclass
class TransportStatus:
    status: str
    message: str = ""


class Transport:
    def __init__(self, id: str, type_str: str):
        self.id = id
        self.type = type_str

    def connect(self, session) -> Result:
        raise NotImplementedError()

    def disconnect(self, session) -> Result:
        raise NotImplementedError()

    def getStatus(self, session) -> TransportStatus:
        raise NotImplementedError()


__all__ = ["Transport", "Result", "TransportStatus"]
