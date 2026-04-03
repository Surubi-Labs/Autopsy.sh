package dev.autopsy.db.repository

import dev.autopsy.db.entity.RepoEntity
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.time.Instant
import java.util.UUID

interface RepoRepository : CrudRepository<RepoEntity, UUID> {

    @Query("SELECT * FROM repositories WHERE org_id = :orgId ORDER BY name")
    fun findByOrgId(orgId: UUID): List<RepoEntity>

    @Query("SELECT * FROM repositories WHERE org_id = :orgId AND active = true ORDER BY name")
    fun findActiveByOrgId(orgId: UUID): List<RepoEntity>

    @Modifying
    @Query("UPDATE repositories SET last_analyzed_sha = :sha, last_analyzed_at = :analyzedAt WHERE id = :id")
    fun updateLastAnalyzed(id: UUID, sha: String, analyzedAt: Instant)

    @Query("SELECT COUNT(*) FROM repositories WHERE org_id = :orgId")
    fun countByOrgId(orgId: UUID): Int

    @Query("SELECT * FROM repositories WHERE active = true AND schedule = :schedule")
    fun findActiveBySchedule(schedule: String): List<RepoEntity>
}
