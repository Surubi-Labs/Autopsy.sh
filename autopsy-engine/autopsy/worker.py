"""Worker entry point — consumes jobs from Redis and dispatches to handlers."""

from __future__ import annotations

import json
import logging
import os

import redis

from autopsy.db.connection import close_pool, get_connection
from autopsy.pipeline.clone_handler import handle_clone
from autopsy.pipeline.deliver_handler import handle_deliver
from autopsy.pipeline.embed_handler import handle_embed
from autopsy.pipeline.extract_handler import handle_extract
from autopsy.pipeline.report_handler import handle_report
from autopsy.queue.consumer import JobConsumer

logger = logging.getLogger(__name__)


def main() -> None:
    """Start the worker process."""
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s: %(message)s",
    )

    redis_url = os.environ.get("REDIS_URL", "redis://localhost:6379")
    data_dir = os.environ.get("DATA_DIR", "/data/repos")
    openai_key = os.environ.get("OPENAI_API_KEY")

    openai_client = None
    if openai_key:
        from openai import OpenAI

        openai_client = OpenAI(api_key=openai_key)

    redis_client = redis.from_url(redis_url)
    consumer = JobConsumer(redis_client, consumer_name="python-worker-1")

    logger.info("Starting Autopsy worker (data_dir=%s)", data_dir)

    def handler(message: dict[str, str]) -> None:
        stage = message.get("stage", "").upper()
        run_id = message["runId"]
        repo_id = message["repoId"]
        org_id = message["orgId"]
        payload = _parse_payload(message.get("payload"))

        logger.info("Handling stage=%s run=%s repo=%s", stage, run_id, repo_id)

        if stage == "METRICS":
            logger.info("Skipping METRICS stage (handled by Kotlin)")
            return

        with get_connection() as conn:
            if stage == "CLONE":
                handle_clone(conn, redis_client, run_id, repo_id, org_id, data_dir)
            elif stage == "EXTRACT":
                handle_extract(
                    conn, redis_client, run_id, repo_id, org_id, data_dir, payload,
                )
            elif stage == "EMBED":
                handle_embed(
                    conn, redis_client, run_id, repo_id, org_id, openai_client,
                )
            elif stage == "REPORT":
                handle_report(
                    conn, redis_client, run_id, repo_id, org_id,
                    openai_client, payload.get("headSha") if payload else None,
                )
            elif stage == "DELIVER":
                handle_deliver(conn, redis_client, run_id, repo_id, org_id)
            else:
                logger.warning("Unknown stage: %s", stage)

    try:
        consumer.consume(handler)
    finally:
        close_pool()
        redis_client.close()
        logger.info("Worker shutdown complete")


def _parse_payload(raw: str | None) -> dict[str, str] | None:
    """Parse JSON payload from message field."""
    if not raw:
        return None
    try:
        return json.loads(raw)  # type: ignore[no-any-return]
    except (json.JSONDecodeError, TypeError):
        return None


if __name__ == "__main__":
    main()
