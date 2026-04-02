"""Fixtures for creating temporary git repositories for testing."""

from __future__ import annotations

import subprocess
from typing import TYPE_CHECKING

import pytest

if TYPE_CHECKING:
    from pathlib import Path


def _run_git(repo_path: Path, *args: str) -> str:
    """Run a git command in the given repo.

    Uses git config (not env vars) so per-commit author overrides work.
    """
    result = subprocess.run(
        ["git", *args],
        cwd=repo_path,
        capture_output=True,
        text=True,
        check=True,
    )
    return result.stdout


@pytest.fixture()
def empty_git_repo(tmp_path: Path) -> Path:
    """Create an empty git repo (initialized but no commits)."""
    _run_git(tmp_path, "init")
    _run_git(tmp_path, "config", "user.email", "test@example.com")
    _run_git(tmp_path, "config", "user.name", "Test Author")
    return tmp_path


@pytest.fixture()
def simple_git_repo(tmp_path: Path) -> Path:
    """Create a git repo with a few commits across different files."""
    _run_git(tmp_path, "init")
    _run_git(tmp_path, "config", "user.email", "dev@company.com")
    _run_git(tmp_path, "config", "user.name", "Dev User")

    # Commit 1: add a source file
    src_dir = tmp_path / "src" / "payments"
    src_dir.mkdir(parents=True)
    handler_content = "def pay():\n    pass\n"
    (src_dir / "handler.py").write_text(handler_content)
    _run_git(tmp_path, "add", ".")
    _run_git(tmp_path, "commit", "-m", "feat: add payment handler")

    # Commit 2: add a test file (different author)
    _run_git(tmp_path, "config", "user.email", "qa@company.com")
    _run_git(tmp_path, "config", "user.name", "QA User")
    test_dir = tmp_path / "tests"
    test_dir.mkdir()
    (test_dir / "test_handler.py").write_text("def test_pay():\n    assert True\n")
    _run_git(tmp_path, "add", ".")
    _run_git(tmp_path, "commit", "-m", "test: add payment handler tests")

    # Commit 3: modify source file
    _run_git(tmp_path, "config", "user.email", "dev@company.com")
    _run_git(tmp_path, "config", "user.name", "Dev User")
    updated_handler = (
        "def pay(amount: int):\n    return amount\n\ndef refund():\n    pass\n"
    )
    (src_dir / "handler.py").write_text(updated_handler)
    _run_git(tmp_path, "add", ".")
    _run_git(tmp_path, "commit", "-m", "feat: add refund and update pay signature")

    return tmp_path


@pytest.fixture()
def merge_commit_repo(tmp_path: Path) -> Path:
    """Create a git repo with a merge commit."""
    _run_git(tmp_path, "init")
    _run_git(tmp_path, "config", "user.email", "dev@company.com")
    _run_git(tmp_path, "config", "user.name", "Dev User")

    # Initial commit on main
    (tmp_path / "main.py").write_text("# main\n")
    _run_git(tmp_path, "add", ".")
    _run_git(tmp_path, "commit", "-m", "initial commit")

    # Create a branch and add a commit
    _run_git(tmp_path, "checkout", "-b", "feature")
    (tmp_path / "feature.py").write_text("# feature\n")
    _run_git(tmp_path, "add", ".")
    _run_git(tmp_path, "commit", "-m", "feat: add feature")

    # Go back to main, add another commit
    _run_git(tmp_path, "checkout", "master")
    (tmp_path / "main.py").write_text("# main updated\n")
    _run_git(tmp_path, "add", ".")
    _run_git(tmp_path, "commit", "-m", "update main")

    # Merge feature into main
    _run_git(tmp_path, "merge", "feature", "--no-ff", "-m", "Merge feature branch")

    return tmp_path
