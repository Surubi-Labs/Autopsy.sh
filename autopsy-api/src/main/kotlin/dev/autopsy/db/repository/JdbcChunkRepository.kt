package dev.autopsy.db.repository

import dev.autopsy.db.entity.ChunkEntity
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JdbcChunkRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) : ChunkRepository {

    override fun findByRepoId(repoId: UUID): List<ChunkEntity> {
        val sql = "SELECT id, repo_id, source_type, source_ref, content, created_at FROM chunks WHERE repo_id = :repoId"
        return jdbc.query(sql, MapSqlParameterSource("repoId", repoId)) { rs, _ -> ChunkEntity.fromRow(rs) }
    }

    override fun deleteByRepoId(repoId: UUID): Int {
        val sql = "DELETE FROM chunks WHERE repo_id = :repoId"
        return jdbc.update(sql, MapSqlParameterSource("repoId", repoId))
    }

    override fun save(repoId: UUID, sourceType: String, sourceRef: String, content: String): ChunkEntity {
        val sql = """
            INSERT INTO chunks (repo_id, source_type, source_ref, content)
            VALUES (:repoId, :sourceType, :sourceRef, :content)
            RETURNING id, repo_id, source_type, source_ref, content, created_at
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("repoId", repoId)
            .addValue("sourceType", sourceType)
            .addValue("sourceRef", sourceRef)
            .addValue("content", content)
        return jdbc.queryForObject(sql, params) { rs, _ -> ChunkEntity.fromRow(rs) }!!
    }
}
