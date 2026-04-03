package dev.autopsy.db.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("extractions")
data class Extraction(
    @Id val id: UUID? = null,
    val runId: UUID,
    val dataType: String,
    val modulePath: String? = null,
    val payload: String,
    val createdAt: Instant? = null,
)
