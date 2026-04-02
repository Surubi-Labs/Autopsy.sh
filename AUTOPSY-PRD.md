# Autopsy - Product Requirements Document

## 1. Overview

Autopsy is a codebase health monitoring tool that analyzes git repositories and generates AI-powered health reports for engineering teams. It examines commit patterns, code churn, ownership concentration, dependency staleness, and test coverage trends, then produces narrative reports grounded in the repository's own documentation and decision history.

Unlike static analysis tools (SonarQube, CodeClimate) that look at code quality, or engineering metrics platforms (LinearB, Jellyfish) that track DORA metrics and developer productivity, Autopsy combines repo-level technical signals with AI-generated narrative analysis grounded in the project's own context. Every claim is backed by evidence from commits, PRs, and documentation.

### 1.1 Problem Statement

Engineering teams accumulate tech debt invisibly. Code ownership concentrates in single contributors. Dependencies go stale. Test coverage erodes. Documentation becomes outdated. These problems compound silently until something breaks or a key engineer leaves.

Existing tools either show raw metrics without interpretation (dashboards nobody checks) or provide generic AI advice not grounded in the actual project context. Nobody is producing evidence-backed, contextualized health reports that engineering leads can act on.

### 1.2 Target Customer

Engineering managers and VPs at Series A-C startups with 10-50 engineers. Large enough to have multiple repositories and tech debt accumulating, small enough that they lack a dedicated platform engineering team monitoring codebase health. The buyer opens this tool on Monday morning to understand what needs attention across their system.

### 1.3 Product Vision

Phase 1: Open source CLI tool. Analyze a local repo, output a markdown health report.
Phase 2: Multi-repo pipeline with job queue, PostgreSQL persistence, incremental analysis.
Phase 3: SaaS with API, GitHub OAuth, Next.js dashboard, scheduled reports.
Phase 4: Delivery (email/Slack), billing (Stripe), team management.

---

## 2. Architecture

### 2.1 System Boundaries

The system is split into three services that communicate via Redis (job queue) and PostgreSQL (shared data store).

```
┌─────────────────────────────────────┐
│  autopsy-api (Kotlin/Spring Boot)   │
│                                     │
│  - REST API (public + dashboard)    │
│  - Job orchestration                │
│  - Metrics computation              │
│  - Report delivery                  │
│  - Scheduling                       │
│  - Multi-tenancy                    │
│  - Billing                          │
└──────────────┬──────────────────────┘
               │ Redis (job queue)
               ▼
┌─────────────────────────────────────┐
│  autopsy-engine (Python)            │
│                                     │
│  - Git cloning/fetching             │
│  - History extraction               │
│  - Diff parsing                     │
│  - Dependency scanning              │
│  - Embedding generation             │
│  - LLM report generation            │
└──────────────┬──────────────────────┘
               │ PostgreSQL + pgvector
               ▼
┌─────────────────────────────────────┐
│  autopsy-web (Next.js)              │
│                                     │
│  - Dashboard                        │
│  - Repo management                  │
│  - Report viewer                    │
│  - Org settings / billing           │
└─────────────────────────────────────┘
```

### 2.2 Why This Split

Kotlin and Python run in separate processes. The question is how they communicate.

Option A: Subprocess calls (Kotlin shells out to Python). Simpler, but no retry logic, no visibility, no backpressure.
Option B: Shared Redis job queue. Retry logic, stage-level failure isolation, progress visibility, backpressure handling.

**Decision: Option B.** The queue gives operational maturity from day one and enables independent scaling of workers later. For Phase 1 (CLI), Kotlin calls Python via subprocess as a temporary bridge. Phase 2 introduces the queue.

### 2.3 Responsibility Boundaries

**Kotlin (autopsy-api)** owns:
- Business logic and domain modeling
- Metrics computation from raw extractions
- Job orchestration (what runs when, in what order)
- API surface and authentication
- Multi-tenancy, billing, scheduling
- Report delivery (email, Slack)

**Python (autopsy-engine)** owns:
- Everything that touches git (clone, fetch, log, diff, blame, worktree)
- File parsing (dependency files, docs, test detection)
- Embedding generation and vector storage
- LLM prompt construction and API calls
- RAG retrieval

