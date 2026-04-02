"""Tests for git log extractor."""

from __future__ import annotations

from typing import TYPE_CHECKING

from autopsy.git.extractor import extract_commits

if TYPE_CHECKING:
    from pathlib import Path


class TestExtractCommits:
    def test_simple_repo_commit_count(self, simple_git_repo: Path) -> None:
        commits = extract_commits(str(simple_git_repo), since_days=365)
        assert len(commits) == 3

    def test_simple_repo_first_commit(self, simple_git_repo: Path) -> None:
        commits = extract_commits(str(simple_git_repo), since_days=365)
        # Commits are in reverse chronological order (newest first)
        latest = commits[0]
        assert latest.message == "feat: add refund and update pay signature"
        assert latest.author_email == "dev@company.com"
        assert len(latest.sha) == 40

    def test_simple_repo_file_changes(self, simple_git_repo: Path) -> None:
        commits = extract_commits(str(simple_git_repo), since_days=365)
        latest = commits[0]
        assert len(latest.files_changed) == 1
        fc = latest.files_changed[0]
        assert fc.path == "src/payments/handler.py"
        assert fc.additions > 0

    def test_different_authors(self, simple_git_repo: Path) -> None:
        commits = extract_commits(str(simple_git_repo), since_days=365)
        emails = {c.author_email for c in commits}
        assert "dev@company.com" in emails
        assert "qa@company.com" in emails

    def test_timestamp_is_iso_format(self, simple_git_repo: Path) -> None:
        commits = extract_commits(str(simple_git_repo), since_days=365)
        for commit in commits:
            assert "T" in commit.timestamp

    def test_empty_repo_returns_empty_list(self, empty_git_repo: Path) -> None:
        commits = extract_commits(str(empty_git_repo), since_days=365)
        assert commits == []

    def test_merge_commit_included(self, merge_commit_repo: Path) -> None:
        commits = extract_commits(str(merge_commit_repo), since_days=365)
        messages = [c.message for c in commits]
        assert any("Merge" in m for m in messages)

    def test_merge_commit_may_have_no_files(self, merge_commit_repo: Path) -> None:
        commits = extract_commits(str(merge_commit_repo), since_days=365)
        merge_commits = [c for c in commits if "Merge" in c.message]
        assert len(merge_commits) >= 1

    def test_nonexistent_path_raises(self) -> None:
        try:
            extract_commits("/nonexistent/path", since_days=365)
            raise AssertionError("Should have raised")
        except (FileNotFoundError, RuntimeError):
            pass

    def test_not_a_git_repo_raises(self, tmp_path: Path) -> None:
        try:
            extract_commits(str(tmp_path), since_days=365)
            raise AssertionError("Should have raised")
        except RuntimeError:
            pass
