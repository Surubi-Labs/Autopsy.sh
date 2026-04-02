package dev.autopsy.db.repository

import dev.autopsy.db.entity.Extraction
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JdbcExtractionRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) : ExtractionRepository {

    override fun findByRunId(runId: UUID): List<Extraction> {
        val sql = "SELECT * FROM extractions WHERE run_id = :runId ORDER BY created_at"
        return jdbc.query(sql, MapSqlParameterSource("runId", runId)) { rs, _ -> Extraction.fromRow(rs) }
    }

    override fun findByRunIdAndType(runId: UUID, dataType: String): List<Extraction> {
        val sql = "SELECT * FROM extractions WHERE run_id = :runId AND data_type = :dataType ORDER BY created_at"
        val params = MapSqlParameterSource().addValue("runId", runId).addValue("dataType", dataType)
        return jdbc.query(sql, params) { rs, _ -> Extraction.fromRow(rs) }
    }

    override fun save(runId: UUID, dataType: String, modulePath: String?, payload: String): Extraction {
        val sql = """
            INSERT INTO extractions (run_id, data_type, module_path, payload)
            VALUES (:runId, :dataType, :modulePath, :payload::jsonb)
            RETURNING *
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("runId", runId)
            .addValue("dataType", dataType)
            .addValue("modulePath", modulePath)
            .addValue("payload", jsonbParam(payload))
        return jdbc.queryForObject(sql, params) { rs, _ -> Extraction.fromRow(rs) }!!
    }

    override fun bulkSave(runId: UUID, extractions: List<BulkExtraction>) {
        val sql = """
            INSERT INTO extractions (run_id, data_type, module_path, payload)
            VALUES (:runId, :dataType, :modulePath, :payload::jsonb)
        """.trimIndent()
        val batchParams = extractions.map { ext ->
            MapSqlParameterSource()
                .addValue("runId", runId)
                .addValue("dataType", ext.dataType)
                .addValue("modulePath", ext.modulePath)
                .addValue("payload", jsonbParam(ext.payload))
        }.toTypedArray()
        jdbc.batchUpdate(sql, batchParams)
    }

    private fun jsonbParam(json: String): PGobject =
        PGobject().apply {
            type = "jsonb"
            value = json
        }
}
