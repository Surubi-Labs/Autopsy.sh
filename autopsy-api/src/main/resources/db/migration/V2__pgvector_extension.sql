-- Enable pgvector extension for embedding storage
CREATE EXTENSION IF NOT EXISTS vector;

-- Add embedding column to chunks table
ALTER TABLE chunks ADD COLUMN embedding vector(1536);

-- IVFFlat index for cosine similarity search
CREATE INDEX idx_chunks_embedding ON chunks
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
