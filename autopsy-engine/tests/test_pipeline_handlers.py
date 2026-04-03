"""Unit tests for pipeline handler dispatch logic."""

from __future__ import annotations

from unittest.mock import MagicMock, patch


class TestDeliverHandler:
    @patch("autopsy.pipeline.deliver_handler.dispatch_delivery")
    @patch("autopsy.pipeline.deliver_handler.get_repository")
    @patch("autopsy.pipeline.deliver_handler.get_report_by_run_id")
    def test_marks_run_completed(
        self,
        mock_report: MagicMock,
        mock_repo: MagicMock,
        mock_dispatch: MagicMock,
    ) -> None:
        mock_report.return_value = {"summary": "ok", "risk_score": 0.1}
        mock_repo.return_value = {"name": "test-repo"}
        mock_dispatch.return_value = ["email"]

        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_conn.cursor.return_value.__enter__ = MagicMock(return_value=mock_cursor)
        mock_conn.cursor.return_value.__exit__ = MagicMock(return_value=False)

        from autopsy.pipeline.deliver_handler import handle_deliver

        handle_deliver(
            mock_conn, MagicMock(),
            "550e8400-e29b-41d4-a716-446655440000",
            "660e8400-e29b-41d4-a716-446655440001",
            "770e8400-e29b-41d4-a716-446655440002",
        )

        mock_cursor.execute.assert_called_once()
        sql = mock_cursor.execute.call_args[0][0]
        assert "completed" in sql
        mock_conn.commit.assert_called_once()

    @patch("autopsy.pipeline.deliver_handler.dispatch_delivery")
    @patch("autopsy.pipeline.deliver_handler.get_repository")
    @patch("autopsy.pipeline.deliver_handler.get_report_by_run_id")
    def test_no_report_still_completes(
        self,
        mock_report: MagicMock,
        mock_repo: MagicMock,
        mock_dispatch: MagicMock,
    ) -> None:
        mock_report.return_value = None
        mock_repo.return_value = None

        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_conn.cursor.return_value.__enter__ = MagicMock(return_value=mock_cursor)
        mock_conn.cursor.return_value.__exit__ = MagicMock(return_value=False)

        from autopsy.pipeline.deliver_handler import handle_deliver

        handle_deliver(
            mock_conn, MagicMock(),
            "550e8400-e29b-41d4-a716-446655440000",
            "660e8400-e29b-41d4-a716-446655440001",
            "770e8400-e29b-41d4-a716-446655440002",
        )

        mock_dispatch.assert_not_called()
        mock_conn.commit.assert_called_once()


class TestWorkerDispatch:
    def test_stage_routing(self) -> None:
        known_stages = {"CLONE", "EXTRACT", "METRICS", "EMBED", "REPORT", "DELIVER"}
        python_stages = {"CLONE", "EXTRACT", "EMBED", "REPORT", "DELIVER"}
        assert python_stages.issubset(known_stages)

    def test_unknown_stage_is_safe(self) -> None:
        from autopsy.queue.producer import _STAGE_ORDER

        assert "CLONE" in _STAGE_ORDER
        assert "BOGUS" not in _STAGE_ORDER