**Key tradeoff: metrics computation lives in Kotlin, not Python.** Metrics computation is business logic (what counts as "high churn," how to weight bus factor, how to score dependency risk). This logic should be co-located with the API, tests, and billing rules. Python extracts raw structured data. Kotlin computes derived metrics. This boundary means metrics are fully unit-testable without spinning up git repos.

---

## 3. Data Architecture

### 3.1 Database Schema

PostgreSQL with pgvector extension. Single database, multi-tenant via org_id foreign keys (not RLS or schema-per-tenant in Phase 1, to keep it simple).

```sql
-- Multi-tenancy
CREATE TABLE organizations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT NOT NULL,
    plan            TEXT NOT NULL DEFAULT 'free',
    stripe_customer TEXT,
    repo_limit      INT NOT NULL DEFAULT 1,
    created_at      TIMESTAMP DEFAULT now()
);

-- Git repositories
CREATE TABLE repositories (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id              UUID NOT NULL REFERENCES organizations(id),
    name                TEXT NOT NULL,
    git_url             TEXT NOT NULL,
    default_branch      TEXT NOT NULL DEFAULT 'main',
    github_install_id   TEXT,
    last_analyzed_sha   TEXT,
    last_analyzed_at    TIMESTAMP,
    schedule            TEXT NOT NULL DEFAULT 'weekly',
    active              BOOLEAN DEFAULT true,
    created_at          TIMESTAMP DEFAULT now(),
    UNIQUE(org_id, git_url)
);

-- Analysis runs (one per repo per analysis cycle)
CREATE TABLE analysis_runs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repo_id         UUID NOT NULL REFERENCES repositories(id),
    status          TEXT NOT NULL DEFAULT 'queued',
    current_stage   TEXT,
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    error           TEXT,
    commit_range    TEXT,  -- 'abc123..def456'
    created_at      TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_runs_repo_status ON analysis_runs(repo_id, status);

-- Raw extractions (Python writes, Kotlin reads)
CREATE TABLE extractions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id          UUID NOT NULL REFERENCES analysis_runs(id),
    data_type       TEXT NOT NULL,  -- 'commit', 'file_diff', 'dependency', 'doc'
    module_path     TEXT,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_extractions_run ON extractions(run_id, data_type);

-- Computed metrics (Kotlin writes and reads)
CREATE TABLE metrics (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id          UUID NOT NULL REFERENCES analysis_runs(id),
    repo_id         UUID NOT NULL REFERENCES repositories(id),
    module_path     TEXT NOT NULL,
    metric_type     TEXT NOT NULL,  -- 'churn', 'bus_factor', 'hotspot', 'dep_staleness', 'test_ratio'
    value           DOUBLE PRECISION NOT NULL,
    details         JSONB,
    computed_at     TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_metrics_repo_module ON metrics(repo_id, module_path, metric_type);
CREATE INDEX idx_metrics_time ON metrics(repo_id, computed_at);

-- Document chunks for RAG (Python writes and reads)
CREATE TABLE chunks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repo_id         UUID NOT NULL REFERENCES repositories(id),
    source_type     TEXT NOT NULL,  -- 'adr', 'readme', 'pr_description', 'changelog'
    source_ref      TEXT,           -- file path or PR number
    content         TEXT NOT NULL,
    embedding       vector(1536),
    created_at      TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_chunks_embedding ON chunks
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- Generated reports
CREATE TABLE reports (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id              UUID NOT NULL REFERENCES analysis_runs(id),
    repo_id             UUID NOT NULL REFERENCES repositories(id),
    markdown            TEXT NOT NULL,
    summary             TEXT,           -- one-line for dashboard cards
    risk_score          DOUBLE PRECISION,
    delivered_at        TIMESTAMP,
    delivery_channel    TEXT,
    created_at          TIMESTAMP DEFAULT now()
);
```

### 3.2 Extraction Data Model

Python writes raw structured data to the extractions table as JSONB. This is the contract between Python and Kotlin.

**Commit extraction:**
```json
{
  "sha": "abc123",
  "author_email": "dev@company.com",
  "timestamp": "2026-03-15T14:30:00Z",
  "message": "refactor payment handler",
  "files_changed": [
    {"path": "src/payments/handler.kt", "additions": 45, "deletions": 12},
    {"path": "src/payments/handler_test.kt", "additions": 20, "deletions": 5}
  ]
}
```

