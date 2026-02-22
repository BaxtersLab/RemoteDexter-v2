import re
from typing import Dict
from .TailDataPacket import TailDataPacket


def to_snake_case(name: str) -> str:
    s1 = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', name)
    s2 = re.sub('([a-z0-9])([A-Z])', r'\1_\2', s1)
    return re.sub('[^0-9a-zA-Z_]+', '_', s2).lower()


class NormalizedTailDataPacket:
    def __init__(self, session_id: str, timestamp: float, payload: Dict):
        self.session_id = session_id
        self.timestamp = timestamp
        self.payload = payload


class TailDataNormalizer:
    def __init__(self):
        # track last timestamp per session to enforce monotonicity
        self._last_ts = {}

    def validate_and_normalize(self, packet: TailDataPacket) -> NormalizedTailDataPacket:
        # basic validation already in TailDataPacket
        sid = packet.session_id
        ts = packet.timestamp
        payload = packet.payload

        # enforce monotonic timestamps per session
        last = self._last_ts.get(sid)
        if last is not None and ts <= last:
            # bump to slightly higher than last
            ts = last + 1e-6
        self._last_ts[sid] = ts

        # normalize payload keys to snake_case lowercase
        norm = {}
        for k, v in payload.items():
            if not isinstance(k, str):
                continue
            nk = to_snake_case(k)
            # strip forbidden fields
            if nk.startswith('__'):
                continue
            norm[nk] = v

        return NormalizedTailDataPacket(session_id=sid, timestamp=ts, payload=norm)


__all__ = ["TailDataNormalizer", "NormalizedTailDataPacket", "to_snake_case"]
