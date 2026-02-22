from src.runtime.EventDispatcher import EventDispatcher


class TailDataRouter:
    def __init__(self, dispatcher: EventDispatcher):
        self.dispatcher = dispatcher

    def route(self, normalized_packet):
        # normalized_packet: has session_id, timestamp, payload
        ev = {
            'type': 'TAIL_DATA',
            'sessionId': normalized_packet.session_id,
            'timestamp': normalized_packet.timestamp,
            'payload': normalized_packet.payload,
        }
        # Use dispatch to preserve deterministic ordering
        self.dispatcher.dispatch(ev)


__all__ = ["TailDataRouter"]