**Dependency extraction:**
```json
{
  "file": "package.json",
  "manager": "npm",
  "dependencies": [
    {"name": "express", "current": "4.18.2", "latest": "4.19.1", "outdated_days": 142},
    {"name": "lodash", "current": "4.17.20", "latest": "4.17.21", "cves": ["CVE-2021-23337"]}
  ]
}
```

**Doc extraction:**
```json
{
  "type": "adr",
  "path": "docs/adr/003-migrate-to-graphql.md",
  "title": "Migrate from REST to GraphQL",
  "content": "## Context\nOur REST API has grown to 47 endpoints...",
  "last_modified": "2025-08-12T10:00:00Z"
}
```

### 3.3 Queue Protocol

Kotlin publishes jobs to Redis. Python consumes and writes results back.

**Kotlin to Python (job dispatch):**
```json
{
  "runId": "uuid",
  "repoId": "uuid",
  "orgId": "uuid",
  "stage": "clone|extract|embed|report",
  "config": {
    "repoUrl": "https://github.com/org/repo.git",
    "branch": "main",
    "lastAnalyzedSha": "abc123|null",
    "storagePath": "/data/repos/{orgId}/{repoId}.git"
  }
}
```

**Python to Kotlin (result notification):**
```json
{
  "runId": "uuid",
  "stage": "clone|extract|embed|report",
  "status": "completed|failed",
  "error": "message|null",
  "metadata": {
    "commitsProcessed": 142,
    "filesAnalyzed": 87,
    "chunksGenerated": 234
  }
}
```

### 3.4 Job Flow

```
Kotlin: publish clone job      -> Python: clones/fetches repo, writes status
Kotlin: receives "clone done"  -> publishes extract job
Python: extracts git data      -> writes to extractions table
Kotlin: receives "extract done"-> computes metrics IN-PROCESS (no queue)
Kotlin: publishes embed job    -> Python: chunks docs, generates embeddings
Kotlin: receives "embed done"  -> publishes report job
Python: retrieves context      -> calls LLM, writes report to DB
Kotlin: receives "report done" -> delivers via email/Slack IN-PROCESS
```

Serialization within a repo is enforced by BullMQ group ID:
```typescript
await queue.add('repo:clone', payload, {
  group: { id: `repo:${repoId}` }
});
```

All parallelism is across repos. Never within a repo. This eliminates all file-level locking concerns.

---

## 4. Git Storage Strategy

### 4.1 Storage Layout

```
/data/repos/
  /{org_id}/
    /{repo_id}.git          -> bare repo (persistent, shared)
    /worktrees/
      /{run_id}/            -> temporary worktree (per analysis run)
```

Tenant isolation is directory-level. Each org gets its own directory subtree.

### 4.2 Clone Strategy

**First run:** Bare clone. No working tree. Just the git object database.
```bash
git clone --bare https://github.com/org/repo.git /data/repos/{org_id}/{repo_id}.git
```

**Subsequent runs:** Incremental fetch. Only new objects since last analysis.
```bash
git --git-dir=/data/repos/{org_id}/{repo_id}.git fetch origin --prune
```

This turns a 5-minute clone into a 10-second fetch. The `last_analyzed_sha` field in the repositories table tracks where the previous analysis ended.

### 4.3 Worktree Strategy

Workers never read directly from the bare repo for file-level access. Instead, they create a temporary worktree:

```bash
git --git-dir=/data/repos/{org_id}/{repo_id}.git \
  worktree add /data/repos/{org_id}/worktrees/{run_id} \
  origin/main --detach
```

Worktrees share the object database with the bare repo via hardlinks. Creating a worktree for a 2GB repo takes seconds and uses minimal additional disk. Each analysis run gets its own worktree keyed by run_id. Two workers analyzing the same repo in different runs get independent file trees. No locks, no conflicts.

For git history analysis (commit log, blame, diff stats), workers read directly from the bare repo:
```bash
git --git-dir=/data/repos/{org_id}/{repo_id}.git log --format='%H|%ae|%at|%s' --numstat
```

Most metrics (churn, ownership, hotspots, commit patterns) come from git log and don't need a worktree. The worktree is only needed for reading actual file contents (package.json, ADR markdown, test directories).

