from .RuntimeState import RuntimeState
from .EventDispatcher import EventDispatcher, MalformedEvent
from .ModuleHandshake import ModuleRegistry, ModuleInfo
from .ErrorBoundary import ErrorBoundary, RecoverableError, FatalError
from .LifecycleHooks import LifecycleHooks
from typing import Optional


class Runtime:
    def __init__(self, hooks: Optional[LifecycleHooks] = None):
        self.state = RuntimeState.IDLE
        self.dispatcher = EventDispatcher()
        self.registry = ModuleRegistry()
        self.error_boundary = ErrorBoundary(self)
        self.hooks = hooks or LifecycleHooks()
        self._shutdown_reason = None

    def init(self):
        self.hooks.onInit(self)
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


__all__ = ["Runtime"]
