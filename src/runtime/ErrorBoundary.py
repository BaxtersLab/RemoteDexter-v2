class RecoverableError(Exception):
    pass


class FatalError(Exception):
    pass


class ErrorBoundary:
    def __init__(self, runtime):
        self.runtime = runtime

    def handle_exception(self, exc: Exception, context: dict = None):
        # Classify
        if isinstance(exc, RecoverableError):
            # trigger fallback but keep runtime running
            self._handle_recoverable(exc, context)
            return "recoverable"
        else:
            # treat as fatal
            self._handle_fatal(exc, context)
            return "fatal"

    def _handle_recoverable(self, exc: Exception, context: dict):
        # Default fallback: log and continue
        try:
            print(f"[ErrorBoundary] Recoverable error: {exc} context={context}")
        except Exception:
            pass

    def _handle_fatal(self, exc: Exception, context: dict):
        try:
            print(f"[ErrorBoundary] Fatal error: {exc} context={context}")
        except Exception:
            pass
        # escalate to runtime shutdown
        try:
            self.runtime.shutdown(reason=str(exc))
        except Exception:
            pass


__all__ = ["RecoverableError", "FatalError", "ErrorBoundary"]
