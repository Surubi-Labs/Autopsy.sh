"""Git log extraction — parses commit history into structured data."""

from __future__ import annotations

import subprocess
from pathlib import Path

from autopsy.models import CommitExtraction, FileChange

_LOG_FORMAT = "%H|%ae|%aI|%s"
_SEPARATOR = "|"


def extract_commits(repo_path: str, since_days: int = 90) -> list[CommitExtraction]:
    """Extract commits from a git repository.

    Args:
        repo_path: Path to the git repository.
        since_days: How many days of history to extract.

    Returns:
        List of CommitExtraction objects, newest first.

    Raises:
        FileNotFoundError: If repo_path does not exist.
        RuntimeError: If the path is not a git repo or git command fails.
    """
    path = Path(repo_path)
    if not path.exists():
        msg = f"Repository path does not exist: {repo_path}"
        raise FileNotFoundError(msg)

    if not (path / ".git").exists():
        msg = f"Not a git repository: {repo_path}"
        raise RuntimeError(msg)

    raw = _run_git_log(repo_path, since_days)
    if not raw.strip():
        return []

    return _parse_log_output(raw)


def _run_git_log(repo_path: str, since_days: int) -> str:
    """Run git log and return raw output."""
    result = subprocess.run(
        [
            "git",
            "-C",
            repo_path,
            "log",
            f"--format={_LOG_FORMAT}",
            "--numstat",
            f"--since={since_days} days ago",
        ],
        capture_output=True,
        text=True,
        check=False,
    )
    if result.returncode != 0:
        stderr = result.stderr.strip()
        # Empty repos (no commits yet) return non-zero — treat as empty
        if "does not have any commits" in stderr:
            return ""
        msg = f"git log failed: {stderr}"
        raise RuntimeError(msg)
    return result.stdout


def _parse_log_output(raw: str) -> list[CommitExtraction]:
    """Parse git log output into CommitExtraction objects."""
    commits: list[CommitExtraction] = []
    lines = raw.strip().split("\n")

    i = 0
    while i < len(lines):
        line = lines[i].strip()
        if not line:
            i += 1
            continue

        # Try to parse as a commit header line
        parts = line.split(_SEPARATOR, 3)
        if len(parts) == 4 and len(parts[0]) == 40:
            sha, author_email, timestamp, message = parts
            files_changed: list[FileChange] = []

            # Advance past the commit header
            i += 1

            # Parse numstat lines until next commit or end
            while i < len(lines):
                numstat_line = lines[i].strip()
                if not numstat_line:
                    i += 1
                    continue

                # Check if this is a new commit header
                numstat_parts = numstat_line.split(_SEPARATOR, 3)
                if len(numstat_parts) == 4 and len(numstat_parts[0]) == 40:
                    break

                # Parse numstat: additions\tdeletions\tfilepath
                tab_parts = numstat_line.split("\t")
                if len(tab_parts) == 3:
                    adds_str, dels_str, file_path = tab_parts
                    # Binary files show "-" for additions/deletions
                    additions = int(adds_str) if adds_str != "-" else 0
                    deletions = int(dels_str) if dels_str != "-" else 0
                    files_changed.append(
                        FileChange(path=file_path, additions=additions, deletions=deletions)
                    )
                i += 1

            commits.append(
                CommitExtraction(
                    sha=sha,
                    author_email=author_email,
                    timestamp=timestamp,
                    message=message,
                    files_changed=files_changed,
                )
            )
        else:
            i += 1

    return commits
