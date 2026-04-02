"""Prompt templates for health report generation."""

from __future__ import annotations

SYSTEM_PROMPT = """You are Autopsy, a codebase health analyst. You produce weekly health reports
for engineering teams. Your reports are evidence-based: every claim must cite
specific metrics, commits, files, or documentation.

Rules:
- Never speculate. If the data doesn't support a conclusion, say so.
- Be specific. "The payments module is risky" is bad.
  "The payments module had 47 commits from 3 authors in 2 weeks with
  no corresponding test changes" is good.
- Be actionable. Every risk should include a suggested next step.
- Be concise. Engineering leads read this on Monday morning. Respect their time.
- Acknowledge positive trends. Not every report should be doom and gloom."""


def build_user_prompt(
    metrics_json: str,
    context_text: str,
    previous_summary: str | None = None,
) -> str:
    """Build the user prompt for report generation."""
    parts = [
        "Here are the computed metrics for the repository:",
        "",
        "## Metrics",
        metrics_json,
        "",
        "## Retrieved Context",
        "The following documentation and PR descriptions are relevant:",
        context_text if context_text else "(No context available)",
    ]

    if previous_summary:
        parts.extend([
            "",
            "## Previous Report Summary",
            previous_summary,
        ])

    parts.extend([
        "",
        "Generate a health report with these sections:",
        "1. Executive Summary (2-3 sentences)",
        "2. Risk Alerts (specific, actionable, with evidence)",
        "3. Module Health (per-module breakdown)",
        "4. Dependency Status",
        "5. Positive Signals",
        "6. Recommendations (prioritized)",
    ])

    return "\n".join(parts)
