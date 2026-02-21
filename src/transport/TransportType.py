from enum import Enum


class TransportType(Enum):
    RDP = "RDP"
    RUSTDESK = "RUSTDESK"
    CUSTOM = "CUSTOM"


__all__ = ["TransportType"]
