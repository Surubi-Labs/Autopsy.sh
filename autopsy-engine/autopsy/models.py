"""Extraction data models — the contract between Python engine and Kotlin API."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


@dataclass(frozen=True)
class FileChange:
    """A single file changed in a commit."""

    path: str
    additions: int
    deletions: int

    def to_dict(self) -> dict[str, Any]:
        return {"path": self.path, "additions": self.additions, "deletions": self.deletions}

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> FileChange:
        return cls(path=data["path"], additions=data["additions"], deletions=data["deletions"])


@dataclass(frozen=True)
class CommitExtraction:
    """Extracted commit metadata."""

    sha: str
    author_email: str
    timestamp: str
    message: str
    files_changed: list[FileChange]

    def to_dict(self) -> dict[str, Any]:
        return {
            "sha": self.sha,
            "author_email": self.author_email,
            "timestamp": self.timestamp,
            "message": self.message,
            "files_changed": [fc.to_dict() for fc in self.files_changed],
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> CommitExtraction:
        return cls(
            sha=data["sha"],
            author_email=data["author_email"],
            timestamp=data["timestamp"],
            message=data["message"],
            files_changed=[FileChange.from_dict(fc) for fc in data["files_changed"]],
        )


@dataclass(frozen=True)
class DependencyInfo:
    """A single dependency with version info."""

    name: str
    current: str
    latest: str | None = None
    outdated_days: int | None = None
    cves: list[str] = field(default_factory=list)

    def to_dict(self) -> dict[str, Any]:
        return {
            "name": self.name,
            "current": self.current,
            "latest": self.latest,
            "outdated_days": self.outdated_days,
            "cves": list(self.cves),
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> DependencyInfo:
        return cls(
            name=data["name"],
            current=data["current"],
            latest=data.get("latest"),
            outdated_days=data.get("outdated_days"),
            cves=data.get("cves", []),
        )


@dataclass(frozen=True)
class DependencyExtraction:
    """Dependencies extracted from a single manifest file."""

    file: str
    manager: str
    dependencies: list[DependencyInfo]

    def to_dict(self) -> dict[str, Any]:
        return {
            "file": self.file,
            "manager": self.manager,
            "dependencies": [dep.to_dict() for dep in self.dependencies],
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> DependencyExtraction:
        return cls(
            file=data["file"],
            manager=data["manager"],
            dependencies=[DependencyInfo.from_dict(d) for d in data["dependencies"]],
        )


@dataclass(frozen=True)
class DocExtraction:
    """A detected documentation file."""

    type: str
    path: str
    title: str
    content: str
    last_modified: str

    def to_dict(self) -> dict[str, Any]:
        return {
            "type": self.type,
            "path": self.path,
            "title": self.title,
            "content": self.content,
            "last_modified": self.last_modified,
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> DocExtraction:
        return cls(
            type=data["type"],
            path=data["path"],
            title=data["title"],
            content=data["content"],
            last_modified=data["last_modified"],
        )


@dataclass(frozen=True)
class ExtractionResult:
    """Complete extraction result — the full contract from Python to Kotlin."""

    commits: list[CommitExtraction]
    dependencies: list[DependencyExtraction]
    docs: list[DocExtraction]
    test_files: list[str]
    source_files: list[str]

    def to_dict(self) -> dict[str, Any]:
        return {
            "commits": [c.to_dict() for c in self.commits],
            "dependencies": [d.to_dict() for d in self.dependencies],
            "docs": [d.to_dict() for d in self.docs],
            "test_files": list(self.test_files),
            "source_files": list(self.source_files),
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> ExtractionResult:
        return cls(
            commits=[CommitExtraction.from_dict(c) for c in data["commits"]],
            dependencies=[DependencyExtraction.from_dict(d) for d in data["dependencies"]],
            docs=[DocExtraction.from_dict(d) for d in data["docs"]],
            test_files=data["test_files"],
            source_files=data["source_files"],
        )
