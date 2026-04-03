"""Slack delivery via incoming webhook."""

from __future__ import annotations

import json
import urllib.request
from urllib.error import URLError

from autopsy.delivery.email_sender import DeliveryError


def send_report_to_slack(
    webhook_url: str,
    repo_name: str,
    report_summary: str | None,
    risk_score: float | None,
    report_url: str,
) -> bool:
    """Post a health report summary to Slack via incoming webhook.

    Returns True on success. Raises DeliveryError on failure.
    """
    payload = _build_block_kit_payload(repo_name, report_summary, risk_score, report_url)

    try:
        data = json.dumps(payload).encode("utf-8")
        req = urllib.request.Request(
            webhook_url,
            data=data,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=10) as response:
            if response.status != 200:
                msg = f"Slack webhook returned status {response.status}"
                raise DeliveryError(msg)
            return True
    except URLError as e:
        msg = f"Failed to post to Slack webhook: {e}"
        raise DeliveryError(msg) from e


def _build_block_kit_payload(
    repo_name: str,
    summary: str | None,
    risk_score: float | None,
    report_url: str,
) -> dict[str, object]:
    """Build Slack Block Kit message payload."""
    summary_text = summary or "Health report generated."

    return {
        "blocks": [
            {
                "type": "header",
                "text": {"type": "plain_text", "text": f"Autopsy: {repo_name}"},
            },
            {
                "type": "section",
                "text": {"type": "mrkdwn", "text": summary_text},
            },
            {"type": "divider"},
            {
                "type": "actions",
                "elements": [
                    {
                        "type": "button",
                        "text": {"type": "plain_text", "text": "View Full Report"},
                        "url": report_url,
                        "style": "primary",
                    },
                ],
            },
        ],
    }
