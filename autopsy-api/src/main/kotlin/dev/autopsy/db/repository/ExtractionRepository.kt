package dev.autopsy.db.repository

import dev.autopsy.db.entity.Extraction
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface ExtractionRepository : CrudRepository<Extraction, UUID> {

    @Query("SELECT * FROM extractions WHERE run_id = :runId ORDER BY created_at")
    fun findByRunId(runId: UUID): List<Extraction>

    @Query("SELECT * FROM extractions WHERE run_id = :runId AND data_type = :dataType ORDER BY created_at")
    fun findByRunIdAndType(runId: UUID, dataType: String): List<Extraction>
}
