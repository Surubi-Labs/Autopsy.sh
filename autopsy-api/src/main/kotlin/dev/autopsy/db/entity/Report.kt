package dev.autopsy.db.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("reports")
data class Report(
    @Id val id: UUID? = null,
    val runId: UUID,
    val repoId: UUID,
    val markdown: String,
    val summary: String? = null,
    val riskScore: Double? = null,
    val deliveredAt: Instant? = null,
    val deliveryChannel: String? = null,
    val createdAt: Instant? = null,
)
