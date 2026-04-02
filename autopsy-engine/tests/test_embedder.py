"""Tests for embedding generator."""

from __future__ import annotations

from unittest.mock import MagicMock

from autopsy.ai.embedder import Embedder
from autopsy.ai.models import Chunk


class TestEmbedder:
    def test_embeds_chunks_calls_api(self) -> None:
        mock_client = MagicMock()
        mock_response = MagicMock()
        mock_response.data = [
            MagicMock(embedding=[0.1] * 1536),
        ]
        mock_client.embeddings.create.return_value = mock_response

        embedder = Embedder(client=mock_client)
        chunks = [Chunk("readme", "README.md", "content", "metadata")]
        result = embedder.embed_chunks(chunks)

        assert len(result) == 1
        assert len(result[0].embedding) == 1536
        mock_client.embeddings.create.assert_called_once()

    def test_batches_large_inputs(self) -> None:
        mock_client = MagicMock()
        mock_response = MagicMock()
        # Create 150 mock embeddings for 150 chunks
        mock_response.data = [MagicMock(embedding=[0.1] * 1536) for _ in range(100)]
        mock_response2 = MagicMock()
        mock_response2.data = [MagicMock(embedding=[0.1] * 1536) for _ in range(50)]
        mock_client.embeddings.create.side_effect = [mock_response, mock_response2]

        embedder = Embedder(client=mock_client)
        chunks = [
            Chunk("readme", "README.md", f"content {i}", "meta") for i in range(150)
        ]
        result = embedder.embed_chunks(chunks)

        assert len(result) == 150
        assert mock_client.embeddings.create.call_count == 2

    def test_empty_chunks_returns_empty(self) -> None:
        mock_client = MagicMock()
        embedder = Embedder(client=mock_client)
        result = embedder.embed_chunks([])
        assert result == []
        mock_client.embeddings.create.assert_not_called()
