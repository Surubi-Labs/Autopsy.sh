package dev.autopsy.db.repository

import dev.autopsy.db.entity.Metric
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JdbcMetricRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) : MetricRepository {

    override fun findByRunId(runId: UUID): List<Metric> {
        val sql = "SELECT * FROM metrics WHERE run_id = :runId ORDER BY computed_at"
        return jdbc.query(sql, MapSqlParameterSource("runId", runId)) { rs, _ -> Metric.fromRow(rs) }
    }

    override fun findByRepoId(repoId: UUID): List<Metric> {
        val sql = "SELECT * FROM metrics WHERE repo_id = :repoId ORDER BY computed_at DESC"
        return jdbc.query(sql, MapSqlParameterSource("repoId", repoId)) { rs, _ -> Metric.fromRow(rs) }
    }

    override fun findByRepoIdAndType(repoId: UUID, metricType: String): List<Metric> {
        val sql = "SELECT * FROM metrics WHERE repo_id = :repoId AND metric_type = :type ORDER BY computed_at DESC"
        val params = MapSqlParameterSource().addValue("repoId", repoId).addValue("type", metricType)
        return jdbc.query(sql, params) { rs, _ -> Metric.fromRow(rs) }
    }

    override fun save(
        runId: UUID,
        repoId: UUID,
        modulePath: String?,
        metricType: String,
        value: Double,
        details: String?,
    ): Metric {
        val sql = """
            INSERT INTO metrics (run_id, repo_id, module_path, metric_type, value, details)
            VALUES (:runId, :repoId, :modulePath, :metricType, :value, :details::jsonb)
            RETURNING *
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("runId", runId)
            .addValue("repoId", repoId)
            .addValue("modulePath", modulePath)
            .addValue("metricType", metricType)
            .addValue("value", value)
            .addValue("details", details?.let { jsonbParam(it) })
        return jdbc.queryForObject(sql, params) { rs, _ -> Metric.fromRow(rs) }!!
    }

    private fun jsonbParam(json: String): PGobject =
        PGobject().apply {
            type = "jsonb"
            this.value = json
        }
}
