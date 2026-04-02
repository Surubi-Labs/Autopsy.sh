"""Redis Streams job consumer."""

from __future__ import annotations

import contextlib
import logging
import signal
from typing import TYPE_CHECKING, Any

if TYPE_CHECKING:
    from collections.abc import Callable

logger = logging.getLogger(__name__)

STREAM_KEY = "autopsy:jobs"
GROUP_NAME = "autopsy-workers"


class JobConsumer:
    """Consumes jobs from a Redis Stream using consumer groups."""

    def __init__(
        self,
        redis_client: Any,
        consumer_name: str,
        stream: str = STREAM_KEY,
        group: str = GROUP_NAME,
    ) -> None:
        self._redis = redis_client
        self._consumer_name = consumer_name
        self._stream = stream
        self._group = group
        self._running = True

        signal.signal(signal.SIGTERM, self._shutdown)
        signal.signal(signal.SIGINT, self._shutdown)

    def ensure_group(self) -> None:
        """Create the consumer group if it doesn't exist."""
        with contextlib.suppress(Exception):
            self._redis.xgroup_create(self._stream, self._group, id="0", mkstream=True)

    def consume(
        self,
        handler: Callable[[dict[str, str]], None],
        block_ms: int = 5000,
    ) -> None:
        """Consume messages in a loop. ACK on success, leave pending on failure."""
        self.ensure_group()

        while self._running:
            try:
                messages = self._redis.xreadgroup(
                    self._group,
                    self._consumer_name,
                    {self._stream: ">"},
                    count=1,
                    block=block_ms,
                )
                if not messages:
                    continue

                for _stream_name, entries in messages:
                    for msg_id, fields in entries:
                        msg_id_str = (
                            msg_id.decode() if isinstance(msg_id, bytes) else str(msg_id)
                        )
                        decoded = {
                            (k.decode() if isinstance(k, bytes) else k): (
                                v.decode() if isinstance(v, bytes) else v
                            )
                            for k, v in fields.items()
                        }
                        try:
                            handler(decoded)
                            self._redis.xack(self._stream, self._group, msg_id_str)
                            logger.info("Processed and ACKed message %s", msg_id_str)
                        except Exception:
                            logger.exception("Handler failed for message %s", msg_id_str)

            except Exception:
                if self._running:
                    logger.exception("Error reading from stream")

    def claim_stale(self, min_idle_ms: int = 300_000) -> list[dict[str, str]]:
        """Reclaim messages that have been pending for too long."""
        pending = self._redis.xpending_range(
            self._stream, self._group, min="-", max="+", count=10,
        )
        reclaimed: list[dict[str, str]] = []
        for entry in pending:
            msg_id = entry["message_id"]
            if isinstance(msg_id, bytes):
                msg_id = msg_id.decode()
            idle = entry.get("time_since_delivered", 0)
            if idle >= min_idle_ms:
                claimed = self._redis.xclaim(
                    self._stream, self._group, self._consumer_name,
                    min_idle_time=min_idle_ms, message_ids=[msg_id],
                )
                for _claimed_id, fields in claimed:
                    decoded = {
                        (k.decode() if isinstance(k, bytes) else k): (
                            v.decode() if isinstance(v, bytes) else v
                        )
                        for k, v in fields.items()
                    }
                    reclaimed.append(decoded)
        return reclaimed

    def stop(self) -> None:
        """Stop the consumer loop."""
        self._running = False

    def _shutdown(self, _signum: int, _frame: Any) -> None:
        logger.info("Received shutdown signal, stopping consumer...")
        self._running = False
