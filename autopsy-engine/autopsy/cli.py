"""CLI entry point for the Autopsy engine."""

from __future__ import annotations

import json
import sys

from autopsy.git.extractor import extract_commits
from autopsy.models import ExtractionResult
from autopsy.parsing.dependency_scanner import scan_dependencies
from autopsy.parsing.docs import detect_docs
from autopsy.parsing.test_detector import detect_test_files


def main() -> None:
    """Main entry point for the autopsy-engine CLI."""
    if len(sys.argv) < 2:
        _print_usage()
        sys.exit(1)

    command = sys.argv[1]

    if command == "extract":
        _handle_extract()
    else:
        print(f"Unknown command: {command}", file=sys.stderr)
        _print_usage()
        sys.exit(1)


def _handle_extract() -> None:
    """Handle the 'extract' subcommand."""
    if len(sys.argv) < 3:
        print("Usage: autopsy-engine extract <repo-path>", file=sys.stderr)
        sys.exit(1)

    repo_path = sys.argv[2]

    try:
        commits = extract_commits(repo_path)
        dependencies = scan_dependencies(repo_path)
        docs = detect_docs(repo_path)
        test_files, source_files = detect_test_files(repo_path)

        result = ExtractionResult(
            commits=commits,
            dependencies=dependencies,
            docs=docs,
            test_files=test_files,
            source_files=source_files,
        )

        json.dump(result.to_dict(), sys.stdout, indent=2)
        print()  # trailing newline

    except (FileNotFoundError, RuntimeError) as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)


def _print_usage() -> None:
    """Print CLI usage."""
    print("Usage: autopsy-engine <command> [args]", file=sys.stderr)
    print("Commands:", file=sys.stderr)
    print("  extract <repo-path>  Extract git data from a repository", file=sys.stderr)


if __name__ == "__main__":
    main()
