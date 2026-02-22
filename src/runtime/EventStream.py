from dataclasses import dataclass, asdict
from typing import Any, Callable, Dict, List, Optional
import time


@dataclass(frozen=True)
class StreamEvent:
    timestamp: float
    type: str
    payload: Optional[Dict[str, Any]] = None


class EventStream:
    def __init__(self, max_size: Optional[int] = None):
        self._items: List[StreamEvent] = []
        self.max_size = max_size
        self._subscribers: List[Callable[[Dict[str, Any]], None]] = []

    def publish(self, type: str, payload: Optional[Dict[str, Any]] = None) -> StreamEvent:
        # shallow-copy payload to avoid side effects
        p = None
        if payload is not None:
            try:
                p = dict(payload)
            except Exception:
                p = payload

        ev = StreamEvent(timestamp=time.time(), type=type, payload=p)
        self._append(ev)
        # deliver pure-data dicts to subscribers
        data = {'timestamp': ev.timestamp, 'type': ev.type, 'payload': ev.payload}
        for cb in list(self._subscribers):
            try:
                cb(dict(data))
            except Exception:
                # swallow subscriber errors to avoid affecting runtime
                pass
        return ev

    def _append(self, ev: StreamEvent):
        self._items.append(ev)
        if self.max_size is not None and len(self._items) > self.max_size:
            del self._items[0]

    def all(self) -> List[Dict[str, Any]]:
        return [{'timestamp': e.timestamp, 'type': e.type, 'payload': e.payload} for e in list(self._items)]

    def subscribe(self, callback: Callable[[Dict[str, Any]], None], context: Optional[Any] = None) -> bool:
        # callers must provide a callable; we store and return True
        if not callable(callback):
            return False
        self._subscribers.append(callback)
        return True

    def unsubscribe(self, callback: Callable[[Dict[str, Any]], None]) -> bool:
        try:
            self._subscribers.remove(callback)
            return True
        except ValueError:
            return False


__all__ = ["StreamEvent", "EventStream"]
