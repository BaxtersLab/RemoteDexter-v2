from dataclasses import dataclass, asdict
from typing import Any, Dict, List, Optional


@dataclass(frozen=True)
class LifecycleEvent:
    id: int
    subsystem: str
    phase: str
    tick: int
    timestamp: float
    metadata: Optional[Dict[str, Any]] = None


class LifecycleMap:
    """Records deterministic lifecycle events for subsystems.

    Timestamps are normalized to be deterministic across runs by deriving
    them from the tick number and an internal sequence counter.
    """

    def __init__(self):
        self._events: List[LifecycleEvent] = []
        self._seq = 0

    def _next_id(self) -> int:
        self._seq += 1
        return self._seq

    def record(self, subsystem: str, phase: str, tick: int = 0, metadata: Optional[Dict[str, Any]] = None) -> int:
        eid = self._next_id()
        # deterministic normalized timestamp: tick + seq * 1e-6
        ts = float(tick) + (float(eid) * 1e-6)
        ev = LifecycleEvent(id=eid, subsystem=subsystem, phase=phase, tick=tick, timestamp=ts, metadata=metadata)
        self._events.append(ev)
        return eid

    def get_events(self, subsystem: Optional[str] = None, phase: Optional[str] = None, limit: Optional[int] = None) -> List[Dict[str, Any]]:
        items = list(self._events)
        if subsystem is not None:
            items = [e for e in items if e.subsystem == subsystem]
        if phase is not None:
            items = [e for e in items if e.phase == phase]
        if limit is not None and isinstance(limit, int):
            items = items[-limit:]
        return [asdict(e) for e in items]

    def serialize(self) -> List[Dict[str, Any]]:
        return [asdict(e) for e in list(self._events)]


__all__ = ['LifecycleEvent', 'LifecycleMap']
