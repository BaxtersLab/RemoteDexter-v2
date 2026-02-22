from typing import List, Any, Optional
from .RuntimeState import RuntimeSnapshot, serialize


class StateHistory:
    """Stores serialized runtime snapshots in deterministic order.

    By default this is unbounded; callers may wrap or replace if bounded storage is required.
    """

    def __init__(self):
        self._items: List[Any] = []

    def record(self, snapshot: RuntimeSnapshot):
        # store serialized snapshot (pure-data) to avoid holding references
        sdata = serialize(snapshot)
        self._items.append(sdata)

    def get(self, index: int) -> Optional[Any]:
        if index < 0 or index >= len(self._items):
            return None
        return self._items[index]

    def latest(self) -> Optional[Any]:
        if not self._items:
            return None
        return self._items[-1]

    def previous(self) -> Optional[Any]:
        if len(self._items) < 2:
            return None
        return self._items[-2]

    def all(self) -> List[Any]:
        return list(self._items)


__all__ = ["StateHistory"]
