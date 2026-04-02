"""Document detector — finds ADRs, READMEs, and CHANGELOGs in a repo."""

from __future__ import annotations

import subprocess
from pathlib import Path

from autopsy.models import DocExtraction

_ADR_DIR_NAMES = {"adr", "adrs"}
_SKIP_DIRS = {"node_modules", ".git", "__pycache__", ".venv", "venv", "build", "dist", ".gradle"}


def detect_docs(repo_path: str) -> list[DocExtraction]:
    """Detect documentation files in a repository.

    Returns:
        List of DocExtraction objects for ADRs, READMEs, and CHANGELOGs.
    """
    root = Path(repo_path)
    docs: list[DocExtraction] = []

    for path in _walk_files(root):
        doc = _classify_doc(path, root, repo_path)
        if doc is not None:
            docs.append(doc)

    return docs


def _classify_doc(path: Path, root: Path, repo_path: str) -> DocExtraction | None:
    """Classify a file as a doc type, or return None if not a doc."""
    if not path.name.endswith(".md"):
        return None

    rel = str(path.relative_to(root))

    # Check if it's an ADR
    if _is_adr(path):
        return _make_doc("adr", rel, path, repo_path)

    # Check if it's a README
    if path.name.upper().startswith("README"):
        return _make_doc("readme", rel, path, repo_path)

    # Check if it's a CHANGELOG
    if path.name.upper().startswith("CHANGELOG"):
        return _make_doc("changelog", rel, path, repo_path)

    return None


def _is_adr(path: Path) -> bool:
    """Check if a file is in an ADR directory."""
    return any(part.lower() in _ADR_DIR_NAMES for part in path.parts)


def _make_doc(doc_type: str, rel_path: str, path: Path, repo_path: str) -> DocExtraction:
    """Create a DocExtraction from a file."""
    content = path.read_text(errors="replace")
    title = _extract_title(content)
    last_modified = _get_last_modified(repo_path, rel_path)

    return DocExtraction(
        type=doc_type,
        path=rel_path,
        title=title,
        content=content,
        last_modified=last_modified,
    )


def _extract_title(content: str) -> str:
    """Extract the first markdown heading as the title."""
    for line in content.splitlines():
        stripped = line.strip()
        if stripped.startswith("# "):
            return stripped[2:].strip()
    return ""


def _get_last_modified(repo_path: str, rel_path: str) -> str:
    """Get the last git modification date for a file."""
    result = subprocess.run(
        ["git", "-C", repo_path, "log", "-1", "--format=%aI", "--", rel_path],
        capture_output=True,
        text=True,
        check=False,
    )
    return result.stdout.strip()


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
