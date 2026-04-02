package dev.autopsy.db.entity

import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

data class Report(
    val id: UUID,
    val runId: UUID,
    val repoId: UUID,
    val markdown: String,
    val summary: String?,
    val riskScore: Double?,
    val deliveredAt: Instant?,
    val deliveryChannel: String?,
    val createdAt: Instant,
) {
    companion object {
        fun fromRow(rs: ResultSet): Report = Report(
            id = rs.getObject("id", UUID::class.java),
            runId = rs.getObject("run_id", UUID::class.java),
            repoId = rs.getObject("repo_id", UUID::class.java),
            markdown = rs.getString("markdown"),
            summary = rs.getString("summary"),
            riskScore = rs.getObject("risk_score") as? Double,
            deliveredAt = rs.getTimestamp("delivered_at")?.toInstant(),
            deliveryChannel = rs.getString("delivery_channel"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
    }
}
