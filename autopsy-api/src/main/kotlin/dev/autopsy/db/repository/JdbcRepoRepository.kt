package dev.autopsy.db.repository

import dev.autopsy.db.entity.RepoEntity
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Repository
class JdbcRepoRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) : RepoRepository {

    override fun findById(id: UUID): RepoEntity? {
        val sql = "SELECT * FROM repositories WHERE id = :id"
        return jdbc.query(sql, MapSqlParameterSource("id", id)) { rs, _ -> RepoEntity.fromRow(rs) }.firstOrNull()
    }

    override fun findByOrgId(orgId: UUID): List<RepoEntity> {
        val sql = "SELECT * FROM repositories WHERE org_id = :orgId ORDER BY name"
        return jdbc.query(sql, MapSqlParameterSource("orgId", orgId)) { rs, _ -> RepoEntity.fromRow(rs) }
    }

    override fun findActiveByOrgId(orgId: UUID): List<RepoEntity> {
        val sql = "SELECT * FROM repositories WHERE org_id = :orgId AND active = true ORDER BY name"
        return jdbc.query(sql, MapSqlParameterSource("orgId", orgId)) { rs, _ -> RepoEntity.fromRow(rs) }
    }

    override fun save(orgId: UUID, name: String, gitUrl: String, defaultBranch: String): RepoEntity {
        val sql = """
            INSERT INTO repositories (org_id, name, git_url, default_branch)
            VALUES (:orgId, :name, :gitUrl, :defaultBranch)
            RETURNING *
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("orgId", orgId)
            .addValue("name", name)
            .addValue("gitUrl", gitUrl)
            .addValue("defaultBranch", defaultBranch)
        return jdbc.queryForObject(sql, params) { rs, _ -> RepoEntity.fromRow(rs) }!!
    }

    override fun updateLastAnalyzed(id: UUID, sha: String, analyzedAt: Instant) {
        val sql = """
            UPDATE repositories SET last_analyzed_sha = :sha, last_analyzed_at = :analyzedAt WHERE id = :id
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("id", id)
            .addValue("sha", sha)
            .addValue("analyzedAt", Timestamp.from(analyzedAt))
        jdbc.update(sql, params)
    }

    override fun deleteById(id: UUID) {
        jdbc.update("DELETE FROM repositories WHERE id = :id", MapSqlParameterSource("id", id))
    }

    override fun countByOrgId(orgId: UUID): Int {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM repositories WHERE org_id = :orgId",
            MapSqlParameterSource("orgId", orgId),
            Int::class.java,
        ) ?: 0
    }

    override fun findActiveBySchedule(schedule: String): List<RepoEntity> {
        val sql = "SELECT * FROM repositories WHERE active = true AND schedule = :schedule"
        return jdbc.query(sql, MapSqlParameterSource("schedule", schedule)) { rs, _ -> RepoEntity.fromRow(rs) }
    }
}
