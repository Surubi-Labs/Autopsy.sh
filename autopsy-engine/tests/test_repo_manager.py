"""Tests for bare repo and worktree management."""

from __future__ import annotations

import subprocess
from typing import TYPE_CHECKING

from autopsy.git.repo_manager import (
    clone_or_fetch,
    create_worktree,
    get_commit_range,
    remove_worktree,
    repo_path_for,
    resolve_head,
    worktree_path_for,
)

if TYPE_CHECKING:
    from pathlib import Path


def _create_source_repo(path: Path) -> Path:
    """Create a non-bare source repo to clone from."""
    subprocess.run(["git", "init", str(path)], capture_output=True, check=True)
    subprocess.run(
        ["git", "config", "user.email", "test@test.com"], cwd=path, capture_output=True, check=True
    )
    subprocess.run(
        ["git", "config", "user.name", "Test"], cwd=path, capture_output=True, check=True
    )
    (path / "file.txt").write_text("hello")
    subprocess.run(["git", "add", "."], cwd=path, capture_output=True, check=True)
    subprocess.run(["git", "commit", "-m", "init"], cwd=path, capture_output=True, check=True)
    return path


class TestCloneOrFetch:
    def test_clones_bare_repo(self, tmp_path: Path) -> None:
        source = _create_source_repo(tmp_path / "source")
        bare = tmp_path / "bare.git"

        sha = clone_or_fetch(str(source), bare)

        assert bare.exists()
        assert (bare / "HEAD").exists()  # bare repo marker
        assert len(sha) == 40

    def test_fetch_updates_existing(self, tmp_path: Path) -> None:
        source = _create_source_repo(tmp_path / "source")
        bare = tmp_path / "bare.git"

        sha1 = clone_or_fetch(str(source), bare)

        # Add a new commit to source
        (source / "file2.txt").write_text("world")
        subprocess.run(["git", "add", "."], cwd=source, capture_output=True, check=True)
        subprocess.run(
            ["git", "commit", "-m", "second"],
            cwd=source, capture_output=True, check=True,
        )

        sha2 = clone_or_fetch(str(source), bare)
        assert sha2 != sha1
        assert len(sha2) == 40

    def test_idempotent_fetch(self, tmp_path: Path) -> None:
        source = _create_source_repo(tmp_path / "source")
        bare = tmp_path / "bare.git"

        sha1 = clone_or_fetch(str(source), bare)
        sha2 = clone_or_fetch(str(source), bare)
        assert sha1 == sha2


class TestWorktree:
    def test_create_and_remove_worktree(self, tmp_path: Path) -> None:
        source = _create_source_repo(tmp_path / "source")
        bare = tmp_path / "bare.git"
        clone_or_fetch(str(source), bare)

        wt = tmp_path / "worktree"
        create_worktree(bare, wt)

        assert wt.exists()
        assert (wt / "file.txt").exists()

        remove_worktree(bare, wt)
        assert not wt.exists()

    def test_remove_nonexistent_worktree_is_safe(self, tmp_path: Path) -> None:
        source = _create_source_repo(tmp_path / "source")
        bare = tmp_path / "bare.git"
        clone_or_fetch(str(source), bare)

        wt = tmp_path / "nonexistent"
        remove_worktree(bare, wt)  # Should not raise


class TestCommitRange:
    def test_returns_empty_without_since_sha(self, tmp_path: Path) -> None:
        source = _create_source_repo(tmp_path / "source")
        bare = tmp_path / "bare.git"
        clone_or_fetch(str(source), bare)

        result = get_commit_range(bare, None)
        assert result == ""

    def test_returns_range_with_since_sha(self, tmp_path: Path) -> None:
        source = _create_source_repo(tmp_path / "source")
        bare = tmp_path / "bare.git"
        sha1 = clone_or_fetch(str(source), bare)

        result = get_commit_range(bare, sha1)
        head = resolve_head(bare)
        assert result == f"{sha1}..{head}"


class TestPathHelpers:
    def test_repo_path_for(self, tmp_path: Path) -> None:
        result = repo_path_for(tmp_path, "org-1", "repo-1")
        assert result == tmp_path / "org-1" / "repo-1.git"

    def test_worktree_path_for(self, tmp_path: Path) -> None:
        result = worktree_path_for(tmp_path, "org-1", "run-1")
        assert result == tmp_path / "org-1" / "worktrees" / "run-1"