**Cleanup after analysis:**
```bash
git --git-dir=/data/repos/{org_id}/{repo_id}.git \
  worktree remove /data/repos/{org_id}/worktrees/{run_id} --force
```

### 4.4 Locking Avoidance

Git uses internal lock files. Dangerous operations:
- `git fetch` writes to the bare repo (ref updates, object downloads)
- `git worktree add` modifies the bare repo's worktree metadata

Both are serialized by the job queue (one repo = one job chain). Fetch happens in the clone stage, worktree creation in the extract stage. These are sequential stages in the same job group. No race conditions possible.

### 4.5 Disk Budgeting

At 300 repos: average bare repo ~200MB. Total: ~60GB persistent. Worktrees are ephemeral and share objects with bare repos (10-20% overhead while active). A 200GB volume handles this.

Weekly cleanup job: remove bare clones for repos inactive >30 days.

### 4.6 Multi-Machine Scaling (Future)

At single-machine scale (up to ~1000 repos): all storage on one volume.

At multi-machine scale: repo affinity via consistent hashing on repo_id. Each worker machine "owns" a set of repos with bare clones on local disk. Job queue routes jobs to the assigned machine. This avoids shared network storage (NFS + git is slow).

Not needed until well past Phase 4.

---

## 5. Metrics Engine

### 5.1 Metric Types

All metrics are computed by Kotlin from raw extractions. Python never computes business metrics.

**Code Churn (per module, per week)**
- Definition: (lines added + lines deleted) per week, normalized by module size
- Source: commit extractions, files_changed field
- Risk signal: churn increasing over time without corresponding test changes
- Computation: aggregate file-level diffs by module_path prefix, group by week

**Bus Factor (per module)**
- Definition: minimum number of contributors whose departure would leave a module with no active maintainer
- Source: commit extractions, author_email field, filtered to last 90 days
- Risk signal: bus factor of 1 for any module in a critical path
- Computation: count distinct authors per module in the last 90 days, weighted by recency

**Hotspot Detection (per file)**
- Definition: files changed frequently by many different authors in a short period
- Source: commit extractions
- Risk signal: files with high change frequency + high author count + low test coverage
- Computation: rank files by (change_count * unique_authors) over last 30 days

**Dependency Staleness (per dependency)**
- Definition: how outdated each dependency is, weighted by known vulnerabilities
- Source: dependency extractions
- Risk signal: dependencies with known CVEs, or >12 months behind latest
- Computation: days since last update, CVE count from public vulnerability databases

**Test Coverage Trend (per module)**
- Definition: ratio of test file changes to source file changes over time
- Source: commit extractions, using test file detection heuristics
- Risk signal: decreasing ratio over time (test erosion)
- Computation: classify files as test/source by path conventions, compute ratio per week

**PR Cycle Time (per repo, optional -- requires GitHub API)**
- Definition: average time from PR open to merge
- Source: GitHub API PR metadata
- Risk signal: increasing cycle time suggests review bottlenecks or scope creep
- Computation: median open-to-merge duration, grouped by week

### 5.2 Module Detection

Modules are detected automatically from directory structure. First-level directories under src/ (or the repo root if no src/) are treated as modules. This can be overridden via a .autopsy.yml config file in the repo root:

```yaml
modules:
  - path: src/payments
    name: Payments Service
  - path: src/auth
    name: Authentication
  - path: lib/shared
    name: Shared Libraries
    critical: true  # flags this module for stricter thresholds
```

### 5.3 Health Score

Each repo gets an aggregate health score (0-100) computed as a weighted average:

- Code churn trend: 25% (penalize increasing churn)
- Bus factor: 25% (penalize modules with factor < 2)
- Dependency health: 20% (penalize CVEs and staleness)
- Test coverage trend: 20% (penalize declining ratio)
- Hotspot concentration: 10% (penalize high-churn files)

Thresholds are configurable per org in later phases. Phase 1 uses sensible defaults.

---

## 6. AI Report Generation

### 6.1 Report Structure

Each report is a markdown document with the following sections:

1. **Executive Summary** -- 2-3 sentences. Overall health, biggest change since last report, top risk.
2. **Risk Alerts** -- Specific, actionable risks with evidence. Each alert cites commits, files, or metrics.
3. **Module Health** -- Per-module breakdown. Churn trend, ownership, test health.
4. **Dependency Status** -- Outdated dependencies, CVEs, recommended updates.
5. **Positive Signals** -- What's going well. Test coverage improving, churn decreasing, etc.
6. **Recommendations** -- Prioritized list of suggested actions.

