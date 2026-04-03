"""Delivery dispatcher — orchestrates sending reports across channels."""

from __future__ import annotations

import logging
import os
from typing import Any
from uuid import UUID

from autopsy.db.repositories import (
    get_org_delivery_preferences,
    update_report_delivery,
)
from autopsy.delivery.email_sender import DeliveryError, send_report_email
from autopsy.delivery.slack_sender import send_report_to_slack

logger = logging.getLogger(__name__)


def dispatch_delivery(
    conn: Any,
    org_id: str,
    run_id: str,
    repo_name: str,
    report_summary: str | None,
    risk_score: float | None,
    dashboard_base_url: str | None = None,
) -> list[str]:
    """Dispatch report delivery to configured channels.

    Returns list of successful channel names (e.g., ["email", "slack"]).
    Individual channel failures don't block other channels.
    """
    base_url = dashboard_base_url or os.environ.get(
        "DASHBOARD_BASE_URL", "http://localhost:3000"
    )
    report_url = f"{base_url}/reports/{run_id}"

    prefs = get_org_delivery_preferences(conn, UUID(org_id))
    if not prefs:
        logger.warning("No org found for delivery: %s", org_id)
        return []

    channels: list[str] = []

    # Email delivery
    email = prefs.get("email_address")
    if email and os.environ.get("RESEND_API_KEY"):
        try:
            send_report_email(email, repo_name, report_summary, risk_score, report_url)
            channels.append("email")
            logger.info("Email sent to %s for run %s", email, run_id)
        except DeliveryError:
            logger.exception("Email delivery failed for run %s", run_id)

    # Slack delivery
    slack_url = prefs.get("slack_webhook_url")
    if slack_url:
        try:
            send_report_to_slack(slack_url, repo_name, report_summary, risk_score, report_url)
            channels.append("slack")
            logger.info("Slack message sent for run %s", run_id)
        except DeliveryError:
            logger.exception("Slack delivery failed for run %s", run_id)

    # Update delivery status
    if channels:
        update_report_delivery(conn, UUID(run_id), ",".join(channels))

    return channels
