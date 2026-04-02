"""Tests for dependency scanner orchestrator."""

from __future__ import annotations

from typing import TYPE_CHECKING

from autopsy.parsing.dependency_scanner import scan_dependencies

if TYPE_CHECKING:
    from pathlib import Path


class TestScanDependencies:
    def test_finds_package_json(self, tmp_path: Path) -> None:
        (tmp_path / "package.json").write_text('{"dependencies": {"express": "4.18.2"}}')
        results = scan_dependencies(str(tmp_path))
        assert len(results) == 1
        assert results[0].manager == "npm"

    def test_finds_nested_dependency_files(self, tmp_path: Path) -> None:
        # package.json at root
        (tmp_path / "package.json").write_text('{"dependencies": {"express": "4.18.2"}}')
        # requirements.txt in a subdirectory
        sub = tmp_path / "backend"
        sub.mkdir()
        (sub / "requirements.txt").write_text("flask==2.3.0\n")
        results = scan_dependencies(str(tmp_path))
        assert len(results) == 2
        managers = {r.manager for r in results}
        assert "npm" in managers
        assert "pip" in managers

    def test_ignores_unknown_files(self, tmp_path: Path) -> None:
        (tmp_path / "random.txt").write_text("not a dep file")
        results = scan_dependencies(str(tmp_path))
        assert results == []

    def test_parser_error_does_not_block_others(self, tmp_path: Path) -> None:
        # Malformed package.json
        (tmp_path / "package.json").write_text("{bad json")
        # Valid requirements.txt
        (tmp_path / "requirements.txt").write_text("flask==2.3.0\n")
        results = scan_dependencies(str(tmp_path))
        # Should still get the requirements.txt result
        assert len(results) == 1
        assert results[0].manager == "pip"

    def test_empty_directory(self, tmp_path: Path) -> None:
        results = scan_dependencies(str(tmp_path))
        assert results == []

    def test_skips_node_modules(self, tmp_path: Path) -> None:
        nm = tmp_path / "node_modules" / "express"
        nm.mkdir(parents=True)
        (nm / "package.json").write_text('{"dependencies": {"dep": "1.0"}}')
        # Root package.json should be found
        (tmp_path / "package.json").write_text('{"dependencies": {"express": "4.18.2"}}')
        results = scan_dependencies(str(tmp_path))
        assert len(results) == 1