### 6.2 Prompt Architecture

The LLM does not perform the analysis. It narrates analysis that the metrics engine has already computed and connects it to project context retrieved via RAG.

**System prompt:**
```
You are Autopsy, a codebase health analyst. You produce weekly health reports
for engineering teams. Your reports are evidence-based: every claim must cite
specific metrics, commits, files, or documentation.

Rules:
- Never speculate. If the data doesn't support a conclusion, say so.
- Be specific. "The payments module is risky" is bad.
  "The payments module had 47 commits from 3 authors in 2 weeks with
  no corresponding test changes" is good.
- Be actionable. Every risk should include a suggested next step.
- Be concise. Engineering leads read this on Monday morning. Respect their time.
- Acknowledge positive trends. Not every report should be doom and gloom.
```

**User prompt construction:**
```
Here are the computed metrics for repository "{repo_name}" for the period
{start_date} to {end_date}:

## Metrics
{structured_metrics_json}

## Retrieved Context
The following documentation and PR descriptions are relevant to the modules
analyzed:
{retrieved_chunks_with_sources}

## Previous Report Summary (if available)
{previous_report_summary}

Generate a health report following the standard structure.
```

### 6.3 RAG Strategy

**Chunking:** Documents (ADRs, READMEs, PR descriptions, CHANGELOG entries) are split by section headers. Each chunk includes the document title and path as metadata. Fixed-size fallback (512 tokens with 50 token overlap) for documents without clear section structure.

**Embedding model:** OpenAI text-embedding-3-small (1536 dimensions). Chosen for cost efficiency at scale. Can be swapped for local embeddings (Ollama) for the open source CLI version.

**Retrieval:** For each module mentioned in the metrics, retrieve the top 3 most relevant chunks by cosine similarity. This grounds the AI report in the project's own documentation.

**Hybrid search (Phase 2+):** Combine vector similarity with PostgreSQL full-text search. Vector search catches semantic relevance ("auth migration" matches "authentication overhaul"). Full-text search catches exact matches (specific function names, error codes).

### 6.4 Cost Estimates

Per report: ~4-8K input tokens (metrics + context), ~1-2K output tokens.
At 300 reports/week: ~2-3M tokens/week.
With Claude Sonnet or GPT-4o: ~$15-30/week in API costs.

Embedding: ~500-2000 chunks per repo. At 300 repos: 150K-600K embedding calls/week. With OpenAI embeddings: negligible cost (<$1/week).

---

## 7. Job Pipeline

### 7.1 Pipeline Stages

```
┌─────────────────────────────────────────────────┐
│                   Scheduler                      │
│  Cron: checks which repos need analysis          │
│  Respects per-org schedules (weekly/daily)       │
│  Enqueues jobs with priority (Pro > Free)        │
└──────────────────┬──────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────┐
│              Job Queue (BullMQ / Redis)          │
│                                                  │
│  repo:clone     -> clone or fetch the repo       │
│  repo:extract   -> parse git history + PR data   │
│  repo:metrics   -> compute health metrics        │
│  repo:embed     -> generate embeddings for docs  │
│  repo:report    -> AI-generated health report    │
│  repo:deliver   -> email / Slack / dashboard     │
└──────────────────┬──────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────┐
│              Worker Pool                         │
│                                                  │
│  Clone Workers (2-3)    -> I/O bound, network    │
│  Extract Workers (4-6)  -> CPU bound, parsing    │
│  Metrics Workers (4-6)  -> CPU bound, computation│
│  Embed Workers (2-3)    -> API bound, rate limits│
│  Report Workers (2-3)   -> API bound, LLM calls  │
│  Deliver Workers (2)    -> I/O bound, email/Slack│
└─────────────────────────────────────────────────┘
```

### 7.2 Why Separate Stages

**Failure isolation:** If embedding generation fails (OpenAI API down), don't re-clone and re-parse. Retry from the embed stage. Expensive work is preserved.

**Different scaling profiles:** Clone workers are network-bound. Extract workers are CPU-bound. Embed and report workers are API-bound with external rate limits. Scale each independently.

