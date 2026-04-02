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
    elif command == "report":
        _handle_report()
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


def _handle_report() -> None:
    """Handle the 'report' subcommand. Reads metrics JSON from stdin."""
    import os

    from autopsy.ai.chunker import chunk_docs
    from autopsy.ai.reporter import generate_report
    from autopsy.models import DocExtraction

    api_key = os.environ.get("OPENAI_API_KEY")
    if not api_key:
        print("Error: OPENAI_API_KEY environment variable is required", file=sys.stderr)
        sys.exit(1)

    try:
        input_data = json.load(sys.stdin)
    except json.JSONDecodeError as e:
        print(f"Error: Invalid JSON input: {e}", file=sys.stderr)
        sys.exit(1)

    metrics_json = json.dumps(input_data.get("metrics", input_data), indent=2)

    # Extract docs from input if present
    raw_docs = input_data.get("docs", [])
    docs = [DocExtraction.from_dict(d) for d in raw_docs]
    chunks = chunk_docs(docs)

    try:
        from openai import OpenAI

        client = OpenAI(api_key=api_key)
        report = generate_report(
            client=client,
            metrics_json=metrics_json,
            chunks=chunks,
            previous_summary=input_data.get("previous_summary"),
        )
        print(report)
    except Exception as e:
        print(f"Error generating report: {e}", file=sys.stderr)
        sys.exit(1)


def _print_usage() -> None:
    """Print CLI usage."""
    print("Usage: autopsy-engine <command> [args]", file=sys.stderr)
    print("Commands:", file=sys.stderr)
    print("  extract <repo-path>  Extract git data from a repository", file=sys.stderr)
    print("  report               Generate AI report (reads JSON from stdin)", file=sys.stderr)


if __name__ == "__main__":
    main()
