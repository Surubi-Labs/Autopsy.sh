"""Tests for Slack webhook delivery."""

from __future__ import annotations

import json
from unittest.mock import MagicMock, patch

import pytest

from autopsy.delivery.email_sender import DeliveryError
from autopsy.delivery.slack_sender import _build_block_kit_payload, send_report_to_slack


class TestBuildBlockKitPayload:
    def test_includes_repo_name(self) -> None:
        payload = _build_block_kit_payload("my-repo", "Summary", 0.2, "https://example.com")
        header = payload["blocks"][0]
        assert "my-repo" in header["text"]["text"]  # type: ignore[index]

    def test_includes_report_url(self) -> None:
        payload = _build_block_kit_payload("repo", "ok", 0.1, "https://example.com/r/1")
        actions = payload["blocks"][3]
        button = actions["elements"][0]  # type: ignore[index]
        assert button["url"] == "https://example.com/r/1"

    def test_valid_json_serialization(self) -> None:
        payload = _build_block_kit_payload("repo", "ok", None, "https://example.com")
        json_str = json.dumps(payload)
        assert json.loads(json_str) == payload


class TestSendReportToSlack:
    @patch("autopsy.delivery.slack_sender.urllib.request.urlopen")
    def test_successful_post(self, mock_urlopen: MagicMock) -> None:
        mock_response = MagicMock()
        mock_response.status = 200
        mock_response.__enter__ = MagicMock(return_value=mock_response)
        mock_response.__exit__ = MagicMock(return_value=False)
        mock_urlopen.return_value = mock_response

        result = send_report_to_slack(
            "https://hooks.slack.com/services/T/B/X",
            "repo", "ok", 0.1, "https://example.com",
        )
        assert result is True
        mock_urlopen.assert_called_once()

    @patch("autopsy.delivery.slack_sender.urllib.request.urlopen")
    def test_non_200_raises(self, mock_urlopen: MagicMock) -> None:
        mock_response = MagicMock()
        mock_response.status = 500
        mock_response.__enter__ = MagicMock(return_value=mock_response)
        mock_response.__exit__ = MagicMock(return_value=False)
        mock_urlopen.return_value = mock_response

        with pytest.raises(DeliveryError, match="status 500"):
            send_report_to_slack(
                "https://hooks.slack.com/services/T/B/X",
                "repo", None, None, "https://example.com",
            )
