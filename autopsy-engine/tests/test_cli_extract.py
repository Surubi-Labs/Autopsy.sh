"""Integration tests for the extraction CLI entry point."""

from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

_ENGINE_DIR = str(Path(__file__).resolve().parent.parent)


def _init_test_repo(tmp_path: Path) -> None:
    """Create a realistic test git repo."""
    subprocess.run(["git", "init"], cwd=tmp_path, capture_output=True, check=True)
    subprocess.run(
        ["git", "config", "user.email", "dev@test.com"],
        cwd=tmp_path, capture_output=True, check=True,
    )
    subprocess.run(
        ["git", "config", "user.name", "Dev"],
        cwd=tmp_path, capture_output=True, check=True,
    )

    # Add source files
    src = tmp_path / "src" / "auth"
    src.mkdir(parents=True)
    (src / "login.py").write_text("def login():\n    pass\n")

    # Add test files
    tests = tmp_path / "tests"
    tests.mkdir()
    (tests / "test_login.py").write_text("def test_login():\n    assert True\n")

    # Add a package.json
    (tmp_path / "package.json").write_text(
        json.dumps({"dependencies": {"express": "4.18.2"}})
    )

    # Add a README
    (tmp_path / "README.md").write_text("# Test Project\nA test project.")

    subprocess.run(["git", "add", "."], cwd=tmp_path, capture_output=True, check=True)
    subprocess.run(
        ["git", "commit", "-m", "initial commit"],
        cwd=tmp_path, capture_output=True, check=True,
    )


def _run_cli(args: list[str]) -> subprocess.CompletedProcess[str]:
    """Run the autopsy CLI as a subprocess."""
    return subprocess.run(
        [sys.executable, "-m", "autopsy.cli", *args],
        capture_output=True,
        text=True,
        check=False,
        cwd=_ENGINE_DIR,
    )


class TestCliExtract:
    def test_extract_produces_valid_json(self, tmp_path: Path) -> None:
        _init_test_repo(tmp_path)
        result = _run_cli(["extract", str(tmp_path)])
        assert result.returncode == 0, f"stderr: {result.stderr}"
        data = json.loads(result.stdout)
        assert "commits" in data
        assert "dependencies" in data
        assert "docs" in data
        assert "test_files" in data
        assert "source_files" in data

    def test_extract_includes_commits(self, tmp_path: Path) -> None:
        _init_test_repo(tmp_path)
        result = _run_cli(["extract", str(tmp_path)])
        assert result.returncode == 0, f"stderr: {result.stderr}"
        data = json.loads(result.stdout)
        assert len(data["commits"]) >= 1
        assert data["commits"][0]["sha"]

    def test_extract_includes_dependencies(self, tmp_path: Path) -> None:
        _init_test_repo(tmp_path)
        result = _run_cli(["extract", str(tmp_path)])
        assert result.returncode == 0, f"stderr: {result.stderr}"
        data = json.loads(result.stdout)
        assert len(data["dependencies"]) >= 1

    def test_extract_includes_docs(self, tmp_path: Path) -> None:
        _init_test_repo(tmp_path)
        result = _run_cli(["extract", str(tmp_path)])
        assert result.returncode == 0, f"stderr: {result.stderr}"
        data = json.loads(result.stdout)
        assert len(data["docs"]) >= 1
        assert data["docs"][0]["type"] == "readme"

    def test_extract_classifies_test_and_source_files(self, tmp_path: Path) -> None:
        _init_test_repo(tmp_path)
        result = _run_cli(["extract", str(tmp_path)])
        assert result.returncode == 0, f"stderr: {result.stderr}"
        data = json.loads(result.stdout)
        assert any("test_login.py" in f for f in data["test_files"])
        assert any("login.py" in f for f in data["source_files"])

    def test_extract_invalid_path_exits_nonzero(self) -> None:
        result = _run_cli(["extract", "/nonexistent/path"])
        assert result.returncode != 0
