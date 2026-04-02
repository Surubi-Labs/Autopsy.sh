"""Data models for the AI layer."""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class Chunk:
    """A document chunk for embedding and retrieval."""

    source_type: str
    source_ref: str
    content: str
    metadata: str


@dataclass(frozen=True)
class EmbeddedChunk:
    """A chunk with its embedding vector."""

    source_type: str
    source_ref: str
    content: str
    metadata: str
    embedding: list[float]
