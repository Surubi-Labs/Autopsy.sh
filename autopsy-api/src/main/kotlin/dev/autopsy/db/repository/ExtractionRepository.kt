package dev.autopsy.db.repository

import dev.autopsy.db.entity.Extraction
import java.util.UUID

interface ExtractionRepository {
    fun findByRunId(runId: UUID): List<Extraction>
    fun findByRunIdAndType(runId: UUID, dataType: String): List<Extraction>
    fun save(runId: UUID, dataType: String, modulePath: String?, payload: String): Extraction
    fun bulkSave(runId: UUID, extractions: List<BulkExtraction>): Unit
}

data class BulkExtraction(
    val dataType: String,
    val modulePath: String?,
    val payload: String,
)
