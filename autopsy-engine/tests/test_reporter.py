"""Tests for LLM report generator."""

from __future__ import annotations

from unittest.mock import MagicMock

from autopsy.ai.models import Chunk
from autopsy.ai.prompts.health_report import SYSTEM_PROMPT, build_user_prompt
from autopsy.ai.reporter import generate_report


class TestBuildUserPrompt:
    def test_includes_metrics(self) -> None:
        prompt = build_user_prompt('{"score": 72}', "some context")
        assert '{"score": 72}' in prompt

    def test_includes_context(self) -> None:
        prompt = build_user_prompt("{}", "ADR: Use PostgreSQL")
        assert "ADR: Use PostgreSQL" in prompt

    def test_includes_previous_summary_when_provided(self) -> None:
        prompt = build_user_prompt("{}", "", previous_summary="Previous score was 65")
        assert "Previous score was 65" in prompt

    def test_omits_previous_summary_when_none(self) -> None:
        prompt = build_user_prompt("{}", "")
        assert "Previous Report" not in prompt


class TestSystemPrompt:
    def test_system_prompt_contains_key_rules(self) -> None:
        assert "evidence-based" in SYSTEM_PROMPT
        assert "Never speculate" in SYSTEM_PROMPT
        assert "actionable" in SYSTEM_PROMPT


class TestGenerateReport:
    def test_calls_openai_and_returns_markdown(self) -> None:
        mock_client = MagicMock()
        mock_response = MagicMock()
        mock_response.choices = [
            MagicMock(message=MagicMock(content="# Health Report\n\nAll good."))
        ]
        mock_client.chat.completions.create.return_value = mock_response

        result = generate_report(
            client=mock_client,
            metrics_json='{"score": 72}',
            chunks=[Chunk("readme", "README.md", "project info", "meta")],
        )

        assert "Health Report" in result
        mock_client.chat.completions.create.assert_called_once()

    def test_passes_system_prompt(self) -> None:
        mock_client = MagicMock()
        mock_response = MagicMock()
        mock_response.choices = [
            MagicMock(message=MagicMock(content="report"))
        ]
        mock_client.chat.completions.create.return_value = mock_response

        generate_report(mock_client, "{}", [])

        call_args = mock_client.chat.completions.create.call_args
        messages = call_args.kwargs.get("messages", call_args[1].get("messages", []))
        system_msg = next(m for m in messages if m["role"] == "system")
        assert "evidence-based" in system_msg["content"]

    def test_includes_previous_summary(self) -> None:
        mock_client = MagicMock()
        mock_response = MagicMock()
        mock_response.choices = [MagicMock(message=MagicMock(content="report"))]
        mock_client.chat.completions.create.return_value = mock_response

        generate_report(mock_client, "{}", [], previous_summary="Score was 65")

        call_args = mock_client.chat.completions.create.call_args
        messages = call_args.kwargs.get("messages", call_args[1].get("messages", []))
        user_msg = next(m for m in messages if m["role"] == "user")
        assert "Score was 65" in user_msg["content"]
