"""Tests for extraction data models."""

from __future__ import annotations

import json

from autopsy.models import (
    CommitExtraction,
    DependencyExtraction,
    DependencyInfo,
    DocExtraction,
    ExtractionResult,
    FileChange,
)


class TestFileChange:
    def test_creation(self) -> None:
        fc = FileChange(path="src/main.py", additions=10, deletions=5)
        assert fc.path == "src/main.py"
        assert fc.additions == 10
        assert fc.deletions == 5

    def test_immutability(self) -> None:
        fc = FileChange(path="src/main.py", additions=10, deletions=5)
        try:
            fc.path = "other.py"  # type: ignore[misc]
            raise AssertionError("Should not allow mutation")
        except AttributeError:
            pass

    def test_to_dict(self) -> None:
        fc = FileChange(path="src/main.py", additions=10, deletions=5)
        d = fc.to_dict()
        assert d == {"path": "src/main.py", "additions": 10, "deletions": 5}


class TestCommitExtraction:
    def test_creation(self) -> None:
        commit = CommitExtraction(
            sha="abc123",
            author_email="dev@company.com",
            timestamp="2026-03-15T14:30:00Z",
            message="refactor payment handler",
            files_changed=[
                FileChange(path="src/payments/handler.py", additions=45, deletions=12),
            ],
        )
        assert commit.sha == "abc123"
        assert commit.author_email == "dev@company.com"
        assert len(commit.files_changed) == 1

    def test_to_dict(self) -> None:
        commit = CommitExtraction(
            sha="abc123",
            author_email="dev@company.com",
            timestamp="2026-03-15T14:30:00Z",
            message="fix bug",
            files_changed=[
                FileChange(path="src/main.py", additions=1, deletions=0),
            ],
        )
        d = commit.to_dict()
        assert d["sha"] == "abc123"
        assert d["author_email"] == "dev@company.com"
        assert d["timestamp"] == "2026-03-15T14:30:00Z"
        assert d["message"] == "fix bug"
        assert len(d["files_changed"]) == 1
        assert d["files_changed"][0]["path"] == "src/main.py"

    def test_empty_files_changed(self) -> None:
        commit = CommitExtraction(
            sha="abc123",
            author_email="dev@company.com",
            timestamp="2026-03-15T14:30:00Z",
            message="empty merge",
            files_changed=[],
        )
        assert commit.files_changed == []

    def test_json_roundtrip(self) -> None:
        commit = CommitExtraction(
            sha="abc123",
            author_email="dev@company.com",
            timestamp="2026-03-15T14:30:00Z",
            message="fix bug",
            files_changed=[
                FileChange(path="src/main.py", additions=1, deletions=0),
            ],
        )
        json_str = json.dumps(commit.to_dict())
        parsed = json.loads(json_str)
        restored = CommitExtraction.from_dict(parsed)
        assert restored == commit


class TestDependencyInfo:
    def test_creation_with_all_fields(self) -> None:
        dep = DependencyInfo(
            name="express",
            current="4.18.2",
            latest="4.19.1",
            outdated_days=142,
            cves=["CVE-2021-23337"],
        )
        assert dep.name == "express"
        assert dep.latest == "4.19.1"
        assert dep.outdated_days == 142
        assert dep.cves == ["CVE-2021-23337"]

    def test_creation_with_optional_fields(self) -> None:
        dep = DependencyInfo(name="lodash", current="4.17.21")
        assert dep.latest is None
        assert dep.outdated_days is None
        assert dep.cves == []

    def test_to_dict(self) -> None:
        dep = DependencyInfo(
            name="express",
            current="4.18.2",
            latest="4.19.1",
            outdated_days=142,
            cves=["CVE-2021-23337"],
        )
        d = dep.to_dict()
        assert d["name"] == "express"
        assert d["cves"] == ["CVE-2021-23337"]


