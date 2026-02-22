from dataclasses import dataclass
from typing import Any, Dict, List, Optional

from .RuntimeState import serialize


@dataclass
class ScenarioStep:
    action: str
    payload: Optional[Dict[str, Any]] = None
    expected: Optional[Dict[str, Any]] = None


@dataclass
class Scenario:
    name: str
    steps: List[ScenarioStep]
    metadata: Optional[Dict[str, Any]] = None


class ScenarioHarness:
    """Deterministic harness to run scenarios against a runtime instance.

    The harness drives runtime.dispatch() and runtime.tick() only; it records
    serialized snapshots, telemetry, and invariant results for each step.
    """

    def __init__(self, runtime):
        self.runtime = runtime

    def run(self, scenario: Scenario) -> Dict[str, Any]:
        results = {
            'scenario': scenario.name,
            'steps': [],
            'snapshots': [],
            'telemetry': [],
            'invariants': [],
        }

        for step in scenario.steps:
            step_res = {'action': step.action, 'payload': step.payload}

            if step.action == 'dispatch':
                # dispatch event into runtime
                self.runtime.dispatch(step.payload or {})

            elif step.action == 'tick':
                # run a deterministic tick
                self.runtime.tick()

                # capture snapshots via existing API
                try:
                    before = getattr(self.runtime, '_last_snapshot_before', None)
                    after = getattr(self.runtime, '_last_snapshot_after', None)
                    sb = serialize(before) if before is not None else None
                    sa = serialize(after) if after is not None else None
                except Exception:
                    sb = None
                    sa = None

                results['snapshots'].append({'before': sb, 'after': sa})

                # telemetry (serializable)
                try:
                    telem = []
                    if hasattr(self.runtime, 'telemetry') and self.runtime.telemetry is not None:
                        for e in self.runtime.telemetry.all():
                            telem.append({'timestamp': e['timestamp'] if isinstance(e, dict) else getattr(e, 'timestamp', None), 'type': e['type'] if isinstance(e, dict) else getattr(e, 'type', None), 'payload': e['payload'] if isinstance(e, dict) else getattr(e, 'payload', None)})
                    results['telemetry'].append(telem)
                except Exception:
                    results['telemetry'].append(None)

                # invariant results summary
                try:
                    invs = []
                    for r in getattr(self.runtime, '_last_check_results', []) or []:
                        invs.append({'name': r.name, 'passed': r.passed, 'severity': r.severity.value})
                    results['invariants'].append(invs)
                except Exception:
                    results['invariants'].append(None)

            elif step.action == 'expect_shutdown':
                step_res['ok'] = (self.runtime.state is not None and getattr(self.runtime.state, 'name', None) == 'SHUTDOWN')

            results['steps'].append(step_res)

        return results


__all__ = ["ScenarioStep", "Scenario", "ScenarioHarness"]
