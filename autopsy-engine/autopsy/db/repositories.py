"""Database repository functions — stateless, connection passed as first arg."""

from __future__ import annotations

import json
from typing import TYPE_CHECKING, Any
from uuid import UUID

if TYPE_CHECKING:
    from psycopg import Connection


def save_extractions(
    conn: Connection[Any],
    run_id: UUID,
    extractions: list[dict[str, Any]],
) -> None:
    """Bulk insert extraction records."""
    with conn.cursor() as cur:
        for ext in extractions:
            cur.execute(
                """
                INSERT INTO extractions (run_id, data_type, module_path, payload)
                VALUES (%s, %s, %s, %s::jsonb)
                """,
                (str(run_id), ext["data_type"], ext.get("module_path"), json.dumps(ext["payload"])),
            )
    conn.commit()


def save_chunks(
    conn: Connection[Any],
    repo_id: UUID,
    chunks: list[dict[str, Any]],
) -> None:
    """Bulk insert chunk records with optional embedding vectors."""
    with conn.cursor() as cur:
        # Clear old chunks for this repo
        cur.execute("DELETE FROM chunks WHERE repo_id = %s", (str(repo_id),))
        for chunk in chunks:
            embedding = chunk.get("embedding")
            if embedding:
                cur.execute(
                    """
                    INSERT INTO chunks (repo_id, source_type, source_ref, content, embedding)
                    VALUES (%s, %s, %s, %s, %s::vector)
                    """,
                    (
                        str(repo_id),
                        chunk["source_type"],
                        chunk["source_ref"],
                        chunk["content"],
                        str(embedding),
                    ),
                )
            else:
                cur.execute(
                    """
                    INSERT INTO chunks (repo_id, source_type, source_ref, content)
                    VALUES (%s, %s, %s, %s)
                    """,
                    (str(repo_id), chunk["source_type"], chunk["source_ref"], chunk["content"]),
                )
    conn.commit()


def save_report(
    conn: Connection[Any],
    run_id: UUID,
    repo_id: UUID,
    markdown: str,
    summary: str | None = None,
    risk_score: float | None = None,
) -> UUID:
    """Insert a report and return its ID."""
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO reports (run_id, repo_id, markdown, summary, risk_score)
            VALUES (%s, %s, %s, %s, %s)
            RETURNING id
            """,
            (str(run_id), str(repo_id), markdown, summary, risk_score),
        )
        row = cur.fetchone()
        conn.commit()
        return UUID(str(row[0])) if row else UUID(int=0)


def get_extractions_by_run(
    conn: Connection[Any],
    run_id: UUID,
    data_type: str | None = None,
) -> list[dict[str, Any]]:
    """Get extractions for a run, optionally filtered by type."""
    with conn.cursor() as cur:
        if data_type:
            cur.execute(
                "SELECT id, data_type, module_path, payload"
                " FROM extractions WHERE run_id = %s AND data_type = %s",
                (str(run_id), data_type),
            )
        else:
            cur.execute(
                "SELECT id, data_type, module_path, payload FROM extractions WHERE run_id = %s",
                (str(run_id),),
            )
        rows = cur.fetchall()
        return [
            {
                "id": str(row[0]),
                "data_type": row[1],
                "module_path": row[2],
                "payload": row[3] if isinstance(row[3], dict) else json.loads(row[3]),
            }
            for row in rows
        ]


def update_run_status(
    conn: Connection[Any],
    run_id: UUID,
    status: str,
    stage: str | None = None,
    error: str | None = None,
) -> None:
    """Update an analysis run's status and stage."""
    with conn.cursor() as cur:
        cur.execute(
            """
            UPDATE analysis_runs
            SET status = %s, current_stage = %s, error = %s,
                started_at = CASE WHEN started_at IS NULL AND %s = 'running'
                    THEN now() ELSE started_at END
            WHERE id = %s
            """,
            (status, stage, error, status, str(run_id)),
        )
    conn.commit()


def get_repository(
    conn: Connection[Any],
    repo_id: UUID,
) -> dict[str, Any] | None:
    """Get a repository record by ID."""
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT id, org_id, name, git_url, default_branch,
                   last_analyzed_sha, schedule, active
            FROM repositories WHERE id = %s
            """,
            (str(repo_id),),
        )
        row = cur.fetchone()
        if not row:
            return None
        return {
            "id": str(row[0]),
            "org_id": str(row[1]),
            "name": row[2],
            "git_url": row[3],
            "default_branch": row[4],
            "last_analyzed_sha": row[5],
            "schedule": row[6],
            "active": row[7],
        }


def update_repo_last_analyzed(
    conn: Connection[Any],
    repo_id: UUID,
    sha: str,
) -> None:
    """Update a repository's last analyzed SHA and timestamp."""
    with conn.cursor() as cur:
        cur.execute(
            "UPDATE repositories"
            " SET last_analyzed_sha = %s, last_analyzed_at = now()"
            " WHERE id = %s",
            (sha, str(repo_id)),
        )
    conn.commit()


def get_org_delivery_preferences(
    conn: Connection[Any],
    org_id: UUID,
) -> dict[str, Any] | None:
    """Get delivery preferences for an organization."""
    with conn.cursor() as cur:
        cur.execute(
            "SELECT email_address, slack_webhook_url, plan"
            " FROM organizations WHERE id = %s",
            (str(org_id),),
        )
        row = cur.fetchone()
        if not row:
            return None
        return {
            "email_address": row[0],
            "slack_webhook_url": row[1],
            "plan": row[2],
        }


def get_report_by_run_id(
    conn: Connection[Any],
    run_id: UUID,
) -> dict[str, Any] | None:
    """Get a report by its analysis run ID."""
    with conn.cursor() as cur:
        cur.execute(
            "SELECT id, summary, risk_score, markdown"
            " FROM reports WHERE run_id = %s",
            (str(run_id),),
        )
        row = cur.fetchone()
        if not row:
            return None
        return {
            "id": str(row[0]),
            "summary": row[1],
            "risk_score": row[2],
            "markdown": row[3],
        }


def update_report_delivery(
    conn: Connection[Any],
    run_id: UUID,
    delivery_channel: str,
) -> None:
    """Update report delivery status."""
    with conn.cursor() as cur:
        cur.execute(
            "UPDATE reports"
            " SET delivered_at = now(), delivery_channel = %s"
            " WHERE run_id = %s",
            (delivery_channel, str(run_id)),
        )
    conn.commit()