**Progress visibility:** See exactly where every repo is in the pipeline. "147 repos extracted, 89 metrics computed, 43 reports generated, 12 delivered."

**Backpressure:** If the LLM API slows down, report jobs queue up but clone/extract/metrics keep running. Pipeline doesn't stall.

### 7.3 Serialization

One repo = one job chain. No parallelism within a repo. Full parallelism across repos.

```
repo_A:clone -> repo_A:extract -> repo_A:metrics -> repo_A:report
repo_B:clone -> repo_B:extract -> repo_B:metrics -> repo_B:report
repo_C:clone -> repo_C:extract -> repo_C:metrics -> repo_C:report
     ^              ^               ^               ^
     └── run in parallel across repos ──────────────┘
```

Enforced via BullMQ group ID per repo. This eliminates all git lock file concerns without any application-level locking.

### 7.4 Rate Limits

**GitHub API:** 5,000 req/hour (personal token), 15,000 (GitHub App). Each repo analysis needs ~10-20 API calls. 300 repos/week = ~6,000 calls/week. Fine with a GitHub App.

**LLM API:** 300 reports/week at 4-8K input tokens each. Retry with exponential backoff. Cache structured metrics so reports can be regenerated without re-running the pipeline.

**Embedding API:** 150K-600K calls/week at 300 repos. Negligible rate limit concern with OpenAI.

---

## 8. API Surface

### 8.1 Endpoints

```
Authentication:
POST   /auth/github                         -> GitHub OAuth callback

Repositories:
GET    /repos                                -> list repos for current org
POST   /repos                                -> connect a repo
DELETE /repos/:id                            -> disconnect repo
POST   /repos/:id/analyze                    -> trigger manual analysis

Analysis:
GET    /repos/:id/runs                       -> list analysis runs
GET    /runs/:id                             -> run detail with stage progress

Metrics:
GET    /repos/:id/metrics                    -> latest metrics summary
GET    /repos/:id/metrics/:type              -> specific metric over time
GET    /repos/:id/modules/:path/metrics      -> all metrics for a module

Reports:
GET    /repos/:id/reports                    -> list reports
GET    /reports/:id                          -> full report markdown

Dashboard:
GET    /dashboard                            -> org-wide health summary
GET    /dashboard/risks                      -> top risks across all repos

Query (RAG):
POST   /repos/:id/query                     -> ask a question about the repo

Billing:
GET    /billing                              -> current plan and usage
POST   /billing/checkout                     -> Stripe checkout session
POST   /webhooks/stripe                      -> Stripe webhook handler
```

---

## 9. Product Tiers

### 9.1 Pricing

**Free:** 1 repo, weekly email report. No dashboard, no history. Acquisition channel.

**Pro ($49/month per org):** Up to 10 repos. Full dashboard with historical trends. Slack integration. Module drill-down. Dependency vulnerability alerts.

**Team ($149/month per org):** Unlimited repos. Cross-repo analysis. Custom report scheduling. GitHub/GitLab PR comments with health impact warnings. API access. Priority analysis queue.

### 9.2 Limits Enforcement

Repo count enforced at the API layer (POST /repos checks org.repo_limit). Analysis priority enforced at the queue layer (Pro/Team jobs have higher priority than Free).

---

## 10. Go-to-Market

### 10.1 Phased Rollout

**Phase 1 (weeks 1-4):** Open source CLI. `autopsy analyze /path/to/repo > report.md`. Post on Hacker News, Reddit r/programming. Goal: get the tool in engineers' hands, collect feedback. The CLI is the distribution engine.

**Phase 2 (weeks 5-8):** Hosted version. GitHub OAuth, connect repos, weekly email reports. Free tier only. Landing page with waitlist. Conversion: CLI user likes the report, wants it automated.

**Phase 3 (weeks 9-12):** Dashboard, Slack integration, historical trends. Launch Pro tier. First paying customers from free tier users who want more depth. 20 customers at $49/month = $1K MRR.

**Phase 4 (ongoing):** Iterate on customer feedback. Team tier. Cross-repo analysis. PR comments. Enterprise features as demand warrants.

### 10.2 Content Strategy

Launch blog post: "Your codebase is trying to tell you something." Covers metrics that predict tech debt, why existing tools miss the human dimension, and how grounding AI analysis in repo context produces insights generic tools can't. Target: Hacker News front page, engineering manager shares.

