"""Pipeline stage: deliver report via email and/or Slack."""

from __future__ import annotations

import logging
from typing import Any
from uuid import UUID

from autopsy.db.repositories import get_report_by_run_id, get_repository
from autopsy.delivery.dispatcher import dispatch_delivery

logger = logging.getLogger(__name__)


def handle_deliver(
    conn: Any,
    redis_client: Any,
    run_id: str,
    repo_id: str,
    org_id: str,
) -> None:
    """Deliver the report via configured channels, then mark run as completed."""
    report = get_report_by_run_id(conn, UUID(run_id))
    repo = get_repository(conn, UUID(repo_id))

    if report and repo:
        channels = dispatch_delivery(
            conn=conn,
            org_id=org_id,
            run_id=run_id,
            repo_name=repo["name"],
            report_summary=report.get("summary"),
            risk_score=report.get("risk_score"),
        )
        if channels:
            logger.info("Delivered run %s via: %s", run_id, ", ".join(channels))
        else:
            logger.info("No delivery channels configured for run %s", run_id)
    else:
        logger.warning("No report found for run %s, skipping delivery", run_id)

    # Always mark run as completed regardless of delivery outcome
    with conn.cursor() as cur:
        cur.execute(
            "UPDATE analysis_runs"
            " SET status = 'completed', completed_at = now(), current_stage = 'deliver'"
            " WHERE id = %s",
            (run_id,),
        )
    conn.commit()