class TestDependencyExtraction:
    def test_creation(self) -> None:
        ext = DependencyExtraction(
            file="package.json",
            manager="npm",
            dependencies=[
                DependencyInfo(name="express", current="4.18.2"),
            ],
        )
        assert ext.file == "package.json"
        assert ext.manager == "npm"
        assert len(ext.dependencies) == 1

    def test_to_dict(self) -> None:
        ext = DependencyExtraction(
            file="package.json",
            manager="npm",
            dependencies=[
                DependencyInfo(name="express", current="4.18.2"),
            ],
        )
        d = ext.to_dict()
        assert d["file"] == "package.json"
        assert d["dependencies"][0]["name"] == "express"

    def test_json_roundtrip(self) -> None:
        ext = DependencyExtraction(
            file="requirements.txt",
            manager="pip",
            dependencies=[
                DependencyInfo(name="flask", current="2.0.0", latest="3.0.0", outdated_days=365),
            ],
        )
        json_str = json.dumps(ext.to_dict())
        parsed = json.loads(json_str)
        restored = DependencyExtraction.from_dict(parsed)
        assert restored == ext


class TestDocExtraction:
    def test_creation(self) -> None:
        doc = DocExtraction(
            type="adr",
            path="docs/adr/001.md",
            title="Use PostgreSQL",
            content="## Context\nWe need a database...",
            last_modified="2025-08-12T10:00:00Z",
        )
        assert doc.type == "adr"
        assert doc.title == "Use PostgreSQL"

    def test_to_dict(self) -> None:
        doc = DocExtraction(
            type="readme",
            path="README.md",
            title="Project README",
            content="# My Project",
            last_modified="2026-01-01T00:00:00Z",
        )
        d = doc.to_dict()
        assert d["type"] == "readme"
        assert d["path"] == "README.md"

    def test_json_roundtrip(self) -> None:
        doc = DocExtraction(
            type="changelog",
            path="CHANGELOG.md",
            title="Changelog",
            content="# Changelog\n## 1.0.0",
            last_modified="2026-01-01T00:00:00Z",
        )
        json_str = json.dumps(doc.to_dict())
        parsed = json.loads(json_str)
        restored = DocExtraction.from_dict(parsed)
        assert restored == doc


class TestExtractionResult:
    def test_creation(self) -> None:
        result = ExtractionResult(
            commits=[
                CommitExtraction(
                    sha="abc",
                    author_email="a@b.com",
                    timestamp="2026-01-01T00:00:00Z",
                    message="init",
                    files_changed=[],
                ),
            ],
            dependencies=[],
            docs=[],
            test_files=["tests/test_main.py"],
            source_files=["src/main.py"],
        )
        assert len(result.commits) == 1
        assert result.test_files == ["tests/test_main.py"]

    def test_to_dict(self) -> None:
        result = ExtractionResult(
            commits=[],
            dependencies=[],
            docs=[],
            test_files=[],
            source_files=[],
        )
        d = result.to_dict()
        assert d["commits"] == []
        assert d["test_files"] == []

    def test_full_json_roundtrip(self) -> None:
        result = ExtractionResult(
            commits=[
                CommitExtraction(
                    sha="abc123",
                    author_email="dev@co.com",
                    timestamp="2026-03-15T14:30:00Z",
                    message="feat: add payment handler",
                    files_changed=[
                        FileChange(path="src/payments/handler.py", additions=45, deletions=12),
                        FileChange(path="tests/test_handler.py", additions=20, deletions=5),
                    ],
                ),
            ],
            dependencies=[
                DependencyExtraction(
                    file="package.json",
                    manager="npm",
                    dependencies=[
                        DependencyInfo(
                            name="express",
                            current="4.18.2",
                            latest="4.19.1",
                            outdated_days=142,
                            cves=["CVE-2021-23337"],
                        ),
                    ],
                ),
            ],
            docs=[
                DocExtraction(
                    type="adr",
                    path="docs/adr/001.md",
                    title="Use PostgreSQL",
                    content="## Context\n...",
                    last_modified="2025-08-12T10:00:00Z",
                ),
            ],
            test_files=["tests/test_handler.py"],
            source_files=["src/payments/handler.py"],
        )
        json_str = json.dumps(result.to_dict())
        parsed = json.loads(json_str)
        restored = ExtractionResult.from_dict(parsed)
        assert restored == result
