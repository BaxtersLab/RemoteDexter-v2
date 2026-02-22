from .RuntimeState import RuntimeState
from .EventDispatcher import EventDispatcher, MalformedEvent
from .ModuleHandshake import ModuleRegistry, ModuleInfo
from .ErrorBoundary import ErrorBoundary, RecoverableError, FatalError
from .LifecycleHooks import LifecycleHooks
from .SubsystemManager import SubsystemManager
from .StateHistory import StateHistory
from .RuntimeChecks import RuntimeChecker, Invariant, Severity
from .SnapshotDiff import diff_snapshots, as_pure_data
from .RuntimeState import serialize
from .Telemetry import TelemetryBuffer
from . import Introspection as _introspection
from .ExternalAPI import ExternalAPIFacade, AccessContext
from .TemporalTrace import TemporalTrace
from .LifecycleMap import LifecycleMap
from typing import Optional
from src.transport.TransportRegistry import TransportRegistry
from src.transport.TransportManager import TransportManager
from src.transport.Transport import Result
from src.transport.TransportType import TransportType
from src.session.SessionManager import SessionManager
from src.transport.Transport import Transport


class Runtime:
    def __init__(self, hooks: Optional[LifecycleHooks] = None):
        self.state = RuntimeState.IDLE
        self.dispatcher = EventDispatcher()
        self.registry = ModuleRegistry()
        self.error_boundary = ErrorBoundary(self)
        self.hooks = hooks or LifecycleHooks()
        self._shutdown_reason = None
        # Guard against re-entrant execution (runLoop/run_scenario)
        self._in_execution = False
        # session & transport components (initialized on init)
        self.session_manager: Optional[SessionManager] = None
        self.transport_registry: Optional[TransportRegistry] = None
        self.transport_manager: Optional[TransportManager] = None

    def init(self):
        self.hooks.onInit(self)
        # initialize subsystem manager
        self.subsystem_manager = SubsystemManager(self.dispatcher, self.error_boundary)
        # provide backref for late registrations to record lifecycle events
        try:
            self.subsystem_manager._runtime = self
        except Exception:
            pass
        # initialize lifecycle map
        self.lifecycle_map = LifecycleMap()
        # initialize state history
        self.state_history = StateHistory()
        # initialize runtime checker
        self.runtime_checker = RuntimeChecker(self.error_boundary)
        # initialize telemetry (unbounded by default)
        self.telemetry = TelemetryBuffer()
        # temporal trace engine
        self.temporal_trace = TemporalTrace()
        # tick counter for deterministic trace indexing
        self._tick_counter = 0
        # initialize event stream
        try:
            from .EventStream import EventStream
            self.event_stream = EventStream()
        except Exception:
            self.event_stream = None
        # expose readonly introspection namespace bound to this runtime
        class _IntrospectFacade:
            def __init__(self, runtime):
                self._runtime = runtime

            def get_runtime_status(self):
                return _introspection.get_runtime_status(self._runtime)

            def get_latest_snapshots(self):
                return _introspection.get_latest_snapshots(self._runtime)

            def get_subsystem_states(self):
                return _introspection.get_subsystem_states(self._runtime)

            def get_telemetry(self, limit=None):
                return _introspection.get_telemetry(self._runtime, limit=limit)

            def get_lifecycle(self, subsystem: Optional[str] = None, phase: Optional[str] = None, limit: Optional[int] = None):
                return _introspection.get_lifecycle(self._runtime, subsystem=subsystem, phase=phase, limit=limit)

            def get_trace(self, limit: Optional[int] = None):
                return _introspection.get_trace(self._runtime, limit=limit)

        self.introspect = _IntrospectFacade(self)
        # expose external API facade (read-only, JSON-safe)
        self.api = ExternalAPIFacade(self)
        # initialize session & transport stack
        self.session_manager = SessionManager(self.dispatcher)
        self.transport_registry = TransportRegistry()
        self.transport_manager = TransportManager(self.transport_registry, self.dispatcher, self.session_manager)

        # register stub transports for each TransportType (fail-safe no-op transports)
        for t in TransportType:
            # create a stub transport that always fails gracefully
            class StubTransport(Transport):
                def __init__(self, id, type_str):
                    super().__init__(id, type_str)

                def connect(self, session):
                    return Result(False, 'stub-not-implemented')

                def disconnect(self, session):
                    return Result(True, 'stub-disconnect')

                def getStatus(self, session):
                    return None

            self.transport_registry.registerTransport(StubTransport(f"stub-{t.value}", t.value))

        # register runtime handlers for session requests
        self.dispatcher.register_handler('REQUEST_SESSION_START', self._handle_request_start)
        self.dispatcher.register_handler('REQUEST_SESSION_TERMINATE', self._handle_request_terminate)

        # taildata subsystem removed (reverted)

        # run subsystem init
        try:
            self.subsystem_manager.init_all(self)
        except Exception:
            pass

        # record lifecycle init events for any subsystems (some subsystems record during init_all)
        try:
            if hasattr(self, 'subsystem_manager') and self.subsystem_manager is not None and hasattr(self, 'lifecycle_map'):
                for _, s in self.subsystem_manager._subsystems:
                    try:
                        self.lifecycle_map.record(getattr(s, 'name', s.__class__.__name__), 'init', tick=0)
                    except Exception:
                        pass
        except Exception:
            pass

        self.state = RuntimeState.IDLE

    def start(self):
        if self.state == RuntimeState.SHUTDOWN:
            raise RuntimeError("Cannot start after shutdown")
        self.hooks.onStart(self)
        # start subsystems
        try:
            self.subsystem_manager.start_all(self)
        except Exception:
            pass
        # record a runtime-level start event
        try:
            if hasattr(self, 'lifecycle_map') and self.lifecycle_map is not None:
                self.lifecycle_map.record('runtime', 'start', tick=getattr(self, '_tick_counter', 0))
        except Exception:
            pass
        self.state = RuntimeState.ACTIVE

    def runLoop(self):
        # unified execution loop: call deterministic tick until idle or shutdown
        if self.state != RuntimeState.ACTIVE:
            return
        if getattr(self, '_in_execution', False):
            # already executing (re-entrant); avoid nested loop
            return
        self._in_execution = True
        processed_all = []
        try:
            # continue ticking while active
            while self.state == RuntimeState.ACTIVE:
                processed = self.tick()

                # halt if runtime requested shutdown during tick
                if self.state == RuntimeState.SHUTDOWN:
                    break

                # if no events processed this cycle, consider idle and exit loop
                if not processed:
                    break

                # accumulate processed events
                processed_all.extend(processed)

            # loop exit: notify subsystems that the run loop stopped
            try:
                self.hooks.onStop(self)
            except Exception as e:
                self.error_boundary.handle_exception(e, {'phase': 'onStop'})

            return processed_all
        except Exception as e:
            self.error_boundary.handle_exception(e, {'phase': 'runLoop'})
        finally:
            self._in_execution = False

    def tick(self):
        """
        Single deterministic tick: run pre_tick hooks, process queued events in insertion order,
        then run post_tick hooks. Returns list of processed events.
        """
        if self.state != RuntimeState.ACTIVE:
            return []
        try:
            # capture snapshot at start of tick
            try:
                from .RuntimeState import capture as capture_snapshot
                self._last_snapshot_before = capture_snapshot(self)
            except Exception:
                self._last_snapshot_before = None

            # increment tick counter for trace
            try:
                self._tick_counter = getattr(self, '_tick_counter', 0) + 1
                tick_no = self._tick_counter
            except Exception:
                tick_no = None

            # pre-tick phase
            # pre-tick snapshot capture and telemetry/event publish
            try:
                self.telemetry.record('tick', 'start', {'phase': 'pre_tick'})
            except Exception:
                pass
            try:
                if hasattr(self, 'event_stream') and self.event_stream is not None:
                    self.event_stream.publish('tick.start', {'phase': 'pre_tick'})
            except Exception:
                pass

            try:
                # record runtime-level pre_tick event
                try:
                    if hasattr(self, 'lifecycle_map') and self.lifecycle_map is not None:
                        self.lifecycle_map.record('runtime', 'pre_tick', tick=tick_no if tick_no is not None else 0)
                except Exception:
                    pass
                self.hooks.onPreTick(self)
            except Exception as e:
                # lifecycle hook failures are handled via ErrorBoundary
                self.error_boundary.handle_exception(e, {'phase': 'pre_tick'})

            # allow subsystems to run pre-tick operations and onTick logic
            if hasattr(self, 'subsystem_manager') and self.subsystem_manager:
                ok = self.subsystem_manager.pre_tick_all(self)
                if not ok:
                    return []
                ok = self.subsystem_manager.tick_all(self)
                if not ok:
                    return []

            # main tick: process all queued events (ordered by insertion)
            # main tick: process all queued events (ordered by insertion)
            processed = self.dispatcher.flush()

            # telemetry: record flush count and publish event
            try:
                self.telemetry.record('events', 'flush', {'count': len(processed)})
            except Exception:
                pass
            try:
                if hasattr(self, 'event_stream') and self.event_stream is not None:
                    self.event_stream.publish('events.flush', {'count': len(processed)})
            except Exception:
                pass

            # post-tick phase
            # post-tick phase
            try:
                # subsystems post-tick
                if hasattr(self, 'subsystem_manager') and self.subsystem_manager:
                    ok = self.subsystem_manager.post_tick_all(self)
                    if not ok:
                        return processed

                # record runtime-level post_tick event
                try:
                    if hasattr(self, 'lifecycle_map') and self.lifecycle_map is not None:
                        self.lifecycle_map.record('runtime', 'post_tick', tick=tick_no if tick_no is not None else 0)
                except Exception:
                    pass

                self.hooks.onPostTick(self)
            except Exception as e:
                self.error_boundary.handle_exception(e, {'phase': 'post_tick'})
            # capture snapshot at end of tick
            try:
                from .RuntimeState import capture as capture_snapshot
                self._last_snapshot_after = capture_snapshot(self)
                # record serialized snapshot in history
                try:
                    if hasattr(self, 'state_history') and self.state_history is not None:
                        self.state_history.record(self._last_snapshot_after)
                except Exception:
                    pass
                # telemetry: record snapshot event
                try:
                    if hasattr(self, 'telemetry') and self.telemetry is not None:
                            self.telemetry.record('snapshot', 'post_tick', {'subsystems': list(self._last_snapshot_after.subsystems.keys())})
                    try:
                        if hasattr(self, 'event_stream') and self.event_stream is not None:
                            self.event_stream.publish('snapshot.post', {'subsystems': list(self._last_snapshot_after.subsystems.keys())})
                    except Exception:
                        pass
                except Exception:
                    pass
            except Exception:
                self._last_snapshot_after = None

            # run runtime invariants after post-tick snapshot capture
            try:
                if hasattr(self, 'runtime_checker') and self.runtime_checker is not None and self._last_snapshot_after is not None:
                    self._last_check_results = self.runtime_checker.evaluate(self._last_snapshot_after, getattr(self, 'state_history', None))
                    # record invariant results to telemetry and publish
                    try:
                        if hasattr(self, 'telemetry') and self.telemetry is not None:
                            for r in getattr(self, '_last_check_results', []) or []:
                                self.telemetry.record('invariant', r.name, {'passed': r.passed, 'severity': r.severity.value})
                        if hasattr(self, 'event_stream') and self.event_stream is not None:
                            for r in getattr(self, '_last_check_results', []) or []:
                                self.event_stream.publish('invariant.result', {'name': r.name, 'passed': r.passed, 'severity': r.severity.value})
                    except Exception:
                        pass
            except Exception:
                pass

            # compute snapshot diff and expose for introspection / API
            try:
                if hasattr(self, '_last_snapshot_before') and hasattr(self, '_last_snapshot_after') and self._last_snapshot_before is not None and self._last_snapshot_after is not None:
                    try:
                        before_ser = serialize(self._last_snapshot_before)
                        after_ser = serialize(self._last_snapshot_after)
                    except Exception:
                        before_ser = None
                        after_ser = None
                    if before_ser is not None and after_ser is not None:
                        diff = diff_snapshots(before_ser, after_ser)
                        pure = as_pure_data(diff)
                        # store for introspection and API
                        self._last_diff_result = pure
                        # publish diff event
                        try:
                            if hasattr(self, 'event_stream') and self.event_stream is not None:
                                self.event_stream.publish('diff', pure)
                        except Exception:
                            pass
            except Exception:
                pass

            # record temporal trace entry for this tick (include lifecycle refs)
            try:
                try:
                    events = self.event_stream.all() if hasattr(self, 'event_stream') and self.event_stream is not None else []
                except Exception:
                    events = []
                try:
                    telem = self.telemetry.all() if hasattr(self, 'telemetry') and self.telemetry is not None else []
                except Exception:
                    telem = []
                try:
                    invs = []
                    for r in getattr(self, '_last_check_results', []) or []:
                        invs.append({'name': r.name, 'passed': r.passed, 'severity': r.severity.value})
                except Exception:
                    invs = []
                try:
                    subs = _introspection.get_subsystem_states(self)
                except Exception:
                    subs = []

                diff_payload = getattr(self, '_last_diff_result', None)
                lifecycle_refs = []
                try:
                    if hasattr(self, 'lifecycle_map') and self.lifecycle_map is not None:
                        # gather lifecycle events for this tick deterministically
                        all_ev = self.lifecycle_map.get_events()
                        for ev in all_ev:
                            if ev.get('tick') == (tick_no if tick_no is not None else 0):
                                lifecycle_refs.append(ev.get('id'))
                except Exception:
                    lifecycle_refs = []

                if hasattr(self, 'temporal_trace') and getattr(self, 'temporal_trace', None) is not None:
                    self.temporal_trace.record_tick(tick_no if tick_no is not None else 0, events, diff_payload, telem, invs, subs, lifecycle_refs=lifecycle_refs)
            except Exception:
                pass

            return processed
        except Exception as e:
            self.error_boundary.handle_exception(e, {'phase': 'tick'})

    def dispatch(self, event: dict):
        try:
            self.dispatcher.dispatch(event)
        except MalformedEvent as me:
            return self.error_boundary.handle_exception(me, {'event': event})

    def suspend(self):
        if self.state != RuntimeState.ACTIVE:
            # capture snapshot at end of tick
            return
        self.hooks.onSuspend(self)
        self.state = RuntimeState.SUSPENDED

    def resume(self):
        if self.state != RuntimeState.SUSPENDED:
            return
        self.hooks.onResume(self)
        self.state = RuntimeState.ACTIVE

    def shutdown(self, reason: str = None):
        self._shutdown_reason = reason
        # emit final telemetry event and publish shutdown event if available
        try:
            if hasattr(self, 'telemetry') and self.telemetry is not None:
                self.telemetry.record('runtime', 'shutdown', {'reason': reason})
        except Exception:
            pass
        try:
            if hasattr(self, 'event_stream') and self.event_stream is not None:
                self.event_stream.publish('runtime.shutdown', {'reason': reason})
        except Exception:
            pass

        self.hooks.onShutdown(self)
        self.state = RuntimeState.SHUTDOWN

        # record invariant results to telemetry if present
        try:
            for r in getattr(self, '_last_check_results', []) or []:
                try:
                    if hasattr(self, 'telemetry') and self.telemetry is not None:
                        self.telemetry.record('invariant', r.name, {'passed': r.passed, 'severity': r.severity.value})
                except Exception:
                    pass
        except Exception:
            pass

        # ensure subsystems see shutdown
        try:
            if hasattr(self, 'subsystem_manager') and self.subsystem_manager:
                self.subsystem_manager.shutdown_all(self)
        except Exception:
            pass
        # record runtime-level shutdown event and final teardown marker
        try:
            if hasattr(self, 'lifecycle_map') and self.lifecycle_map is not None:
                self.lifecycle_map.record('runtime', 'shutdown.requested', tick=(getattr(self, '_tick_counter', 0) + 1), metadata={'reason': reason})
        except Exception:
            pass

    # module handshake helpers
    def register_module(self, module_id: str, capabilities: dict, handler_callable):
        info = ModuleInfo(module_id=module_id, capabilities=capabilities)
        ok = self.registry.register(info, handler_callable)
        return ok

    def call_module(self, module_id: str, *args, **kwargs):
        return self.registry.safe_call(module_id, *args, **kwargs)

    def rollback_to(self, snapshot_data) -> bool:
        """Validate a rollback request without mutating subsystem state.

        Accepts either a `RuntimeSnapshot` or a serialized snapshot dict.
        Returns True if the snapshot is structurally compatible with current subsystems, False otherwise.
        """
        try:
            from .RuntimeState import deserialize, RuntimeSnapshot
            # normalize to RuntimeSnapshot
            if isinstance(snapshot_data, dict):
                snap = deserialize(snapshot_data)
            elif isinstance(snapshot_data, RuntimeSnapshot):
                snap = snapshot_data
            else:
                return False

            # Validate keys match deterministic subsystem order
            if not hasattr(self, 'subsystem_manager') or self.subsystem_manager is None:
                # no subsystems; only compatible with empty snapshot
                return len(snap.subsystems) == 0

            expected_names = [getattr(s, 'name', s.__class__.__name__) for _, s in self.subsystem_manager._subsystems]
            snap_names = list(snap.subsystems.keys())
            return expected_names == snap_names
        except Exception:
            return False

    def run_scenario(self, scenario):
        """Run a Scenario (or scenario-like dict) via the ScenarioHarness using normal runtime loops.

        The scenario may be either a `Scenario` instance or a pure-data dict with `name` and `steps`.
        """
        # prevent nested execution with runLoop or another run_scenario
        if getattr(self, '_in_execution', False):
            return None
        try:
            self._in_execution = True
            from .ScenarioHarness import ScenarioHarness, Scenario, ScenarioStep
            sc = scenario
            if isinstance(scenario, dict):
                # convert pure-data dict to Scenario
                steps = [ScenarioStep(**s) if isinstance(s, dict) else s for s in scenario.get('steps', [])]
                sc = Scenario(name=scenario.get('name', 'unnamed'), steps=steps, metadata=scenario.get('metadata'))
            harness = ScenarioHarness(self)
            return harness.run(sc)
        except Exception:
            return None
        finally:
            self._in_execution = False

    # Runtime internal handlers for session events
    def _handle_request_start(self, ev: dict):
        # ev payload: { 'type': 'REQUEST_SESSION_START', 'transportType': 'RDP' }
        t = ev.get('transportType')
        s = self.session_manager.createSession(t)
        # immediately attempt to connect via transport manager
        self.transport_manager.connectSession(s)

    def _handle_request_terminate(self, ev: dict):
        sid = ev.get('sessionId')
        s = self.session_manager.getSession(sid)
        if not s:
            return
        self.transport_manager.disconnectSession(s)
        self.session_manager.terminateSession(sid)


__all__ = ["Runtime"]
