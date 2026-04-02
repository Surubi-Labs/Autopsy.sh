package dev.autopsy.db.repository

import dev.autopsy.db.entity.Report
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JdbcReportRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) : ReportRepository {

    override fun findById(id: UUID): Report? {
        val sql = "SELECT * FROM reports WHERE id = :id"
        return jdbc.query(sql, MapSqlParameterSource("id", id)) { rs, _ -> Report.fromRow(rs) }.firstOrNull()
    }

    override fun findByRepoId(repoId: UUID): List<Report> {
        val sql = "SELECT * FROM reports WHERE repo_id = :repoId ORDER BY created_at DESC"
        return jdbc.query(sql, MapSqlParameterSource("repoId", repoId)) { rs, _ -> Report.fromRow(rs) }
    }

    override fun findLatestByRepoId(repoId: UUID): Report? {
        val sql = "SELECT * FROM reports WHERE repo_id = :repoId ORDER BY created_at DESC LIMIT 1"
        return jdbc.query(sql, MapSqlParameterSource("repoId", repoId)) { rs, _ -> Report.fromRow(rs) }.firstOrNull()
    }

    override fun save(runId: UUID, repoId: UUID, markdown: String, summary: String?, riskScore: Double?): Report {
        val sql = """
            INSERT INTO reports (run_id, repo_id, markdown, summary, risk_score)
            VALUES (:runId, :repoId, :markdown, :summary, :riskScore)
            RETURNING *
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("runId", runId)
            .addValue("repoId", repoId)
            .addValue("markdown", markdown)
            .addValue("summary", summary)
            .addValue("riskScore", riskScore)
        return jdbc.queryForObject(sql, params) { rs, _ -> Report.fromRow(rs) }!!
    }

    override fun findByRepoIdPaginated(repoId: UUID, limit: Int, offset: Int): List<Report> {
        val sql = "SELECT * FROM reports WHERE repo_id = :repoId ORDER BY created_at DESC LIMIT :limit OFFSET :offset"
        val params = MapSqlParameterSource()
            .addValue("repoId", repoId).addValue("limit", limit).addValue("offset", offset)
        return jdbc.query(sql, params) { rs, _ -> Report.fromRow(rs) }
    }

    override fun countByRepoId(repoId: UUID): Int {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM reports WHERE repo_id = :repoId",
            MapSqlParameterSource("repoId", repoId), Int::class.java,
        ) ?: 0
    }
}
