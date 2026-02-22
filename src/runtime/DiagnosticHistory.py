from collections import deque
from typing import List, Optional

from src.runtime.Diagnostics import DiagnosticSnapshot


class DiagnosticHistory:
    """In-memory bounded history of DiagnosticSnapshot objects.

    - Uses deque with maxlen for ring behavior
    - No thread-safety guarantees (single-threaded use expected)
    """

    def __init__(self, max_size: int = 128):
        self._max_size = int(max_size)
        self._buf = deque(maxlen=self._max_size)

    def add(self, snapshot: DiagnosticSnapshot) -> None:
        # store snapshot reference as-is (immutable by design)
        self._buf.append(snapshot)

    def latest(self) -> Optional[DiagnosticSnapshot]:
        if not self._buf:
            return None
        return self._buf[-1]

    def all(self) -> List[DiagnosticSnapshot]:
        return list(self._buf)

    def window(self, since_ts: float) -> List[DiagnosticSnapshot]:
        # return snapshots with timestamp >= since_ts in chronological order
        return [s for s in self._buf if getattr(s, "timestamp", 0) >= since_ts]

    def clear(self) -> None:
        self._buf.clear()


__all__ = ["DiagnosticHistory"]
