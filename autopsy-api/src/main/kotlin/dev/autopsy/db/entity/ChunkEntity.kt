package dev.autopsy.db.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("chunks")
data class ChunkEntity(
    @Id val id: UUID? = null,
    val repoId: UUID,
    val sourceType: String,
    val sourceRef: String,
    val content: String,
    val createdAt: Instant? = null,
)
