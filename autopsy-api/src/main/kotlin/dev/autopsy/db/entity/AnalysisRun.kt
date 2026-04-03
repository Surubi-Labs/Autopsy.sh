package dev.autopsy.db.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("analysis_runs")
data class AnalysisRun(
    @Id val id: UUID? = null,
    val repoId: UUID,
    val status: String = "queued",
    val currentStage: String? = null,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val error: String? = null,
    val commitRange: String? = null,
    val createdAt: Instant? = null,
)
