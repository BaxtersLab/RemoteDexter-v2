from dataclasses import dataclass
from enum import Enum
from typing import Callable, List, Optional, Any


class Severity(Enum):
    INFO = "info"
    WARN = "warn"
    FATAL = "fatal"


@dataclass
class Invariant:
    name: str
    severity: Severity
    fn: Callable[[Any, Any], bool]


@dataclass
class CheckResult:
    name: str
    severity: Severity
    passed: bool
    details: Optional[str] = None


class RuntimeChecker:
    def __init__(self, error_boundary):
        self.error_boundary = error_boundary
        self.invariants: List[Invariant] = []

    def register(self, inv: Invariant):
        self.invariants.append(inv)

    def evaluate(self, snapshot, history) -> List[CheckResult]:
        results: List[CheckResult] = []
        for inv in self.invariants:
            try:
                ok = bool(inv.fn(snapshot, history))
                results.append(CheckResult(name=inv.name, severity=inv.severity, passed=ok))
                if not ok and inv.severity == Severity.FATAL:
                    # escalate via ErrorBoundary
                    from .ErrorBoundary import FatalError
                    self.error_boundary.handle_exception(FatalError(f"Invariant failed: {inv.name}"), {'invariant': inv.name})
            except Exception as e:
                # treat evaluation errors as fatal
                results.append(CheckResult(name=inv.name, severity=inv.severity, passed=False, details=str(e)))
                if inv.severity == Severity.FATAL:
                    from .ErrorBoundary import FatalError
                    self.error_boundary.handle_exception(FatalError(f"Invariant exception: {inv.name}: {e}"), {'invariant': inv.name})
        return results


__all__ = ["Severity", "Invariant", "CheckResult", "RuntimeChecker"]
