"""Tests for delivery dispatcher."""

from __future__ import annotations

import os
from unittest.mock import MagicMock, patch

from autopsy.delivery.dispatcher import dispatch_delivery
from autopsy.delivery.email_sender import DeliveryError

_ORG = "550e8400-e29b-41d4-a716-446655440000"
_RUN = "660e8400-e29b-41d4-a716-446655440001"
_URL = "https://app.com"


class TestDispatchDelivery:
    def _mock_conn_with_prefs(
        self, email: str | None = None, slack: str | None = None,
    ) -> MagicMock:
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_conn.cursor.return_value.__enter__ = MagicMock(return_value=mock_cursor)
        mock_conn.cursor.return_value.__exit__ = MagicMock(return_value=False)
        mock_cursor.fetchone.return_value = (email, slack, "pro")
        return mock_conn

    @patch("autopsy.delivery.dispatcher.send_report_email")
    @patch("autopsy.delivery.dispatcher.send_report_to_slack")
    def test_both_channels_configured(
        self, mock_slack: MagicMock, mock_email: MagicMock,
    ) -> None:
        conn = self._mock_conn_with_prefs("a@b.com", "https://hooks.slack.com/T/B/X")
        with patch.dict(os.environ, {"RESEND_API_KEY": "key"}):
            channels = dispatch_delivery(
                conn, _ORG, _RUN, "repo", "summary", 0.2, _URL,
            )
        assert "email" in channels
        assert "slack" in channels

    @patch("autopsy.delivery.dispatcher.send_report_email")
    def test_email_only(self, mock_email: MagicMock) -> None:
        conn = self._mock_conn_with_prefs("a@b.com", None)
        with patch.dict(os.environ, {"RESEND_API_KEY": "key"}):
            channels = dispatch_delivery(
                conn, _ORG, _RUN, "repo", None, None, _URL,
            )
        assert channels == ["email"]

    @patch("autopsy.delivery.dispatcher.send_report_to_slack")
    def test_slack_only(self, mock_slack: MagicMock) -> None:
        conn = self._mock_conn_with_prefs(None, "https://hooks.slack.com/T/B/X")
        channels = dispatch_delivery(
            conn, _ORG, _RUN, "repo", None, None, _URL,
        )
        assert channels == ["slack"]

    def test_neither_configured(self) -> None:
        conn = self._mock_conn_with_prefs(None, None)
        channels = dispatch_delivery(
            conn, _ORG, _RUN, "repo", None, None, _URL,
        )
        assert channels == []

    @patch("autopsy.delivery.dispatcher.send_report_email")
    @patch("autopsy.delivery.dispatcher.send_report_to_slack")
    def test_email_fails_slack_succeeds(
        self, mock_slack: MagicMock, mock_email: MagicMock,
    ) -> None:
        mock_email.side_effect = DeliveryError("fail")
        conn = self._mock_conn_with_prefs("a@b.com", "https://hooks.slack.com/T/B/X")
        with patch.dict(os.environ, {"RESEND_API_KEY": "key"}):
            channels = dispatch_delivery(
                conn, _ORG, _RUN, "repo", None, None, _URL,
            )
        assert channels == ["slack"]

    def test_org_not_found(self) -> None:
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_conn.cursor.return_value.__enter__ = MagicMock(return_value=mock_cursor)
        mock_conn.cursor.return_value.__exit__ = MagicMock(return_value=False)
        mock_cursor.fetchone.return_value = None
        channels = dispatch_delivery(
            mock_conn, _ORG, _RUN, "repo", None, None, _URL,
        )
        assert channels == []
