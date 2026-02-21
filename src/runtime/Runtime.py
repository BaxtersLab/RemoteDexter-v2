from .RuntimeState import RuntimeState
from .EventDispatcher import EventDispatcher, MalformedEvent
from .ModuleHandshake import ModuleRegistry, ModuleInfo
from .ErrorBoundary import ErrorBoundary, RecoverableError, FatalError
from .LifecycleHooks import LifecycleHooks
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
        # session & transport components (initialized on init)
        self.session_manager: Optional[SessionManager] = None
        self.transport_registry: Optional[TransportRegistry] = None
        self.transport_manager: Optional[TransportManager] = None

    def init(self):
        self.hooks.onInit(self)
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

        self.state = RuntimeState.IDLE

    def start(self):
        if self.state == RuntimeState.SHUTDOWN:
            raise RuntimeError("Cannot start after shutdown")
        self.hooks.onStart(self)
        self.state = RuntimeState.ACTIVE

    def runLoop(self):
        # process events until queue empty or shutdown
        if self.state != RuntimeState.ACTIVE:
            return
        try:
            processed = self.dispatcher.flush()
            # handle any queued session request events synchronously
            # handlers registered earlier will perform actions
            return processed
        except Exception as e:
            self.error_boundary.handle_exception(e, {'phase': 'runLoop'})

    def dispatch(self, event: dict):
        try:
            self.dispatcher.dispatch(event)
        except MalformedEvent as me:
            return self.error_boundary.handle_exception(me, {'event': event})

    def suspend(self):
        if self.state != RuntimeState.ACTIVE:
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
        self.hooks.onShutdown(self)
        self.state = RuntimeState.SHUTDOWN

    # module handshake helpers
    def register_module(self, module_id: str, capabilities: dict, handler_callable):
        info = ModuleInfo(module_id=module_id, capabilities=capabilities)
        ok = self.registry.register(info, handler_callable)
        return ok

    def call_module(self, module_id: str, *args, **kwargs):
        return self.registry.safe_call(module_id, *args, **kwargs)

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
