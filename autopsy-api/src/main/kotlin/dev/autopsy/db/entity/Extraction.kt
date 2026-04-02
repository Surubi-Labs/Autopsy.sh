package dev.autopsy.db.entity

import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

data class Extraction(
    val id: UUID,
    val runId: UUID,
    val dataType: String,
    val modulePath: String?,
    val payload: String,
    val createdAt: Instant,
) {
    companion object {
        fun fromRow(rs: ResultSet): Extraction = Extraction(
            id = rs.getObject("id", UUID::class.java),
            runId = rs.getObject("run_id", UUID::class.java),
            dataType = rs.getString("data_type"),
            modulePath = rs.getString("module_path"),
            payload = rs.getString("payload"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
    }
}
