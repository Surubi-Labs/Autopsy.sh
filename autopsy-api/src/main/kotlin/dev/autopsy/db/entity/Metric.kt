package dev.autopsy.db.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("metrics")
data class Metric(
    @Id val id: UUID? = null,
    val runId: UUID,
    val repoId: UUID,
    val modulePath: String? = null,
    val metricType: String,
    val value: Double,
    val details: String? = null,
    val computedAt: Instant? = null,
)
