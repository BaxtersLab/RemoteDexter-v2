from typing import Dict, Optional
from src.transport.Transport import Transport


class TransportRegistry:
    def __init__(self):
        # map transport type string -> Transport
        self._by_type: Dict[str, Transport] = {}

    def registerTransport(self, transport: Transport) -> bool:
        t = transport.type
        if t in self._by_type:
            # do not allow multiple transports for same type
            return False
        self._by_type[t] = transport
        return True

    def getTransport(self, type_str: str) -> Optional[Transport]:
        return self._by_type.get(type_str)


__all__ = ["TransportRegistry"]