Ongoing: one technical blog post per significant feature. Each post demonstrates engineering depth and serves as organic marketing.

---

## 11. Code Structure

### 11.1 Python Engine

```
autopsy-engine/
├── autopsy/
│   ├── __init__.py
│   ├── worker.py                -> Redis queue consumer, routes jobs
│   │
│   ├── git/
│   │   ├── __init__.py
│   │   ├── cloner.py            -> clone/fetch logic, bare repos
│   │   ├── extractor.py         -> git log parsing, diff stats
│   │   ├── worktree.py          -> worktree create/cleanup
│   │   └── github_client.py     -> PR metadata, issue threads
│   │
│   ├── parsing/
│   │   ├── __init__.py
│   │   ├── dependencies.py      -> package.json, build.gradle, requirements.txt
│   │   ├── docs.py              -> ADR, README, CHANGELOG detection
│   │   └── test_detector.py     -> identifies test files by convention
│   │
│   ├── ai/
│   │   ├── __init__.py
│   │   ├── embedder.py          -> chunk docs + generate embeddings
│   │   ├── chunker.py           -> splitting strategies
│   │   ├── retriever.py         -> pgvector similarity search
│   │   ├── reporter.py          -> builds prompt, calls LLM, formats report
│   │   └── prompts/
│   │       ├── health_report.py -> system prompt for weekly report
│   │       └── module_deep_dive.py
│   │
│   ├── storage/
│   │   ├── __init__.py
│   │   ├── db.py                -> PostgreSQL connection (psycopg3)
│   │   └── models.py            -> dataclasses matching DB schema
│   │
│   └── config.py                -> env vars, paths, API keys
│
├── tests/
│   ├── fixtures/                -> small git repos for testing
│   ├── test_extractor.py
│   ├── test_chunker.py
│   └── test_reporter.py
│
├── pyproject.toml
└── Dockerfile
```

### 11.2 Kotlin API

```
autopsy-api/
├── src/main/kotlin/dev/autopsy/
│   ├── Application.kt
│   │
│   ├── config/
│   │   ├── RedisConfig.kt
│   │   ├── SecurityConfig.kt
│   │   └── MultiTenantConfig.kt
│   │
│   ├── tenant/
│   │   ├── Organization.kt
│   │   ├── OrganizationRepository.kt
│   │   ├── TenantContext.kt
│   │   └── TenantInterceptor.kt
│   │
│   ├── repository/
│   │   ├── Repository.kt
│   │   ├── RepositoryService.kt
│   │   ├── RepositoryController.kt
│   │   └── GitHubIntegration.kt
│   │
│   ├── analysis/
│   │   ├── AnalysisRun.kt
│   │   ├── AnalysisOrchestrator.kt
│   │   ├── AnalysisScheduler.kt
│   │   └── AnalysisStatus.kt
│   │
│   ├── metrics/
│   │   ├── Metric.kt
│   │   ├── MetricType.kt
│   │   ├── MetricsComputer.kt
│   │   ├── ChurnCalculator.kt
│   │   ├── BusFactorCalculator.kt
│   │   ├── HotspotDetector.kt
│   │   ├── DependencyStaleness.kt
│   │   └── TestCoverageTracker.kt
│   │
│   ├── report/
│   │   ├── Report.kt
│   │   ├── ReportService.kt
│   │   ├── ReportDelivery.kt
│   │   └── ReportController.kt
│   │
│   ├── queue/
│   │   ├── JobPublisher.kt
│   │   ├── JobConsumer.kt
│   │   ├── JobPayload.kt
│   │   └── JobGroup.kt
│   │
│   ├── billing/
│   │   ├── Plan.kt
│   │   ├── BillingService.kt
│   │   └── StripeWebhookController.kt
│   │
│   └── dashboard/
│       ├── DashboardController.kt
│       ├── ModuleHealthDto.kt
│       └── TrendDto.kt
│
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       ├── V001__organizations.sql
│       ├── V002__repositories.sql
│       ├── V003__analysis_runs.sql
│       ├── V004__metrics.sql
│       ├── V005__chunks.sql
│       └── V006__reports.sql
│
└── build.gradle.kts
```

### 11.3 Next.js Dashboard

