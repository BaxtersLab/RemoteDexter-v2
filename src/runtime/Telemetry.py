from dataclasses import dataclass
from collections import deque
import time
from threading import Lock
from typing import List, Dict, Optional


@dataclass(frozen=True)
class TelemetryEvent:
    timestamp: float
    category: str
    message: str
    payload: Optional[Dict]

    def __repr__(self) -> str:
        # concise and stable representation
        return f"TelemetryEvent({self.timestamp:.6f},{self.category},{self.message})"


class TelemetryBuffer:
    """Fixed-size ring buffer for TelemetryEvent objects.

    Uses a deque with `maxlen` to provide ring behavior (oldest dropped when full).
    Thread-safe for simple append/flush operations with a Lock.
    """

    def __init__(self, max_size: int = 1024):
        self._max_size = int(max_size)
        self._buf: deque = deque(maxlen=self._max_size)
        self._lock = Lock()

    def append(self, event: TelemetryEvent) -> None:
        with self._lock:
            self._buf.append(event)

    # Backwards-compatible helpers expected by other runtime code/tests
    def record(self, category: str, message: str, payload: Optional[Dict] = None) -> TelemetryEvent:
        ev = TelemetryEvent(timestamp=time.time(), category=category, message=message, payload=payload or {})
        self.append(ev)
        return ev

    def all(self) -> List[TelemetryEvent]:
        return self.snapshot()

    def latest(self) -> Optional[TelemetryEvent]:
        with self._lock:
            if not self._buf:
                return None
            return self._buf[-1]

    def clear(self) -> None:
        with self._lock:
            self._buf.clear()

    def snapshot(self) -> List[TelemetryEvent]:
        with self._lock:
            return list(self._buf)

    def flush(self) -> List[TelemetryEvent]:
        with self._lock:
            out = list(self._buf)
            self._buf.clear()
        return out

    def __len__(self) -> int:
        with self._lock:
            return len(self._buf)


class TelemetrySink:
    """Facade used by runtime subsystems to emit telemetry."""

    def __init__(self, buffer: Optional[TelemetryBuffer] = None):
        self._buffer = buffer if buffer is not None else TelemetryBuffer()

    def emit(self, category: str, message: str, payload: Optional[Dict] = None) -> None:
        ev = TelemetryEvent(timestamp=time.time(), category=category, message=message, payload=payload or {})
        self._buffer.append(ev)

    def export_snapshot(self) -> List[TelemetryEvent]:
        return self._buffer.snapshot()

    def export_flush(self) -> List[TelemetryEvent]:
        return self._buffer.flush()


def to_timeline_events(events: List[TelemetryEvent]) -> List[Dict]:
    out: List[Dict] = []
    for e in events:
        out.append({
            "ts": e.timestamp,
            "type": "telemetry",
            "category": e.category,
            "message": e.message,
            "payload": e.payload or {},
        })
    return out


__all__ = ["TelemetryEvent", "TelemetryBuffer", "TelemetrySink", "to_timeline_events"]
