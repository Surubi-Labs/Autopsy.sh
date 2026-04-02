"""Pipeline stage: generate AI health report."""

from __future__ import annotations

import json
import logging
from typing import Any
from uuid import UUID

from autopsy.ai.models import Chunk
from autopsy.ai.reporter import generate_report
from autopsy.db.repositories import (
    get_extractions_by_run,
    save_report,
    update_repo_last_analyzed,
    update_run_status,
)
from autopsy.queue.producer import enqueue_next_stage

logger = logging.getLogger(__name__)


def handle_report(
    conn: Any,
    redis_client: Any,
    run_id: str,
    repo_id: str,
    org_id: str,
    openai_client: Any | None = None,
    head_sha: str | None = None,
) -> None:
    """Generate report from metrics + context, write to DB, enqueue DELIVER."""
    update_run_status(conn, UUID(run_id), "running", "report")

    try:
        # Read all extractions as metrics context
        extractions = get_extractions_by_run(conn, UUID(run_id))
        metrics_json = json.dumps(
            {"extractions": [e["payload"] for e in extractions]},
            indent=2,
        )

        # Build chunks from doc extractions
        doc_rows = get_extractions_by_run(conn, UUID(run_id), "doc")
        chunks = [
            Chunk(
                source_type=row["payload"].get("type", "doc"),
                source_ref=row["payload"].get("path", ""),
                content=row["payload"].get("content", ""),
                metadata=row["payload"].get("title", ""),
            )
            for row in doc_rows
        ]

        if openai_client:
            markdown = generate_report(
                client=openai_client,
                metrics_json=metrics_json,
                chunks=chunks,
            )
        else:
            markdown = (
                "# Health Report\n\n"
                "(AI report generation skipped — no OpenAI client)\n\n"
                f"## Raw Metrics\n\n```json\n{metrics_json}\n```"
            )

        save_report(conn, UUID(run_id), UUID(repo_id), markdown)

        # Update repo's last analyzed SHA
        if head_sha:
            update_repo_last_analyzed(conn, UUID(repo_id), head_sha)

        logger.info("Generated report for run %s", run_id)

        enqueue_next_stage(redis_client, run_id, repo_id, org_id, "REPORT")

    except Exception as e:
        update_run_status(conn, UUID(run_id), "failed", "report", str(e))
        raise
