"""Test file detector — classifies files as test or source by path conventions."""

from __future__ import annotations

import re
from pathlib import Path

_CODE_EXTENSIONS = {
    ".py", ".kt", ".kts", ".java", ".js", ".jsx", ".ts", ".tsx",
    ".go", ".rs", ".rb", ".swift", ".c", ".cpp", ".h", ".hpp",
    ".cs", ".scala", ".clj", ".ex", ".exs", ".lua", ".php",
}

_TEST_DIR_NAMES = {"test", "tests", "__tests__", "spec", "specs"}

_TEST_FILE_PATTERNS = [
    re.compile(r"^test_"),
    re.compile(r"_test\.\w+$"),
    re.compile(r"_spec\.\w+$"),
    re.compile(r"\.test\.\w+$"),
    re.compile(r"\.spec\.\w+$"),
    re.compile(r"Test\.\w+$"),
    re.compile(r"Spec\.\w+$"),
]

_SKIP_DIRS = {"node_modules", ".git", "__pycache__", ".venv", "venv", "build", "dist", ".gradle"}


def detect_test_files(repo_path: str) -> tuple[list[str], list[str]]:
    """Classify files in a repo as test or source files.

    Returns:
        A tuple of (test_files, source_files), each a list of relative paths.
    """
    root = Path(repo_path)
    test_files: list[str] = []
    source_files: list[str] = []

    for path in _walk_code_files(root):
        rel = str(path.relative_to(root))
        if _is_test_file(path, root):
            test_files.append(rel)
        else:
            source_files.append(rel)

    return sorted(test_files), sorted(source_files)


def _is_test_file(path: Path, root: Path) -> bool:
    """Determine if a file is a test file by path conventions."""
    rel_parts = path.relative_to(root).parts

    # Check if any parent directory is a test directory
    for part in rel_parts[:-1]:
        if part.lower() in _TEST_DIR_NAMES:
            return True

    # Check filename patterns
    filename = path.name
    return any(pattern.search(filename) for pattern in _TEST_FILE_PATTERNS)


def _walk_code_files(root: Path) -> list[Path]:
    """Walk directory tree and return code files only."""
    files: list[Path] = []
    if not root.is_dir():
        return files

    for item in sorted(root.iterdir()):
        if item.is_dir():
            if item.name in _SKIP_DIRS:
                continue
            files.extend(_walk_code_files(item))
        elif item.is_file() and item.suffix in _CODE_EXTENSIONS:
            files.append(item)

    return files
