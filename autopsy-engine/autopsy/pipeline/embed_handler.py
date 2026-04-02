"""Pipeline stage: embed document chunks for RAG."""

from __future__ import annotations

import logging
from typing import Any
from uuid import UUID

from autopsy.ai.chunker import chunk_docs
from autopsy.ai.embedder import Embedder
from autopsy.db.repositories import get_extractions_by_run, save_chunks, update_run_status
from autopsy.models import DocExtraction
from autopsy.queue.producer import enqueue_next_stage

logger = logging.getLogger(__name__)


def handle_embed(
    conn: Any,
    redis_client: Any,
    run_id: str,
    repo_id: str,
    org_id: str,
    openai_client: Any | None = None,
) -> None:
    """Chunk docs, generate embeddings, write to DB, enqueue REPORT."""
    update_run_status(conn, UUID(run_id), "running", "embed")

    try:
        # Read doc extractions
        doc_rows = get_extractions_by_run(conn, UUID(run_id), "doc")
        docs = [DocExtraction.from_dict(row["payload"]) for row in doc_rows]

        # Chunk documents
        chunks = chunk_docs(docs)

        if chunks and openai_client:
            # Generate embeddings
            embedder = Embedder(client=openai_client)
            embedded = embedder.embed_chunks(chunks)

            chunk_records = [
                {
                    "source_type": ec.source_type,
                    "source_ref": ec.source_ref,
                    "content": ec.content,
                    "embedding": ec.embedding,
                }
                for ec in embedded
            ]
        else:
            # No OpenAI client — store chunks without embeddings
            chunk_records = [
                {
                    "source_type": c.source_type,
                    "source_ref": c.source_ref,
                    "content": c.content,
                }
                for c in chunks
            ]

        save_chunks(conn, UUID(repo_id), chunk_records)

        logger.info("Embedded %d chunks for run %s", len(chunk_records), run_id)

        enqueue_next_stage(redis_client, run_id, repo_id, org_id, "EMBED")

    except Exception as e:
        update_run_status(conn, UUID(run_id), "failed", "embed", str(e))
        raise
