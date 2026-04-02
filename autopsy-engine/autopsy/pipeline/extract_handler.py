"""Pipeline stage: extract git data from repository."""

from __future__ import annotations

import logging
from pathlib import Path
from typing import Any
from uuid import UUID

from autopsy.db.repositories import save_extractions, update_run_status
from autopsy.git.extractor import extract_commits
from autopsy.git.repo_manager import (
    create_worktree,
    remove_worktree,
    repo_path_for,
    worktree_path_for,
)
from autopsy.parsing.dependency_scanner import scan_dependencies
from autopsy.parsing.docs import detect_docs
from autopsy.parsing.test_detector import detect_test_files
from autopsy.queue.producer import enqueue_next_stage

logger = logging.getLogger(__name__)


def handle_extract(
    conn: Any,
    redis_client: Any,
    run_id: str,
    repo_id: str,
    org_id: str,
    data_dir: str,
    payload: dict[str, str] | None = None,
) -> None:
    """Extract git data, write to DB, enqueue METRICS stage."""
    update_run_status(conn, UUID(run_id), "running", "extract")

    bare_path = repo_path_for(Path(data_dir), org_id, repo_id)
    wt_path = worktree_path_for(Path(data_dir), org_id, run_id)

    try:
        # Create worktree for file-level access
        create_worktree(bare_path, wt_path)

        # Extract commits from bare repo (using git log, no worktree needed)
        commits = extract_commits(str(wt_path))

        # Extract from worktree (needs actual files)
        dependencies = scan_dependencies(str(wt_path))
        docs = detect_docs(str(wt_path))
        test_files, source_files = detect_test_files(str(wt_path))

        # Build extraction records
        extractions: list[dict[str, Any]] = []

        for commit in commits:
            extractions.append({
                "data_type": "commit",
                "module_path": None,
                "payload": commit.to_dict(),
            })

        for dep in dependencies:
            extractions.append({
                "data_type": "dependency",
                "module_path": None,
                "payload": dep.to_dict(),
            })

        for doc in docs:
            extractions.append({
                "data_type": "doc",
                "module_path": None,
                "payload": doc.to_dict(),
            })

        # Store test/source file lists as a single extraction
        extractions.append({
            "data_type": "file_diff",
            "module_path": None,
            "payload": {"test_files": test_files, "source_files": source_files},
        })

        save_extractions(conn, UUID(run_id), extractions)

        logger.info(
            "Extracted %d commits, %d deps, %d docs for run %s",
            len(commits), len(dependencies), len(docs), run_id,
        )

        enqueue_next_stage(redis_client, run_id, repo_id, org_id, "EXTRACT")

    finally:
        remove_worktree(bare_path, wt_path)
