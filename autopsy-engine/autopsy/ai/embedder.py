"""Embedding generator — creates vector embeddings for document chunks."""

from __future__ import annotations

from typing import Any

from autopsy.ai.models import Chunk, EmbeddedChunk

_BATCH_SIZE = 100
_DEFAULT_MODEL = "text-embedding-3-small"


class Embedder:
    """Generates embeddings for document chunks via OpenAI API."""

    def __init__(
        self,
        client: Any = None,
        model: str = _DEFAULT_MODEL,
    ) -> None:
        self._client = client
        self._model = model

    def embed_chunks(self, chunks: list[Chunk]) -> list[EmbeddedChunk]:
        """Generate embeddings for a list of chunks.

        Batches API calls to avoid rate limits (max 100 per request).
        """
        if not chunks:
            return []

        result: list[EmbeddedChunk] = []

        for i in range(0, len(chunks), _BATCH_SIZE):
            batch = chunks[i : i + _BATCH_SIZE]
            texts = [c.content for c in batch]

            response = self._client.embeddings.create(
                input=texts,
                model=self._model,
            )

            for chunk, embedding_data in zip(batch, response.data, strict=True):
                result.append(
                    EmbeddedChunk(
                        source_type=chunk.source_type,
                        source_ref=chunk.source_ref,
                        content=chunk.content,
                        metadata=chunk.metadata,
                        embedding=embedding_data.embedding,
                    )
                )

        return result
