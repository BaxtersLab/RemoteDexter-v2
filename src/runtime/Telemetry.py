from dataclasses import dataclass, asdict
from typing import Any, Dict, List, Optional
import time


@dataclass(frozen=True)
class TelemetryEvent:
    timestamp: float
    category: str
    message: str
    payload: Optional[Dict[str, Any]] = None


class TelemetryBuffer:
    def __init__(self, max_size: Optional[int] = None):
        # deterministic append order preserved by list
        self._items: List[TelemetryEvent] = []
        self.max_size = max_size

    def record(self, category: str, message: str, payload: Optional[Dict[str, Any]] = None) -> TelemetryEvent:
        # copy payload to avoid side-effects
        p = None
        if payload is not None:
            try:
                # prefer a shallow copy; assume JSON-safe payloads
                p = dict(payload)
            except Exception:
                p = payload

        ev = TelemetryEvent(timestamp=time.time(), category=category, message=message, payload=p)
        self._append(ev)
        return ev

    def _append(self, ev: TelemetryEvent):
        self._items.append(ev)
        if self.max_size is not None and len(self._items) > self.max_size:
            # drop oldest deterministically
            del self._items[0]

    def all(self) -> List[TelemetryEvent]:
        return list(self._items)

    def latest(self) -> Optional[TelemetryEvent]:
        if not self._items:
            return None
        return self._items[-1]

    def clear(self):
        self._items.clear()


__all__ = ["TelemetryEvent", "TelemetryBuffer"]
