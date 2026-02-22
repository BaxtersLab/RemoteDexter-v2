from dataclasses import dataclass
from typing import Any, Dict, List, Optional
import time


@dataclass(frozen=True)
class TraceEntry:
    tick: int
    timestamp: float
    events: List[Dict[str, Any]]
    diff: Optional[Dict[str, Any]]
    telemetry: List[Dict[str, Any]]
    invariants: List[Dict[str, Any]]
    subsystems: List[Dict[str, Any]]


class TemporalTrace:
    def __init__(self):
        self._entries: List[TraceEntry] = []

    def record_tick(self, tick: int, events: List[Dict[str, Any]], diff: Optional[Dict[str, Any]], telemetry: List[Dict[str, Any]], invariants: List[Dict[str, Any]], subsystems: List[Dict[str, Any]]):
        ts = time.time()
        # normalize inputs to pure-data lists/dicts
        evs = list(events) if events is not None else []
        telem = list(telemetry) if telemetry is not None else []
        invs = list(invariants) if invariants is not None else []
        subs = list(subsystems) if subsystems is not None else []
        entry = TraceEntry(tick=tick, timestamp=ts, events=evs, diff=diff, telemetry=telem, invariants=invs, subsystems=subs)
        self._entries.append(entry)

    def get_trace(self, limit: Optional[int] = None) -> List[Dict[str, Any]]:
        items = list(self._entries)
        if limit is not None and isinstance(limit, int):
            items = items[-limit:]
        out = []
        for e in items:
            out.append({'tick': e.tick, 'timestamp': e.timestamp, 'events': e.events, 'diff': e.diff, 'telemetry': e.telemetry, 'invariants': e.invariants, 'subsystems': e.subsystems})
        return out

    def clear(self):
        self._entries.clear()

    def export_json(self) -> List[Dict[str, Any]]:
        """Return a JSON-safe pure-data representation of the trace.

        Ensures nested timestamps within events/telemetry are removed to keep only
        the top-level tick timestamp if present.
        """
        out = []
        for e in list(self._entries):
            events = []
            for ev in e.events or []:
                if isinstance(ev, dict):
                    ev_copy = {k: v for k, v in ev.items() if k != 'timestamp'}
                else:
                    ev_copy = {'type': getattr(ev, 'type', None), 'payload': getattr(ev, 'payload', None)}
                events.append(ev_copy)

            telemetry = []
            for te in e.telemetry or []:
                if isinstance(te, dict):
                    tcopy = {k: v for k, v in te.items() if k != 'timestamp'}
                else:
                    tcopy = {'category': getattr(te, 'category', None), 'message': getattr(te, 'message', None), 'payload': getattr(te, 'payload', None)}
                telemetry.append(tcopy)

            subs = []
            for s in e.subsystems or []:
                subs.append(s)

            diff = e.diff if isinstance(e.diff, dict) or e.diff is None else None

            out.append({'tick': e.tick, 'timestamp': e.timestamp, 'events': events, 'diff': diff, 'telemetry': telemetry, 'invariants': e.invariants or [], 'subsystems': subs})
        return out

    def export_csv(self) -> str:
        """Return a CSV string summarizing the trace entries deterministically.

        Columns: tick,timestamp,diff_entries_count,events_count,telemetry_count,invariants_count,subsystems_json
        """
        import csv, io, json

        out_io = io.StringIO()
        writer = csv.writer(out_io)
        writer.writerow(['tick', 'timestamp', 'diff_entries', 'events', 'telemetry', 'invariants', 'subsystems'])
        for e in list(self._entries):
            diff_count = 0
            if isinstance(e.diff, dict):
                diff_count = len(e.diff.get('entries', []))
            events_count = len(e.events or [])
            telem_count = len(e.telemetry or [])
            inv_count = len(e.invariants or [])
            subs_json = json.dumps(e.subsystems or [], sort_keys=True)
            writer.writerow([e.tick, e.timestamp, diff_count, events_count, telem_count, inv_count, subs_json])
        return out_io.getvalue()


__all__ = ['TraceEntry', 'TemporalTrace']
