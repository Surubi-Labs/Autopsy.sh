"""Tests for test file detector."""

from __future__ import annotations

from typing import TYPE_CHECKING

from autopsy.parsing.test_detector import detect_test_files

if TYPE_CHECKING:
    from pathlib import Path


class TestDetectTestFiles:
    def test_detects_python_test_files(self, tmp_path: Path) -> None:
        tests_dir = tmp_path / "tests"
        tests_dir.mkdir()
        (tests_dir / "test_handler.py").write_text("")
        src_dir = tmp_path / "src"
        src_dir.mkdir(parents=True)
        (src_dir / "handler.py").write_text("")
        test_files, source_files = detect_test_files(str(tmp_path))
        assert "tests/test_handler.py" in test_files
        assert "src/handler.py" in source_files

    def test_detects_kotlin_test_files(self, tmp_path: Path) -> None:
        (tmp_path / "src" / "main").mkdir(parents=True)
        (tmp_path / "src" / "main" / "App.kt").write_text("")
        (tmp_path / "src" / "test").mkdir(parents=True)
        (tmp_path / "src" / "test" / "AppTest.kt").write_text("")
        test_files, source_files = detect_test_files(str(tmp_path))
        assert any("AppTest.kt" in f for f in test_files)
        assert any("App.kt" in f for f in source_files)

    def test_detects_js_test_conventions(self, tmp_path: Path) -> None:
        tests_dir = tmp_path / "__tests__"
        tests_dir.mkdir()
        (tests_dir / "app.test.js").write_text("")
        (tmp_path / "src").mkdir()
        (tmp_path / "src" / "app.js").write_text("")
        test_files, source_files = detect_test_files(str(tmp_path))
        assert any("app.test.js" in f for f in test_files)

    def test_detects_go_test_files(self, tmp_path: Path) -> None:
        (tmp_path / "handler.go").write_text("")
        (tmp_path / "handler_test.go").write_text("")
        test_files, source_files = detect_test_files(str(tmp_path))
        assert "handler_test.go" in test_files
        assert "handler.go" in source_files

    def test_excludes_non_code_files(self, tmp_path: Path) -> None:
        (tmp_path / "image.png").write_text("")
        (tmp_path / "config.yml").write_text("")
        (tmp_path / "README.md").write_text("")
        test_files, source_files = detect_test_files(str(tmp_path))
        all_files = test_files + source_files
        assert not any(f.endswith(".png") for f in all_files)
        assert not any(f.endswith(".yml") for f in all_files)
        assert not any(f.endswith(".md") for f in all_files)

    def test_empty_directory(self, tmp_path: Path) -> None:
        test_files, source_files = detect_test_files(str(tmp_path))
        assert test_files == []
        assert source_files == []

    def test_spec_files_detected_as_test(self, tmp_path: Path) -> None:
        (tmp_path / "spec").mkdir()
        (tmp_path / "spec" / "handler_spec.rb").write_text("")
        test_files, _ = detect_test_files(str(tmp_path))
        assert any("handler_spec.rb" in f for f in test_files)
