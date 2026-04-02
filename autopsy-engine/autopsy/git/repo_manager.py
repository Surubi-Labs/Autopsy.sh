"""Bare repo and worktree management for persistent git storage."""

from __future__ import annotations

import contextlib
import shutil
import subprocess
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from pathlib import Path


def clone_or_fetch(git_url: str, bare_path: Path) -> str:
    """Clone a bare repo or fetch updates if it already exists.

    Returns the HEAD SHA after clone/fetch.
    """
    if bare_path.exists():
        _run_git_bare(bare_path, "fetch", "origin", "--prune")
        # Update local HEAD to match origin's default branch
        _update_head_after_fetch(bare_path)
    else:
        bare_path.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(
            ["git", "clone", "--bare", git_url, str(bare_path)],
            capture_output=True,
            text=True,
            check=True,
        )
    return resolve_head(bare_path)


def create_worktree(bare_path: Path, worktree_path: Path, ref: str = "HEAD") -> Path:
    """Create a temporary worktree from a bare repo.

    Returns the worktree path.
    """
    worktree_path.parent.mkdir(parents=True, exist_ok=True)
    _run_git_bare(bare_path, "worktree", "add", str(worktree_path), ref, "--detach")
    return worktree_path


def remove_worktree(bare_path: Path, worktree_path: Path) -> None:
    """Remove a worktree. Falls back to shutil.rmtree if git fails."""
    try:
        _run_git_bare(bare_path, "worktree", "remove", str(worktree_path), "--force")
    except (subprocess.CalledProcessError, FileNotFoundError):
        if worktree_path.exists():
            shutil.rmtree(worktree_path, ignore_errors=True)
    # Prune stale worktree references
    with contextlib.suppress(subprocess.CalledProcessError):
        _run_git_bare(bare_path, "worktree", "prune")


def resolve_head(bare_path: Path, branch: str = "HEAD") -> str:
    """Resolve a ref to its SHA in a bare repo.

    For fetched bare repos, HEAD may be stale. Use the default branch
    or origin refs for the latest.
    """
    # Try the requested ref first
    try:
        result = _run_git_bare(bare_path, "rev-parse", branch)
        return result.strip()
    except subprocess.CalledProcessError:
        pass

    # Fall back to origin/HEAD or origin/main
    for ref in ("origin/HEAD", "origin/main", "origin/master"):
        try:
            result = _run_git_bare(bare_path, "rev-parse", ref)
            return result.strip()
        except subprocess.CalledProcessError:
            continue

    msg = f"Could not resolve HEAD for bare repo: {bare_path}"
    raise RuntimeError(msg)


def get_commit_range(bare_path: Path, since_sha: str | None) -> str:
    """Build a commit range string for incremental analysis.

    Returns 'since_sha..HEAD' if since_sha is provided, empty string otherwise.
    """
    if not since_sha:
        return ""
    head = resolve_head(bare_path)
    return f"{since_sha}..{head}"


def repo_path_for(base_dir: Path, org_id: str, repo_id: str) -> Path:
    """Compute the bare repo path for an org/repo pair."""
    return base_dir / org_id / f"{repo_id}.git"


def worktree_path_for(base_dir: Path, org_id: str, run_id: str) -> Path:
    """Compute the worktree path for an analysis run."""
    return base_dir / org_id / "worktrees" / run_id


def _update_head_after_fetch(bare_path: Path) -> None:
    """Update the local default branch to match origin after fetch."""
    try:
        symbolic = _run_git_bare(bare_path, "symbolic-ref", "HEAD").strip()
        branch_name = symbolic.replace("refs/heads/", "")

        # Try origin remote ref first (works for URL-based clones)
        for ref in (f"refs/remotes/origin/{branch_name}", "FETCH_HEAD"):
            try:
                sha = _run_git_bare(bare_path, "rev-parse", ref).strip()
                _run_git_bare(bare_path, "update-ref", symbolic, sha)
                return
            except subprocess.CalledProcessError:
                continue
    except subprocess.CalledProcessError:
        pass


def _run_git_bare(bare_path: Path, *args: str) -> str:
    """Run a git command against a bare repo."""
    result = subprocess.run(
        ["git", "--git-dir", str(bare_path), *args],
        capture_output=True,
        text=True,
        check=True,
    )
    return result.stdout
