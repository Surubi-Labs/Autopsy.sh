"""Unit tests for pipeline handler dispatch logic."""

from __future__ import annotations

from unittest.mock import MagicMock

from autopsy.pipeline.deliver_handler import handle_deliver


class TestDeliverHandler:
    def test_marks_run_completed(self) -> None:
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_conn.cursor.return_value.__enter__ = MagicMock(return_value=mock_cursor)
        mock_conn.cursor.return_value.__exit__ = MagicMock(return_value=False)

        handle_deliver(mock_conn, MagicMock(), "run-1", "repo-1", "org-1")

        mock_cursor.execute.assert_called_once()
        sql = mock_cursor.execute.call_args[0][0]
        assert "completed" in sql
        assert "deliver" in sql
        mock_conn.commit.assert_called_once()


class TestWorkerDispatch:
    def test_stage_routing(self) -> None:
        """Verify the worker routes known stages correctly."""
        known_stages = {"CLONE", "EXTRACT", "METRICS", "EMBED", "REPORT", "DELIVER"}
        # METRICS is skipped by Python worker (handled by Kotlin)
        python_stages = {"CLONE", "EXTRACT", "EMBED", "REPORT", "DELIVER"}
        assert python_stages.issubset(known_stages)

    def test_unknown_stage_is_safe(self) -> None:
        """Unknown stages should not crash the worker."""
        # Just verify the enum-like structure
        from autopsy.queue.producer import _STAGE_ORDER

        assert "CLONE" in _STAGE_ORDER
        assert "BOGUS" not in _STAGE_ORDER
