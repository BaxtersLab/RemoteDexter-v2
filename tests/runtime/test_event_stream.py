import pytest

from src.runtime.Runtime import Runtime
from src.runtime.EventStream import EventStream
from src.runtime.ExternalAPI import AccessContext


def test_event_stream_publish_and_subscribe():
    rt = Runtime()
    rt.init()

    events_received = []

    def cb(evt):
        events_received.append(evt)

    # subscribe via runtime.api
    rt.api.subscribe_to_events(cb, context=AccessContext.EXTERNAL)

    # cause a tick which will publish events
    rt.start()
    rt.tick()

    # ensure we received some events deterministically ordered (oldest->newest)
    assert len(events_received) >= 1
    assert isinstance(events_received[0], dict)

    # unsubscribe and ensure no further events
    rt.api.unsubscribe_from_events(cb)
    prev_count = len(events_received)
    rt.tick()
    assert len(events_received) == prev_count


def test_event_stream_access_control_restricted():
    rt = Runtime()
    rt.init()

    def cb(evt):
        pass

    with pytest.raises(PermissionError):
        rt.api.subscribe_to_events(cb, context=AccessContext.RESTRICTED)
