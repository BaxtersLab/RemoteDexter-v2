from dataclasses import dataclass
from typing import Dict, Callable, Any


@dataclass
class ModuleInfo:
    module_id: str
    capabilities: Dict[str, Any]


class ModuleRegistry:
    def __init__(self):
        self._modules: Dict[str, ModuleInfo] = {}
        self._handlers: Dict[str, Callable] = {}

    def validate_module(self, info: ModuleInfo) -> bool:
        # Basic validation: must have id and capabilities must be a dict
        if not info.module_id or not isinstance(info.capabilities, dict):
            return False
        # Example capability check: require 'version' and 'provides' keys
        if 'provides' not in info.capabilities:
            return False
        return True

    def register(self, info: ModuleInfo, handler: Callable) -> bool:
        if not self.validate_module(info):
            return False
        self._modules[info.module_id] = info
        self._handlers[info.module_id] = handler
        return True

    def unregister(self, module_id: str):
        self._modules.pop(module_id, None)
        self._handlers.pop(module_id, None)

    def get_handler(self, module_id: str):
        return self._handlers.get(module_id)

    def safe_call(self, module_id: str, *args, **kwargs):
        handler = self.get_handler(module_id)
        if not handler:
            raise KeyError(f"Module {module_id} not registered")
        return handler(*args, **kwargs)


__all__ = ["ModuleInfo", "ModuleRegistry"]
