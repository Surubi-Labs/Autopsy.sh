"""Redis Streams job producer."""

from __future__ import annotations

import json
from typing import Any

STREAM_KEY = "autopsy:jobs"

_STAGE_ORDER = ["CLONE", "EXTRACT", "METRICS", "EMBED", "REPORT", "DELIVER"]


def enqueue_next_stage(
    redis_client: Any,
    run_id: str,
    repo_id: str,
    org_id: str,
    current_stage: str,
    payload: dict[str, str] | None = None,
) -> str | None:
    """Enqueue the next pipeline stage after the current one completes.

    Returns the stream entry ID, or None if there is no next stage.
    """
    try:
        current_idx = _STAGE_ORDER.index(current_stage.upper())
    except ValueError:
        return None

    next_idx = current_idx + 1
    if next_idx >= len(_STAGE_ORDER):
        return None

    next_stage = _STAGE_ORDER[next_idx]
    fields: dict[str, str] = {
        "runId": run_id,
        "repoId": repo_id,
        "orgId": org_id,
        "stage": next_stage,
    }
    if payload:
        fields["payload"] = json.dumps(payload)

    entry_id: Any = redis_client.xadd(STREAM_KEY, fields)
    return entry_id.decode() if isinstance(entry_id, bytes) else str(entry_id)


def enqueue_stage(
    redis_client: Any,
    run_id: str,
    repo_id: str,
    org_id: str,
    stage: str,
    payload: dict[str, str] | None = None,
) -> str:
    """Enqueue a specific pipeline stage."""
    fields: dict[str, str] = {
        "runId": run_id,
        "repoId": repo_id,
        "orgId": org_id,
        "stage": stage.upper(),
    }
    if payload:
        fields["payload"] = json.dumps(payload)

    entry_id: Any = redis_client.xadd(STREAM_KEY, fields)
    return entry_id.decode() if isinstance(entry_id, bytes) else str(entry_id)
