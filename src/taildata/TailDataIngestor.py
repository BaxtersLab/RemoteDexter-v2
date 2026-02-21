from .TailDataPacket import TailDataPacket
from .TailDataNormalizer import TailDataNormalizer
from .TailDataRouter import TailDataRouter


class TailDataIngestor:
    def __init__(self, router: TailDataRouter):
        self.normalizer = TailDataNormalizer()
        self.router = router

    def ingest(self, raw_packet: dict):
        # Expect keys: session_id, timestamp, payload
        try:
            session_id = raw_packet.get('session_id')
            timestamp = raw_packet.get('timestamp')
            payload = raw_packet.get('payload')
            packet = TailDataPacket(session_id=session_id, timestamp=float(timestamp), payload=payload)
        except Exception as e:
            raise ValueError(f"Malformed packet: {e}")

        normalized = self.normalizer.validate_and_normalize(packet)
        self.router.route(normalized)


__all__ = ["TailDataIngestor"]
