package dev.autopsy.db.entity

import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

data class Metric(
    val id: UUID,
    val runId: UUID,
    val repoId: UUID,
    val modulePath: String?,
    val metricType: String,
    val value: Double,
    val details: String?,
    val computedAt: Instant,
) {
    companion object {
        fun fromRow(rs: ResultSet): Metric = Metric(
            id = rs.getObject("id", UUID::class.java),
            runId = rs.getObject("run_id", UUID::class.java),
            repoId = rs.getObject("repo_id", UUID::class.java),
            modulePath = rs.getString("module_path"),
            metricType = rs.getString("metric_type"),
            value = rs.getDouble("value"),
            details = rs.getString("details"),
            computedAt = rs.getTimestamp("computed_at").toInstant(),
        )
    }
}
