package dev.autopsy.db.entity

import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

data class ChunkEntity(
    val id: UUID,
    val repoId: UUID,
    val sourceType: String,
    val sourceRef: String,
    val content: String,
    val createdAt: Instant,
) {
    companion object {
        fun fromRow(rs: ResultSet): ChunkEntity = ChunkEntity(
            id = rs.getObject("id", UUID::class.java),
            repoId = rs.getObject("repo_id", UUID::class.java),
            sourceType = rs.getString("source_type"),
            sourceRef = rs.getString("source_ref"),
            content = rs.getString("content"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
    }
}
