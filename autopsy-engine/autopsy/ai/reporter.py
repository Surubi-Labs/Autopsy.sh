"""LLM report generator — builds prompt and calls OpenAI API."""

from __future__ import annotations

from typing import TYPE_CHECKING, Any

from autopsy.ai.prompts.health_report import SYSTEM_PROMPT, build_user_prompt

if TYPE_CHECKING:
    from autopsy.ai.models import Chunk

_DEFAULT_MODEL = "gpt-4o"


def generate_report(
    client: Any,
    metrics_json: str,
    chunks: list[Chunk],
    previous_summary: str | None = None,
    model: str = _DEFAULT_MODEL,
) -> str:
    """Generate an AI-powered health report.

    Args:
        client: OpenAI client instance.
        metrics_json: JSON string of computed metrics.
        chunks: Document chunks for context.
        previous_summary: Summary from the previous report.
        model: LLM model to use.

    Returns:
        Markdown string of the generated report.
    """
    context_text = _format_context(chunks)
    user_prompt = build_user_prompt(metrics_json, context_text, previous_summary)

    response = client.chat.completions.create(
        model=model,
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt},
        ],
        temperature=0.3,
    )

    content: str = response.choices[0].message.content
    return content


def _format_context(chunks: list[Chunk]) -> str:
    """Format chunks into a context string for the prompt."""
    if not chunks:
        return ""

    parts: list[str] = []
    for chunk in chunks:
        parts.append(f"[{chunk.source_type}: {chunk.source_ref}]")
        parts.append(chunk.content)
        parts.append("")

    return "\n".join(parts)
