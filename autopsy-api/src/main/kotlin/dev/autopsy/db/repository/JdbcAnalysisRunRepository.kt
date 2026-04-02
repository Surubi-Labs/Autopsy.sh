package dev.autopsy.db.repository

import dev.autopsy.db.entity.AnalysisRun
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JdbcAnalysisRunRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) : AnalysisRunRepository {

    override fun findById(id: UUID): AnalysisRun? {
        val sql = "SELECT * FROM analysis_runs WHERE id = :id"
        return jdbc.query(sql, MapSqlParameterSource("id", id)) { rs, _ -> AnalysisRun.fromRow(rs) }.firstOrNull()
    }

    override fun findByRepoId(repoId: UUID): List<AnalysisRun> {
        val sql = "SELECT * FROM analysis_runs WHERE repo_id = :repoId ORDER BY created_at DESC"
        return jdbc.query(sql, MapSqlParameterSource("repoId", repoId)) { rs, _ -> AnalysisRun.fromRow(rs) }
    }

    override fun create(repoId: UUID): AnalysisRun {
        val sql = "INSERT INTO analysis_runs (repo_id) VALUES (:repoId) RETURNING *"
        return jdbc.queryForObject(sql, MapSqlParameterSource("repoId", repoId)) { rs, _ -> AnalysisRun.fromRow(rs) }!!
    }

    override fun updateStatus(id: UUID, status: String, stage: String?, error: String?) {
        val sql = """
            UPDATE analysis_runs
            SET status = :status, current_stage = :stage, error = :error,
                started_at = CASE WHEN started_at IS NULL AND :status = 'running' THEN now() ELSE started_at END
            WHERE id = :id
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("id", id)
            .addValue("status", status)
            .addValue("stage", stage)
            .addValue("error", error)
        jdbc.update(sql, params)
    }

    override fun updateCommitRange(id: UUID, commitRange: String) {
        val sql = "UPDATE analysis_runs SET commit_range = :range WHERE id = :id"
        jdbc.update(sql, MapSqlParameterSource().addValue("id", id).addValue("range", commitRange))
    }

    override fun markCompleted(id: UUID) {
        val sql = "UPDATE analysis_runs SET status = 'completed', completed_at = now() WHERE id = :id"
        jdbc.update(sql, MapSqlParameterSource("id", id))
    }

    override fun findByRepoIdPaginated(repoId: UUID, limit: Int, offset: Int): List<AnalysisRun> {
        val sql = "SELECT * FROM analysis_runs WHERE repo_id = :repoId ORDER BY created_at DESC LIMIT :limit OFFSET :offset"
        val params = MapSqlParameterSource()
            .addValue("repoId", repoId).addValue("limit", limit).addValue("offset", offset)
        return jdbc.query(sql, params) { rs, _ -> AnalysisRun.fromRow(rs) }
    }

    override fun countByRepoId(repoId: UUID): Int {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM analysis_runs WHERE repo_id = :repoId",
            MapSqlParameterSource("repoId", repoId), Int::class.java,
        ) ?: 0
    }
}
