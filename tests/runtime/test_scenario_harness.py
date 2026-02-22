import json
import copy
from pathlib import Path

import pytest

from src.runtime.Runtime import Runtime
from src.runtime.RuntimeState import RuntimeState


class CounterSubsystem:
    def __init__(self, fail_at=None):
        self.count = 0
        self.name = 'Counter'
        self._fail_at = fail_at

    def onInit(self, runtime):
        pass

    def onStart(self, runtime):
        pass

    def onPreTick(self, runtime):
        pass

    def onTick(self, runtime):
        self.count += 1
        if self._fail_at and self.count >= self._fail_at:
            # raise a FatalError from ErrorBoundary to trigger runtime.shutdown
            from src.runtime.ErrorBoundary import FatalError

            raise FatalError("counter reached fail threshold")

    def onPostTick(self, runtime):
        pass

    def onStop(self, runtime):
        pass

    def onShutdown(self, runtime):
        pass

    def get_state(self):
        return {"count": self.count}


def make_runtime(fail_at=None):
    r = Runtime()
    r.init()
    # register our test subsystem deterministically
    r.subsystem_manager.register(CounterSubsystem(fail_at=fail_at), order=10)
    r.start()
    return r


def load_scenario(name: str):
    p = Path("tests/runtime/scenarios") / f"{name}.json"
    with p.open("r", encoding="utf-8") as fh:
        return json.load(fh)


def test_scenario_deterministic_replay_and_pure_data_immutability():
    scenario = load_scenario("simple_counter")
    scenario_copy = copy.deepcopy(scenario)

    r1 = make_runtime()
    r2 = make_runtime()

    res1 = r1.run_scenario(scenario)
    res2 = r2.run_scenario(scenario)

    # snapshots and invariants should be identical across independent fresh runtimes
    assert res1.get('snapshots') == res2.get('snapshots')
    assert res1.get('invariants') == res2.get('invariants')

    # telemetry may contain timestamps and runtime-local metadata; compare payload/type only
    def norm_telem(tlist):
        out = []
        for step in (tlist or []):
            if step is None:
                out.append(None)
                continue
            s = []
            for ev in step:
                # ev may be dict or object; normalize to dict-like
                if isinstance(ev, dict):
                    s.append({'type': ev.get('type'), 'payload': ev.get('payload')})
                else:
                    s.append({'type': getattr(ev, 'type', None), 'payload': getattr(ev, 'payload', None)})
            out.append(s)
        return out

    assert norm_telem(res1.get('telemetry')) == norm_telem(res2.get('telemetry'))

    # scenario pure-data should remain unchanged
    assert scenario == scenario_copy


def test_scenario_shutdown_on_fatal_error():
    scenario = load_scenario("shutdown_on_three")

    # runtime that will raise FatalError on third tick
    r = make_runtime(fail_at=3)
    res = r.run_scenario(scenario)

    # runtime should have been shutdown by the FatalError
    assert r.state == RuntimeState.SHUTDOWN

    # The harness appends an 'ok' flag for expect_shutdown steps; verify it's True
    last_step = res.get("steps", [])[-1]
    assert last_step.get("action") == "expect_shutdown"
    assert last_step.get("ok") is True
