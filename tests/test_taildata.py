import pytest
from src.taildata.TailDataPacket import TailDataPacket
from src.taildata.TailDataNormalizer import TailDataNormalizer
from src.taildata.TailDataRouter import TailDataRouter
from src.taildata.TailDataIngestor import TailDataIngestor
from src.runtime.EventDispatcher import EventDispatcher
from src.runtime.Runtime import Runtime


def test_packet_validation():
    with pytest.raises(ValueError):
        TailDataPacket(session_id='', timestamp=1.0, payload={})
    with pytest.raises(ValueError):
        TailDataPacket(session_id='s', timestamp='not-float', payload={})
    with pytest.raises(ValueError):
        TailDataPacket(session_id='s', timestamp=1.0, payload='not-dict')


def test_normalizer_and_router_and_runtime_integration():
    # dispatcher will collect events
    disp = EventDispatcher()
    router = TailDataRouter(disp)
    ing = TailDataIngestor(router)

    events = []

    def collector(ev):
        events.append(ev)

    disp.register_handler('TAIL_DATA', collector)

    raw = { 'session_id': 'sess1', 'timestamp': 1.0, 'payload': {'KeyOne': 123, '__secret': 'x'} }
    ing.ingest(raw)

    processed = disp.flush()
    assert len(processed) == 1
    e = processed[0]
    assert e['type'] == 'TAIL_DATA'
    assert e['sessionId'] == 'sess1'
    assert 'key_one' in e['payload']
    assert '__secret' not in e['payload']

    # runtime integration: ensure runtime receives TAIL_DATA when session emits
    r = Runtime()
    r.init()
    r.start()

    # collect tail events from runtime dispatcher
    got = []
    r.dispatcher.register_handler('TAIL_DATA', lambda ev: got.append(ev))

    # simulate session emit
    # note: session emit hook was reverted in main; this test is for archival branch only
    r.session_manager.emit_tail_data('sess2', {'timestamp': 2.0, 'data': {'FooBar': 'v'}})
    r.runLoop()
    assert len(got) == 1
    ev = got[0]
    assert ev['type'] == 'TAIL_DATA'
    assert ev['sessionId'] == 'sess2'
