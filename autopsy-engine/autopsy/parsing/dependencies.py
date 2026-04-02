"""Dependency file parsers for various package managers."""

from __future__ import annotations

import json
import re
from pathlib import Path

from autopsy.models import DependencyExtraction, DependencyInfo


def parse_package_json(file_path: str) -> DependencyExtraction:
    """Parse a package.json file and extract dependencies.

    Raises:
        ValueError: If the JSON is malformed.
    """
    path = Path(file_path)
    try:
        data = json.loads(path.read_text())
    except json.JSONDecodeError as e:
        msg = f"Malformed JSON in {file_path}: {e}"
        raise ValueError(msg) from e

    deps: list[DependencyInfo] = []

    for section in ("dependencies", "devDependencies"):
        section_data = data.get(section, {})
        for name, version in section_data.items():
            deps.append(DependencyInfo(name=name, current=version))

    return DependencyExtraction(file=file_path, manager="npm", dependencies=deps)


def parse_requirements_txt(file_path: str) -> DependencyExtraction:
    """Parse a requirements.txt file and extract dependencies."""
    path = Path(file_path)
    deps: list[DependencyInfo] = []

    for raw_line in path.read_text().splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or line.startswith("-"):
            continue

        name, version = _parse_requirement_line(line)
        if name:
            deps.append(DependencyInfo(name=name, current=version))

    return DependencyExtraction(file=file_path, manager="pip", dependencies=deps)


def _parse_requirement_line(line: str) -> tuple[str, str]:
    """Parse a single requirement line into (name, version)."""
    # Remove inline comments
    line = line.split("#")[0].strip()
    if not line:
        return ("", "")

    # Match patterns: name==1.0, name>=1.0, name~=1.0, name>=1.0,<2.0, name
    match = re.match(r"^([a-zA-Z0-9_-]+(?:\[[^\]]*\])?)\s*(.*)", line)
    if not match:
        return ("", "")

    name = match.group(1)
    # Strip extras like [standard]
    name = re.sub(r"\[.*\]", "", name)
    version_spec = match.group(2).strip()

    if not version_spec:
        return (name, "*")

    # Extract the first version number from the spec
    version_match = re.search(r"[><=~!]+\s*([0-9][0-9a-zA-Z.*]*)", version_spec)
    if version_match:
        return (name, version_match.group(1))

    return (name, version_spec)


def parse_pyproject_toml(file_path: str) -> DependencyExtraction:
    """Parse a pyproject.toml file (PEP 621) and extract dependencies."""
    path = Path(file_path)
    content = path.read_text()

    deps: list[DependencyInfo] = []

    # Simple TOML parsing for [project] dependencies array
    # Look for dependencies = [ ... ] under [project]
    in_project = False
    in_deps_array = False
    for line in content.splitlines():
        stripped = line.strip()

        if stripped.startswith("["):
            in_project = stripped == "[project]"
            in_deps_array = False
            continue

        if in_project and stripped.startswith("dependencies"):
            in_deps_array = True
            continue

        if in_deps_array:
            if stripped == "]":
                in_deps_array = False
                continue
            # Parse quoted dependency strings
            match = re.match(r'^\s*"([^"]+)"', stripped)
            if match:
                dep_str = match.group(1)
                name, version = _parse_requirement_line(dep_str)
                if name:
                    deps.append(DependencyInfo(name=name, current=version))

    return DependencyExtraction(file=file_path, manager="pip", dependencies=deps)


# Patterns for Gradle dependency declarations
_GRADLE_GROOVY_PATTERN = re.compile(
    r"""(?:implementation|api|testImplementation|runtimeOnly|compileOnly)\s+['"]"""
    r"""([^:'"]+):([^:'"]+):([^'"]+)['"]"""
)

_GRADLE_KOTLIN_PATTERN = re.compile(
    r"""(?:implementation|api|testImplementation|runtimeOnly|compileOnly)\s*\(\s*["']"""
    r"""([^:"']+):([^:"']+):([^"']+)["']\s*\)"""
)


def parse_gradle(file_path: str) -> DependencyExtraction:
    """Parse a build.gradle or build.gradle.kts file and extract dependencies."""
    path = Path(file_path)
    content = path.read_text()

    deps: list[DependencyInfo] = []
    seen: set[str] = set()

    for pattern in (_GRADLE_GROOVY_PATTERN, _GRADLE_KOTLIN_PATTERN):
        for match in pattern.finditer(content):
            group, artifact, version = match.group(1), match.group(2), match.group(3)
            full_name = f"{group}:{artifact}"
            if full_name not in seen:
                seen.add(full_name)
                deps.append(DependencyInfo(name=full_name, current=version))

    return DependencyExtraction(file=file_path, manager="gradle", dependencies=deps)
