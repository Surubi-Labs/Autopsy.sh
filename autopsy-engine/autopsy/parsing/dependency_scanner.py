"""Dependency scanner — walks a repo and dispatches to the correct parser."""

from __future__ import annotations

import logging
from pathlib import Path
from typing import TYPE_CHECKING

from autopsy.parsing.dependencies import (
    parse_gradle,
    parse_package_json,
    parse_pyproject_toml,
    parse_requirements_txt,
)

if TYPE_CHECKING:
    from autopsy.models import DependencyExtraction

logger = logging.getLogger(__name__)

_SKIP_DIRS = {"node_modules", ".git", "__pycache__", ".venv", "venv", "build", "dist", ".gradle"}

_PARSERS: dict[str, type[object] | object] = {
    "package.json": parse_package_json,
    "requirements.txt": parse_requirements_txt,
    "pyproject.toml": parse_pyproject_toml,
    "build.gradle": parse_gradle,
    "build.gradle.kts": parse_gradle,
}


def scan_dependencies(repo_path: str) -> list[DependencyExtraction]:
    """Walk a repository and extract dependencies from all recognized files.

    Parser errors for individual files are logged and skipped.
    """
    root = Path(repo_path)
    results: list[DependencyExtraction] = []

    for path in _walk_files(root):
        parser = _PARSERS.get(path.name)
        if parser is None:
            continue
        try:
            result = parser(str(path))  # type: ignore[operator]
            results.append(result)
        except Exception:
            logger.warning("Failed to parse %s, skipping", path)

    return results


def _walk_files(root: Path) -> list[Path]:
    """Walk directory tree, skipping ignored directories."""
    files: list[Path] = []
    if not root.is_dir():
        return files

    for item in sorted(root.iterdir()):
        if item.is_dir():
            if item.name in _SKIP_DIRS:
                continue
            files.extend(_walk_files(item))
        elif item.is_file():
            files.append(item)

    return files
