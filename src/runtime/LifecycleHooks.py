class LifecycleHooks:
    def onInit(self, runtime):
        pass

    def onStart(self, runtime):
        pass

    def onSuspend(self, runtime):
        pass

    def onResume(self, runtime):
        pass

    def onStop(self, runtime):
        pass

    def onShutdown(self, runtime):
        pass

    def onPreTick(self, runtime):
        pass

    def onPostTick(self, runtime):
        pass


__all__ = ["LifecycleHooks"]
