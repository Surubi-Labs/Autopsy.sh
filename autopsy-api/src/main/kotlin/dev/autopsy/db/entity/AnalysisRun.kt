package dev.autopsy.db.entity

import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

data class AnalysisRun(
    val id: UUID,
    val repoId: UUID,
    val status: String,
    val currentStage: String?,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val error: String?,
    val commitRange: String?,
    val createdAt: Instant,
) {
    companion object {
        fun fromRow(rs: ResultSet): AnalysisRun = AnalysisRun(
            id = rs.getObject("id", UUID::class.java),
            repoId = rs.getObject("repo_id", UUID::class.java),
            status = rs.getString("status"),
            currentStage = rs.getString("current_stage"),
            startedAt = rs.getTimestamp("started_at")?.toInstant(),
            completedAt = rs.getTimestamp("completed_at")?.toInstant(),
            error = rs.getString("error"),
            commitRange = rs.getString("commit_range"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
    }
}
