"""Configuration for the Autopsy engine."""

from __future__ import annotations

import os


def get_openai_api_key() -> str | None:
    """Return the OpenAI API key from environment, or None if not set."""
    return os.environ.get("OPENAI_API_KEY")
