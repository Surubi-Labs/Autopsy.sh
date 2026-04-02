"""Tests for Redis job queue producer."""

from __future__ import annotations

from unittest.mock import MagicMock

from autopsy.queue.producer import _STAGE_ORDER, enqueue_next_stage, enqueue_stage


class TestStageProgression:
    def test_clone_to_extract(self) -> None:
        client = MagicMock()
        client.xadd.return_value = b"1234-0"
        result = enqueue_next_stage(client, "r", "rp", "o", "CLONE")
        assert result == "1234-0"
        call_args = client.xadd.call_args
        assert call_args[0][1]["stage"] == "EXTRACT"

    def test_extract_to_metrics(self) -> None:
        client = MagicMock()
        client.xadd.return_value = b"1234-0"
        result = enqueue_next_stage(client, "r", "rp", "o", "EXTRACT")
        assert result is not None
        assert client.xadd.call_args[0][1]["stage"] == "METRICS"

    def test_deliver_has_no_next(self) -> None:
        client = MagicMock()
        result = enqueue_next_stage(client, "r", "rp", "o", "DELIVER")
        assert result is None
        client.xadd.assert_not_called()

    def test_invalid_stage_returns_none(self) -> None:
        client = MagicMock()
        result = enqueue_next_stage(client, "r", "rp", "o", "BOGUS")
        assert result is None

    def test_full_stage_order(self) -> None:
        assert _STAGE_ORDER == ["CLONE", "EXTRACT", "METRICS", "EMBED", "REPORT", "DELIVER"]

    def test_case_insensitive(self) -> None:
        client = MagicMock()
        client.xadd.return_value = b"1234-0"
        result = enqueue_next_stage(client, "r", "rp", "o", "clone")
        assert result is not None

    def test_payload_included(self) -> None:
        client = MagicMock()
        client.xadd.return_value = b"1234-0"
        enqueue_next_stage(
            client, "r", "rp", "o", "CLONE",
            payload={"commitRange": "abc..def"},
        )
        fields = client.xadd.call_args[0][1]
        assert "payload" in fields


class TestEnqueueStage:
    def test_enqueues_specific_stage(self) -> None:
        client = MagicMock()
        client.xadd.return_value = b"5678-0"
        result = enqueue_stage(client, "r", "rp", "o", "EMBED")
        assert result == "5678-0"
        assert client.xadd.call_args[0][1]["stage"] == "EMBED"
