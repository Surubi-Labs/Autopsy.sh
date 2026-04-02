# Autopsy

Codebase health monitoring tool that analyzes git repositories and generates AI-powered health reports for engineering teams.

Autopsy examines commit patterns, code churn, ownership concentration, dependency staleness, and test coverage trends, then produces narrative reports grounded in the repository's own documentation and decision history.

## Prerequisites

- Python 3.11+
- JDK 21+
- [uv](https://docs.astral.sh/uv/) (recommended for Python management)
- OpenAI API key (optional, for AI-enhanced reports)

## Quick Start

```bash
# Clone the repository
git clone https://github.com/Surubi-Labs/Autopsy.sh.git
cd Autopsy.sh

# Install Python dependencies
cd autopsy-engine
uv sync
cd ..

# Build the Kotlin API
cd autopsy-api
./gradlew build
cd ..

# Run analysis (metrics-only, no AI)
cd autopsy-api
./gradlew run --args="analyze /path/to/your/repo --no-ai"

# Run with AI-enhanced report (requires OpenAI API key)
export OPENAI_API_KEY="sk-..."
./gradlew run --args="analyze /path/to/your/repo"
```

## Architecture

```
autopsy-engine/ (Python)          autopsy-api/ (Kotlin)
┌─────────────────────┐          ┌──────────────────────┐
│ Git log parsing      │          │ Churn calculator      │
│ Dependency scanning  │  JSON    │ Bus factor calculator │
│ Test file detection  │ ──────> │ Hotspot detector      │
│ Doc detection        │          │ Dep staleness scorer  │
│ Doc chunking         │          │ Test coverage tracker │
│ Embedding generation │          │ Health score computer │
│ LLM report generation│ <────── │ Report formatter      │
└─────────────────────┘  JSON    │ CLI orchestrator      │
                                  └──────────────────────┘
```

**Python** handles everything that touches git, files, and AI APIs.
**Kotlin** handles metrics computation (pure functions) and CLI orchestration.

## Report Sections

1. **Health Score** - Overall 0-100 score with component breakdown
2. **Module Health** - Per-module bus factor, churn, and ownership
3. **Code Churn Trends** - Weekly additions/deletions per module
4. **Ownership & Bus Factor** - Risk assessment per module
5. **Hotspots** - Files with high change frequency across many authors
6. **Dependencies** - Staleness scoring and CVE tracking
7. **Test Coverage Trends** - Weekly test/source file change ratios

With `--no-ai` flag, you get a structured metrics report.
Without it, an AI model narrates the analysis with evidence-based insights.

## Development

### Python Engine

```bash
cd autopsy-engine
uv sync                  # install dependencies
make test                # run tests
make lint                # run ruff
make typecheck           # run mypy
```

### Kotlin API

```bash
cd autopsy-api
./gradlew test           # run tests
./gradlew build          # build
```

## License

MIT
