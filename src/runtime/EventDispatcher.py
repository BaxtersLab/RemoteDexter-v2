from typing import Callable, Dict, Any, List
from collections import deque
import threading


class MalformedEvent(Exception):
    pass


class EventDispatcher:
    def __init__(self):
        # handlers: event_type -> list of callables
        self._handlers: Dict[str, List[Callable[[dict], None]]] = {}
        self._queue = deque()
        self._seq = 0
        self._lock = threading.Lock()

    def register_handler(self, event_type: str, handler: Callable[[dict], None]):
        self._handlers.setdefault(event_type, []).append(handler)

    def _validate(self, event: dict):
        if not isinstance(event, dict):
            raise MalformedEvent("Event must be a dict")
        if 'type' not in event:
            raise MalformedEvent("Event missing 'type' field")

    def dispatch(self, event: dict):
        # deterministic ordering via sequence numbers
        self._validate(event)
        with self._lock:
            self._seq += 1
            ev = dict(event)
            ev['_seq'] = self._seq
            self._queue.append(ev)

    def flush(self):
        # process all queued events in order
        processed = []
        while True:
            try:
                ev = self._queue.popleft()
            except IndexError:
                break
            handlers = self._handlers.get(ev['type'], [])
            for h in handlers:
                h(ev)
            processed.append(ev)
        return processed


__all__ = ["EventDispatcher", "MalformedEvent"]
