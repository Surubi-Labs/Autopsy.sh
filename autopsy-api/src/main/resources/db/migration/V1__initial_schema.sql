-- Autopsy Phase 2: Core schema
-- 7 tables for multi-tenant codebase health monitoring

CREATE TABLE organizations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT NOT NULL,
    plan            TEXT NOT NULL DEFAULT 'free',
    stripe_customer TEXT,
    repo_limit      INT NOT NULL DEFAULT 3,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE repositories (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id              UUID NOT NULL REFERENCES organizations(id),
    name                TEXT NOT NULL,
    git_url             TEXT NOT NULL,
    default_branch      TEXT NOT NULL DEFAULT 'main',
    github_install_id   BIGINT,
    last_analyzed_sha   TEXT,
    last_analyzed_at    TIMESTAMPTZ,
    schedule            TEXT NOT NULL DEFAULT 'weekly',
    active              BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(org_id, git_url)
);

CREATE TABLE analysis_runs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repo_id         UUID NOT NULL REFERENCES repositories(id),
    status          TEXT NOT NULL DEFAULT 'queued'
                    CHECK (status IN ('queued', 'running', 'completed', 'failed')),
    current_stage   TEXT,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    error           TEXT,
    commit_range    TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_runs_repo_status ON analysis_runs(repo_id, status);

CREATE TABLE extractions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id          UUID NOT NULL REFERENCES analysis_runs(id),
    data_type       TEXT NOT NULL
                    CHECK (data_type IN ('commit', 'file_diff', 'dependency', 'doc')),
    module_path     TEXT,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_extractions_run ON extractions(run_id, data_type);

CREATE TABLE metrics (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id          UUID NOT NULL REFERENCES analysis_runs(id),
    repo_id         UUID NOT NULL REFERENCES repositories(id),
    module_path     TEXT,
    metric_type     TEXT NOT NULL,
    value           DOUBLE PRECISION NOT NULL,
    details         JSONB,
    computed_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_metrics_repo_module ON metrics(repo_id, module_path, metric_type);
CREATE INDEX idx_metrics_time ON metrics(repo_id, computed_at);

CREATE TABLE chunks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repo_id         UUID NOT NULL REFERENCES repositories(id),
    source_type     TEXT NOT NULL,
    source_ref      TEXT NOT NULL,
    content         TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chunks_repo ON chunks(repo_id);

CREATE TABLE reports (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id              UUID NOT NULL REFERENCES analysis_runs(id),
    repo_id             UUID NOT NULL REFERENCES repositories(id),
    markdown            TEXT NOT NULL,
    summary             TEXT,
    risk_score          DOUBLE PRECISION,
    delivered_at        TIMESTAMPTZ,
    delivery_channel    TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
