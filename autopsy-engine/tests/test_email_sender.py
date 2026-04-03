"""Tests for email delivery via Resend."""

from __future__ import annotations

import os
from unittest.mock import MagicMock, patch

import pytest

from autopsy.delivery.email_sender import DeliveryError, _build_email_html, send_report_email


class TestBuildEmailHtml:
    def test_includes_repo_name(self) -> None:
        html = _build_email_html("my-repo", "All good", 0.15, "https://app.com/report/1")
        assert "my-repo" in html

    def test_includes_report_url(self) -> None:
        html = _build_email_html("repo", None, None, "https://app.com/report/1")
        assert "https://app.com/report/1" in html

    def test_includes_summary_when_present(self) -> None:
        html = _build_email_html("repo", "Health is stable", 0.2, "https://example.com")
        assert "Health is stable" in html

    def test_fallback_when_no_summary(self) -> None:
        html = _build_email_html("repo", None, None, "https://example.com")
        assert "Report generated successfully" in html


class TestSendReportEmail:
    def test_missing_api_key_raises(self) -> None:
        with (
            patch.dict(os.environ, {}, clear=True),
            pytest.raises(DeliveryError, match="RESEND_API_KEY"),
        ):
            send_report_email("a@b.com", "repo", "summary", 0.5, "https://example.com")

    @patch("autopsy.delivery.email_sender.resend")
    def test_successful_send(self, mock_resend: MagicMock) -> None:
        mock_resend.Emails.send.return_value = {"id": "email-123"}
        with patch.dict(os.environ, {"RESEND_API_KEY": "test-key"}):
            result = send_report_email("a@b.com", "repo", "ok", 0.1, "https://example.com")
        assert result == "email-123"
        mock_resend.Emails.send.assert_called_once()

    @patch("autopsy.delivery.email_sender.resend")
    def test_resend_error_raises_delivery_error(self, mock_resend: MagicMock) -> None:
        mock_resend.Emails.send.side_effect = Exception("API error")
        with (
            patch.dict(os.environ, {"RESEND_API_KEY": "test-key"}),
            pytest.raises(DeliveryError, match="Resend"),
        ):
            send_report_email("a@b.com", "repo", None, None, "https://example.com")
