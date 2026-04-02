"""Document chunker — splits docs into chunks for embedding."""

from __future__ import annotations

import re
from typing import TYPE_CHECKING

import tiktoken

from autopsy.ai.models import Chunk

if TYPE_CHECKING:
    from autopsy.models import DocExtraction

_MAX_CHUNK_TOKENS = 512
_OVERLAP_TOKENS = 50
_HEADER_PATTERN = re.compile(r"^##+ ", re.MULTILINE)


def chunk_docs(docs: list[DocExtraction]) -> list[Chunk]:
    """Chunk documents for embedding.

    Strategy:
    1. Split by markdown section headers (## or ###).
    2. Fallback to fixed-size token chunking for docs without headers.
    """
    chunks: list[Chunk] = []
    for doc in docs:
        if not doc.content.strip():
            continue
        doc_chunks = _chunk_single_doc(doc)
        chunks.extend(doc_chunks)
    return chunks


def _chunk_single_doc(doc: DocExtraction) -> list[Chunk]:
    """Chunk a single document."""
    sections = _split_by_headers(doc.content)

    if len(sections) > 1:
        return [
            Chunk(
                source_type=doc.type,
                source_ref=doc.path,
                content=section.strip(),
                metadata=f"{doc.title} | {doc.path}",
            )
            for section in sections
            if section.strip()
        ]

    # Fallback: fixed-size chunking
    return _fixed_size_chunks(doc)


def _split_by_headers(content: str) -> list[str]:
    """Split content by markdown section headers."""
    positions = [m.start() for m in _HEADER_PATTERN.finditer(content)]
    if not positions:
        return [content]

    sections: list[str] = []
    for i, pos in enumerate(positions):
        end = positions[i + 1] if i + 1 < len(positions) else len(content)
        sections.append(content[pos:end])

    return sections


def _fixed_size_chunks(doc: DocExtraction) -> list[Chunk]:
    """Split a document into fixed-size token chunks with overlap."""
    enc = tiktoken.get_encoding("cl100k_base")
    tokens = enc.encode(doc.content)

    if len(tokens) <= _MAX_CHUNK_TOKENS:
        return [
            Chunk(
                source_type=doc.type,
                source_ref=doc.path,
                content=doc.content.strip(),
                metadata=f"{doc.title} | {doc.path}",
            )
        ]

    chunks: list[Chunk] = []
    start = 0
    while start < len(tokens):
        end = min(start + _MAX_CHUNK_TOKENS, len(tokens))
        chunk_tokens = tokens[start:end]
        chunk_text = enc.decode(chunk_tokens)
        chunks.append(
            Chunk(
                source_type=doc.type,
                source_ref=doc.path,
                content=chunk_text.strip(),
                metadata=f"{doc.title} | {doc.path}",
            )
        )
        start = end - _OVERLAP_TOKENS if end < len(tokens) else end

    return chunks
