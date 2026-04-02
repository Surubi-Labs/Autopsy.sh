"""Tests for document detector."""

from __future__ import annotations

import subprocess
from typing import TYPE_CHECKING

from autopsy.parsing.docs import detect_docs

if TYPE_CHECKING:
    from pathlib import Path


def _init_git_repo(path: Path) -> None:
    """Initialize a git repo and commit all files."""
    subprocess.run(["git", "init"], cwd=path, capture_output=True, check=True)
    subprocess.run(
        ["git", "config", "user.email", "test@test.com"], cwd=path, capture_output=True, check=True
    )
    subprocess.run(
        ["git", "config", "user.name", "Test"], cwd=path, capture_output=True, check=True
    )
    subprocess.run(["git", "add", "."], cwd=path, capture_output=True, check=True)
    subprocess.run(
        ["git", "commit", "-m", "init"], cwd=path, capture_output=True, check=True
    )


class TestDetectDocs:
    def test_detects_readme(self, tmp_path: Path) -> None:
        (tmp_path / "README.md").write_text("# My Project\nSome description")
        _init_git_repo(tmp_path)
        docs = detect_docs(str(tmp_path))
        assert len(docs) == 1
        assert docs[0].type == "readme"
        assert docs[0].title == "My Project"

    def test_detects_adr(self, tmp_path: Path) -> None:
        adr_dir = tmp_path / "docs" / "adr"
        adr_dir.mkdir(parents=True)
        (adr_dir / "001-use-postgres.md").write_text("# Use PostgreSQL\n## Context\n...")
        _init_git_repo(tmp_path)
        docs = detect_docs(str(tmp_path))
        assert any(d.type == "adr" for d in docs)
        adr = next(d for d in docs if d.type == "adr")
        assert adr.title == "Use PostgreSQL"

    def test_detects_changelog(self, tmp_path: Path) -> None:
        (tmp_path / "CHANGELOG.md").write_text("# Changelog\n## 1.0.0\nInitial release")
        _init_git_repo(tmp_path)
        docs = detect_docs(str(tmp_path))
        assert any(d.type == "changelog" for d in docs)

    def test_detects_nested_readme(self, tmp_path: Path) -> None:
        sub = tmp_path / "packages" / "core"
        sub.mkdir(parents=True)
        (sub / "README.md").write_text("# Core Package\nCore stuff")
        _init_git_repo(tmp_path)
        docs = detect_docs(str(tmp_path))
        assert any(d.type == "readme" for d in docs)

    def test_empty_repo_no_docs(self, tmp_path: Path) -> None:
        (tmp_path / "src").mkdir()
        (tmp_path / "src" / "main.py").write_text("pass")
        _init_git_repo(tmp_path)
        docs = detect_docs(str(tmp_path))
        assert docs == []

    def test_skips_non_markdown_in_adr_dir(self, tmp_path: Path) -> None:
        adr_dir = tmp_path / "docs" / "adr"
        adr_dir.mkdir(parents=True)
        (adr_dir / "template.txt").write_text("not markdown")
        (adr_dir / "001-use-postgres.md").write_text("# Use PostgreSQL\n...")
        _init_git_repo(tmp_path)
        docs = detect_docs(str(tmp_path))
        assert len(docs) == 1

    def test_last_modified_is_set(self, tmp_path: Path) -> None:
        (tmp_path / "README.md").write_text("# Project")
        _init_git_repo(tmp_path)
        docs = detect_docs(str(tmp_path))
        assert docs[0].last_modified != ""
        assert "T" in docs[0].last_modified
