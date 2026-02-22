from typing import List, Tuple
from .ErrorBoundary import FatalError


class SubsystemManager:
    """Manages ordered subsystems and invokes lifecycle hooks deterministically.

    Subsystems are registered with an integer `order` (lower runs first).
    Each subsystem is expected to implement the lifecycle methods used below.
    """

    def __init__(self, dispatcher, error_boundary):
        self.dispatcher = dispatcher
        self.error_boundary = error_boundary
        self._subsystems: List[Tuple[int, object]] = []
        # optional backreference to runtime (set by Runtime during init)
        self._runtime = None

    def register(self, subsystem, order: int = 100):
        self._subsystems.append((order, subsystem))
        self._subsystems.sort(key=lambda x: x[0])
        # if runtime already attached, record an init lifecycle event for newly registered subsystem
        try:
            rt = getattr(self, '_runtime', None)
            if rt is not None and hasattr(rt, 'lifecycle_map') and rt.lifecycle_map is not None:
                rt.lifecycle_map.record(getattr(subsystem, 'name', subsystem.__class__.__name__), 'init', tick=0)
        except Exception:
            pass

    def _iter(self):
        for _, s in self._subsystems:
            yield s

    def init_all(self, runtime):
        for s in self._iter():
            try:
                s.onInit(runtime)
                # record lifecycle event for init
                try:
                    if hasattr(runtime, 'lifecycle_map') and runtime.lifecycle_map is not None:
                        runtime.lifecycle_map.record(getattr(s, 'name', s.__class__.__name__), 'init', tick=0)
                except Exception:
                    pass
            except Exception as e:
                res = self.error_boundary.handle_exception(e, {'subsystem': getattr(s, '__name__', str(s)), 'phase': 'init'})
                if res == 'fatal':
                    return False
        return True

    def start_all(self, runtime):
        for s in self._iter():
            try:
                s.onStart(runtime)
                # record lifecycle event for start
                try:
                    if hasattr(runtime, 'lifecycle_map') and runtime.lifecycle_map is not None:
                        runtime.lifecycle_map.record(getattr(s, 'name', s.__class__.__name__), 'start', tick=getattr(runtime, '_tick_counter', 0))
                except Exception:
                    pass
            except Exception as e:
                res = self.error_boundary.handle_exception(e, {'subsystem': getattr(s, '__name__', str(s)), 'phase': 'start'})
                if res == 'fatal':
                    return False
        return True

    def pre_tick_all(self, runtime):
        for s in self._iter():
            try:
                # record pre-tick activation
                try:
                    if hasattr(runtime, 'lifecycle_map') and runtime.lifecycle_map is not None:
                        runtime.lifecycle_map.record(getattr(s, 'name', s.__class__.__name__), 'pre_tick', tick=getattr(runtime, '_tick_counter', 0))
                except Exception:
                    pass
                s.onPreTick(runtime)
            except Exception as e:
                res = self.error_boundary.handle_exception(e, {'subsystem': getattr(s, '__name__', str(s)), 'phase': 'pre_tick'})
                if res == 'fatal':
                    return False
        return True

    def tick_all(self, runtime):
        for s in self._iter():
            try:
                # optional onTick method
                if hasattr(s, 'onTick'):
                    s.onTick(runtime)
                    # record tick participation
                    try:
                        if hasattr(runtime, 'lifecycle_map') and runtime.lifecycle_map is not None:
                            runtime.lifecycle_map.record(getattr(s, 'name', s.__class__.__name__), 'tick', tick=getattr(runtime, '_tick_counter', 0))
                    except Exception:
                        pass
            except Exception as e:
                res = self.error_boundary.handle_exception(e, {'subsystem': getattr(s, '__name__', str(s)), 'phase': 'tick'})
                if res == 'fatal':
                    return False
        return True

    def post_tick_all(self, runtime):
        for s in self._iter():
            try:
                s.onPostTick(runtime)
                # record post-tick completion
                try:
                    if hasattr(runtime, 'lifecycle_map') and runtime.lifecycle_map is not None:
                        runtime.lifecycle_map.record(getattr(s, 'name', s.__class__.__name__), 'post_tick', tick=getattr(runtime, '_tick_counter', 0))
                except Exception:
                    pass
            except Exception as e:
                res = self.error_boundary.handle_exception(e, {'subsystem': getattr(s, '__name__', str(s)), 'phase': 'post_tick'})
                if res == 'fatal':
                    return False
        return True

    def stop_all(self, runtime):
        for s in self._iter():
            try:
                s.onStop(runtime)
                try:
                    if hasattr(runtime, 'lifecycle_map') and runtime.lifecycle_map is not None:
                        runtime.lifecycle_map.record(getattr(s, 'name', s.__class__.__name__), 'stop', tick=getattr(runtime, '_tick_counter', 0))
                except Exception:
                    pass
            except Exception as e:
                self.error_boundary.handle_exception(e, {'subsystem': getattr(s, '__name__', str(s)), 'phase': 'stop'})

    def shutdown_all(self, runtime):
        for s in self._iter():
            try:
                s.onShutdown(runtime)
                try:
                    if hasattr(runtime, 'lifecycle_map') and runtime.lifecycle_map is not None:
                        # use tick after last tick + 1 for final teardown marker
                        runtime.lifecycle_map.record(getattr(s, 'name', s.__class__.__name__), 'shutdown', tick=(getattr(runtime, '_tick_counter', 0) + 1))
                except Exception:
                    pass
            except Exception as e:
                self.error_boundary.handle_exception(e, {'subsystem': getattr(s, '__name__', str(s)), 'phase': 'shutdown'})


__all__ = ["SubsystemManager"]
