-- GitHub user to organization mapping
CREATE TABLE github_users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    github_user_id  BIGINT NOT NULL UNIQUE,
    github_login    TEXT NOT NULL,
    org_id          UUID NOT NULL REFERENCES organizations(id),
    access_token    TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_github_users_org ON github_users(org_id);
