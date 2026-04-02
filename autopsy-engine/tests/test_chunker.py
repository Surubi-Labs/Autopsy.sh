"""Tests for document chunker."""

from __future__ import annotations

from autopsy.ai.chunker import chunk_docs
from autopsy.models import DocExtraction


class TestChunkDocs:
    def test_splits_by_section_headers(self) -> None:
        doc = DocExtraction(
            type="adr",
            path="docs/adr/001.md",
            title="Use PostgreSQL",
            content=(
                "## Context\nWe need a database.\n\n"
                "## Decision\nUse PostgreSQL.\n\n"
                "## Status\nAccepted."
            ),
            last_modified="2026-01-01T00:00:00Z",
        )
        chunks = chunk_docs([doc])
        assert len(chunks) == 3
        assert "Context" in chunks[0].content
        assert "Decision" in chunks[1].content
        assert "Status" in chunks[2].content

    def test_includes_metadata_in_chunks(self) -> None:
        doc = DocExtraction(
            type="readme",
            path="README.md",
            title="My Project",
            content="## Section 1\nContent here.",
            last_modified="2026-01-01T00:00:00Z",
        )
        chunks = chunk_docs([doc])
        assert chunks[0].source_type == "readme"
        assert chunks[0].source_ref == "README.md"
        assert "My Project" in chunks[0].metadata

    def test_empty_doc_returns_empty_list(self) -> None:
        doc = DocExtraction(
            type="readme", path="README.md", title="", content="",
            last_modified="2026-01-01T00:00:00Z",
        )
        chunks = chunk_docs([doc])
        assert chunks == []

    def test_doc_without_headers_uses_fixed_size(self) -> None:
        # Create a long doc with no headers
        long_content = "This is a paragraph. " * 200
        doc = DocExtraction(
            type="readme", path="README.md", title="Readme",
            content=long_content, last_modified="2026-01-01T00:00:00Z",
        )
        chunks = chunk_docs([doc])
        assert len(chunks) > 1

    def test_empty_input_returns_empty(self) -> None:
        chunks = chunk_docs([])
        assert chunks == []

    def test_multiple_docs_combined(self) -> None:
        doc1 = DocExtraction(
            type="adr", path="adr/001.md", title="ADR 1",
            content="## Decision\nUse X.", last_modified="2026-01-01T00:00:00Z",
        )
        doc2 = DocExtraction(
            type="readme", path="README.md", title="Readme",
            content="## Overview\nProject info.", last_modified="2026-01-01T00:00:00Z",
        )
        chunks = chunk_docs([doc1, doc2])
        assert len(chunks) == 2
        types = {c.source_type for c in chunks}
        assert "adr" in types
        assert "readme" in types
