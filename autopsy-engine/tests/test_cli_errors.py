"""Tests for CLI error handling."""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

_ENGINE_DIR = str(Path(__file__).resolve().parent.parent)


def _run_cli(args: list[str]) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [sys.executable, "-m", "autopsy.cli", *args],
        capture_output=True,
        text=True,
        check=False,
        cwd=_ENGINE_DIR,
    )


class TestCliErrors:
    def test_no_arguments_shows_usage(self) -> None:
        result = _run_cli([])
        assert result.returncode == 1
        assert "Usage" in result.stderr

    def test_unknown_command_shows_usage(self) -> None:
        result = _run_cli(["bogus"])
        assert result.returncode == 1
        assert "Unknown command" in result.stderr

    def test_extract_no_path_shows_usage(self) -> None:
        result = _run_cli(["extract"])
        assert result.returncode == 1
        assert "Usage" in result.stderr

    def test_extract_nonexistent_path(self) -> None:
        result = _run_cli(["extract", "/nonexistent/path/to/repo"])
        assert result.returncode == 1
        assert "Error" in result.stderr

    def test_extract_not_a_git_repo(self, tmp_path: Path) -> None:
        result = _run_cli(["extract", str(tmp_path)])
        assert result.returncode == 1
        assert "Error" in result.stderr

    def test_report_without_api_key(self) -> None:
        result = subprocess.run(
            [sys.executable, "-m", "autopsy.cli", "report"],
            capture_output=True,
            text=True,
            check=False,
            cwd=_ENGINE_DIR,
            input="{}",
            env={"PATH": "/usr/bin:/bin"},  # no OPENAI_API_KEY
        )
        assert result.returncode == 1
        assert "OPENAI_API_KEY" in result.stderr
