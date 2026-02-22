from enum import Enum


class RuntimeState(Enum):
    ACTIVE = "ACTIVE"
    IDLE = "IDLE"
    SUSPENDED = "SUSPENDED"
    ERROR = "ERROR"
    SHUTDOWN = "SHUTDOWN"


__all__ = ["RuntimeState"]

from dataclasses import dataclass, asdict
from typing import Dict, Any


@dataclass(frozen=True)
class SubsystemSnapshot:
    name: str
    state: Dict[str, Any]


@dataclass(frozen=True)
class RuntimeSnapshot:
    subsystems: Dict[str, Dict[str, Any]]


def capture(runtime) -> RuntimeSnapshot:
    """Capture a deterministic runtime snapshot by querying each subsystem's `get_state()` in registration order.

    Returns a `RuntimeSnapshot` containing an ordered mapping of subsystem name -> pure-data state dict.
    """
    if not hasattr(runtime, 'subsystem_manager') or runtime.subsystem_manager is None:
        return RuntimeSnapshot(subsystems={})

    subs = {}
    # rely on SubsystemManager deterministic ordering
    for s in runtime.subsystem_manager._subsystems:
        _, subsystem = s
        name = getattr(subsystem, 'name', subsystem.__class__.__name__)
        state = None
        try:
            if hasattr(subsystem, 'get_state'):
                # Expect a pure-data dict or dataclass convertible to dict
                st = subsystem.get_state()
                if hasattr(st, '__dict__'):
                    st = asdict(st)
                subs[name] = st
            else:
                subs[name] = None
        except Exception:
            # state capture must not raise
            subs[name] = None

    return RuntimeSnapshot(subsystems=subs)


def diff(a: RuntimeSnapshot, b: RuntimeSnapshot) -> Dict[str, Dict[str, Any]]:
    """Shallow diff between two RuntimeSnapshot instances.

    Returns mapping of subsystem name -> {'before': ..., 'after': ...} for changed entries.
    """
    changes = {}
    keys = list(dict(a.subsystems).keys())
    # ensure deterministic key order: use keys from `a`, then any extra from `b`
    for k in keys:
        before = a.subsystems.get(k)
        after = b.subsystems.get(k)
        if before != after:
            changes[k] = {'before': before, 'after': after}
    for k in b.subsystems.keys():
        if k not in keys:
            before = a.subsystems.get(k)
            after = b.subsystems.get(k)
            if before != after:
                changes[k] = {'before': before, 'after': after}
    return changes


def serialize(snapshot: RuntimeSnapshot) -> Dict[str, Any]:
    """Return a JSON-safe dict representation of a RuntimeSnapshot with deterministic key ordering."""
    # Convert subsystems mapping to a plain dict where each value is JSON-serializable
    out = {'subsystems': {}}
    for k in snapshot.subsystems:
        v = snapshot.subsystems[k]
        # assume v is JSON-safe (primitives, lists, dicts) or None
        out['subsystems'][k] = v
    return out


def deserialize(data: Dict[str, Any]) -> RuntimeSnapshot:
    """Reconstruct a RuntimeSnapshot from a serialized dict without mutating runtime state."""
    subs = {}
    subs_in = data.get('subsystems', {}) if isinstance(data, dict) else {}
    for k in subs_in:
        subs[k] = subs_in[k]
    return RuntimeSnapshot(subsystems=subs)