```
autopsy-web/
├── src/
│   ├── app/
│   │   ├── layout.tsx
│   │   ├── page.tsx                    -> dashboard home
│   │   ├── repos/
│   │   │   ├── [id]/
│   │   │   │   ├── page.tsx            -> repo detail (report + metrics)
│   │   │   │   ├── settings/page.tsx   -> repo settings
│   │   │   │   └── query/page.tsx      -> RAG chat interface
│   │   ├── settings/page.tsx           -> org settings + billing
│   │   └── auth/callback/page.tsx      -> GitHub OAuth
│   │
│   ├── components/
│   │   ├── health-score.tsx            -> circular progress indicator
│   │   ├── metric-card.tsx             -> chart wrapper
│   │   ├── report-viewer.tsx           -> markdown renderer
│   │   ├── repo-card.tsx               -> dashboard grid item
│   │   └── chat-message.tsx            -> query interface message
│   │
│   └── lib/
│       ├── api.ts                      -> API client
│       └── types.ts                    -> shared types
│
├── package.json
└── Dockerfile
```

---

## 12. Infrastructure

### 12.1 Phase 1 (CLI)

No infrastructure. Runs on the user's machine. Python + Kotlin JARs.

### 12.2 Phase 2-3 (Single Machine SaaS)

One VPS: 8 CPU, 16GB RAM, 100GB SSD (Hetzner or Fly.io, ~$50-80/month).

Runs everything: Redis, PostgreSQL (with pgvector), all Python workers, Kotlin API, Next.js dashboard.

Docker Compose for deployment. Caddy or Traefik for HTTPS.

Break-even at 2 Pro customers ($49/month each).

### 12.3 Scaling Inflection Points

**300 repos/week:** Single machine. Easy.

**1,000 repos/week:** Separate workers onto their own instances. Database stays on one machine. Add PgBouncer for connection pooling.

**5,000+ repos/week:** Dedicated queue infrastructure, read replicas for dashboard queries, possibly dedicated vector DB. Fundraising territory.

---

## 13. Build Order

### Phase 1: CLI (Weeks 1-4)

**Week 1-2:**
- Python: git clone, git log parsing, diff stat extraction, dependency file parsing, test file detection
- Kotlin: churn calculator, bus factor calculator, hotspot detector, dependency staleness scorer
- Integration: Kotlin calls Python via subprocess, receives JSON, computes metrics, outputs markdown report to stdout

**Week 3-4:**
- Python: doc detection, chunking, embedding generation, LLM report generation
- Kotlin: health score computation, report formatting
- CLI: `autopsy analyze /path/to/repo > report.md`
- Write README, example report output, blog post draft

### Phase 2: Pipeline (Weeks 5-8)

- Add PostgreSQL schema and migrations
- Add Redis queue with BullMQ
- Implement job stages with serialization per repo
- Implement bare repo persistence and incremental fetch
- Support multiple repos in a single run
- Add worktree management

### Phase 3: SaaS (Weeks 9-12)

- Kotlin: REST API, GitHub OAuth, scheduling
- Next.js: dashboard, repo detail, report viewer, metrics charts
- Deployment: Docker Compose, VPS, HTTPS

### Phase 4: Monetization (Weeks 13+)

- Stripe integration, plan enforcement
- Email report delivery (Resend/SendGrid)
- Slack integration (webhook-based)
- Team management, org settings
- PR comment integration via GitHub App

---

## 14. Open Questions

- **Local LLM support for CLI:** Should Phase 1 support Ollama for users who don't want to use OpenAI/Anthropic APIs? Adds complexity but makes the open source story stronger.
- **Monorepo detection:** How to handle monorepos where module boundaries aren't obvious from directory structure? The .autopsy.yml config file is the escape hatch, but auto-detection would be better.
- **Language-aware analysis:** Should the tool understand programming language semantics (e.g., knowing that a .kt file is Kotlin, understanding import graphs)? Adds significant complexity. Start with file-path heuristics, add language awareness later if demand exists.
- **GitLab/Bitbucket support:** Phase 1 works with any git repo. GitHub API integration for PR data is Phase 2+. GitLab and Bitbucket API support should follow based on customer demand.
- **Self-hosted option:** Some enterprise customers will want to run Autopsy on their own infrastructure. The Docker Compose setup naturally supports this. Formalize it as a product tier later.
