package dev.autopsy.db.repository

import dev.autopsy.db.entity.RepoEntity
import java.time.Instant
import java.util.UUID

interface RepoRepository {
    fun findById(id: UUID): RepoEntity?
    fun findByOrgId(orgId: UUID): List<RepoEntity>
    fun findActiveByOrgId(orgId: UUID): List<RepoEntity>
    fun save(orgId: UUID, name: String, gitUrl: String, defaultBranch: String = "main"): RepoEntity
    fun updateLastAnalyzed(id: UUID, sha: String, analyzedAt: Instant): Unit
    fun deleteById(id: UUID): Unit
    fun countByOrgId(orgId: UUID): Int
    fun findActiveBySchedule(schedule: String): List<RepoEntity>
}
