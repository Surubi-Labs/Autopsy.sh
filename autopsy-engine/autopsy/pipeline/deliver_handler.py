"""Pipeline stage: deliver report (stub for Phase 4)."""

from __future__ import annotations

import logging
from typing import Any

logger = logging.getLogger(__name__)


def handle_deliver(
    conn: Any,
    redis_client: Any,
    run_id: str,
    repo_id: str,
    org_id: str,
) -> None:
    """Mark the analysis run as completed. Delivery is Phase 4."""
    logger.info("Delivery not implemented yet — marking run %s as completed", run_id)

    with conn.cursor() as cur:
        cur.execute(
            "UPDATE analysis_runs"
            " SET status = 'completed', completed_at = now(), current_stage = 'deliver'"
            " WHERE id = %s",
            (run_id,),
        )
    conn.commit()
