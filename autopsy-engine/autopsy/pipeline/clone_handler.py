"""Pipeline stage: clone or fetch a git repository."""

from __future__ import annotations

import logging
from pathlib import Path
from typing import Any
from uuid import UUID

from autopsy.db.repositories import get_repository, update_run_status
from autopsy.git.repo_manager import clone_or_fetch, get_commit_range, repo_path_for
from autopsy.queue.producer import enqueue_next_stage

logger = logging.getLogger(__name__)


def handle_clone(
    conn: Any,
    redis_client: Any,
    run_id: str,
    repo_id: str,
    org_id: str,
    data_dir: str,
) -> None:
    """Clone or fetch the repository, then enqueue the EXTRACT stage."""
    update_run_status(conn, UUID(run_id), "running", "clone")

    repo = get_repository(conn, UUID(repo_id))
    if not repo:
        update_run_status(conn, UUID(run_id), "failed", "clone", f"Repository {repo_id} not found")
        return

    bare_path = repo_path_for(Path(data_dir), org_id, repo_id)

    try:
        head_sha = clone_or_fetch(repo["git_url"], bare_path)
        commit_range = get_commit_range(bare_path, repo.get("last_analyzed_sha"))

        logger.info("Cloned/fetched %s, HEAD=%s, range=%s", repo["git_url"], head_sha, commit_range)

        enqueue_next_stage(
            redis_client, run_id, repo_id, org_id, "CLONE",
            payload={"headSha": head_sha, "commitRange": commit_range},
        )
    except Exception as e:
        update_run_status(conn, UUID(run_id), "failed", "clone", str(e))
        raise
