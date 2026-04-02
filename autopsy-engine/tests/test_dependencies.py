"""Tests for dependency file parsers."""

from __future__ import annotations

from typing import TYPE_CHECKING

from autopsy.parsing.dependencies import (
    parse_gradle,
    parse_package_json,
    parse_pyproject_toml,
    parse_requirements_txt,
)

if TYPE_CHECKING:
    from pathlib import Path

FIXTURES = "tests/fixtures/deps"


class TestParsePackageJson:
    def test_normal_package_json(self) -> None:
        result = parse_package_json(f"{FIXTURES}/package_normal.json")
        assert result.file == f"{FIXTURES}/package_normal.json"
        assert result.manager == "npm"
        names = {d.name for d in result.dependencies}
        assert "express" in names
        assert "lodash" in names
        assert "jest" in names
        assert "typescript" in names

    def test_extracts_version_numbers(self) -> None:
        result = parse_package_json(f"{FIXTURES}/package_normal.json")
        by_name = {d.name: d for d in result.dependencies}
        assert by_name["express"].current == "^4.18.2"
        assert by_name["lodash"].current == "4.17.21"

    def test_empty_package_json(self) -> None:
        result = parse_package_json(f"{FIXTURES}/package_empty.json")
        assert result.dependencies == []

    def test_malformed_json_raises(self, tmp_path: Path) -> None:
        bad_file = tmp_path / "package.json"
        bad_file.write_text("{not valid json")
        try:
            parse_package_json(str(bad_file))
            raise AssertionError("Should have raised")
        except ValueError:
            pass


class TestParseRequirementsTxt:
    def test_normal_requirements(self) -> None:
        result = parse_requirements_txt(f"{FIXTURES}/requirements_normal.txt")
        assert result.file == f"{FIXTURES}/requirements_normal.txt"
        assert result.manager == "pip"
        names = {d.name for d in result.dependencies}
        assert "flask" in names
        assert "requests" in names
        assert "python-dotenv" in names

    def test_extracts_pinned_versions(self) -> None:
        result = parse_requirements_txt(f"{FIXTURES}/requirements_normal.txt")
        by_name = {d.name: d for d in result.dependencies}
        assert by_name["flask"].current == "2.3.0"

    def test_unpinned_deps_have_version(self) -> None:
        result = parse_requirements_txt(f"{FIXTURES}/requirements_normal.txt")
        by_name = {d.name: d for d in result.dependencies}
        # python-dotenv has no version specifier
        assert by_name["python-dotenv"].current == "*"

    def test_skips_comments_and_blanks(self) -> None:
        result = parse_requirements_txt(f"{FIXTURES}/requirements_normal.txt")
        names = {d.name for d in result.dependencies}
        assert "" not in names
        assert "#" not in names


class TestParsePyprojectToml:
    def test_normal_pyproject(self) -> None:
        result = parse_pyproject_toml(f"{FIXTURES}/pyproject_normal.toml")
        assert result.file == f"{FIXTURES}/pyproject_normal.toml"
        assert result.manager == "pip"
        names = {d.name for d in result.dependencies}
        assert "fastapi" in names
        assert "uvicorn" in names
        assert "pydantic" in names

    def test_extracts_versions(self) -> None:
        result = parse_pyproject_toml(f"{FIXTURES}/pyproject_normal.toml")
        by_name = {d.name: d for d in result.dependencies}
        assert by_name["fastapi"].current == "0.100.0"


class TestParseGradle:
    def test_groovy_dsl(self) -> None:
        result = parse_gradle(f"{FIXTURES}/build_groovy.gradle")
        assert result.manager == "gradle"
        names = {d.name for d in result.dependencies}
        assert "spring-boot-starter-web" in names or any(
            "spring-boot-starter-web" in d.name for d in result.dependencies
        )

    def test_kotlin_dsl(self) -> None:
        result = parse_gradle(f"{FIXTURES}/build_kotlin.gradle.kts")
        assert result.manager == "gradle"
        assert len(result.dependencies) >= 3

    def test_extracts_group_artifact_version(self) -> None:
        result = parse_gradle(f"{FIXTURES}/build_groovy.gradle")
        by_name = {d.name: d for d in result.dependencies}
        # Should extract jackson-databind with version
        jackson = by_name.get("com.fasterxml.jackson.core:jackson-databind")
        assert jackson is not None
        assert jackson.current == "2.16.0"
