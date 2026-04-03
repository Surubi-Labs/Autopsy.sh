package dev.autopsy.db.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("repositories")
data class RepoEntity(
    @Id val id: UUID? = null,
    val orgId: UUID,
    val name: String,
    val gitUrl: String,
    val defaultBranch: String = "main",
    val githubInstallId: Long? = null,
    val lastAnalyzedSha: String? = null,
    val lastAnalyzedAt: Instant? = null,
    val schedule: String = "weekly",
    val active: Boolean = true,
    val createdAt: Instant? = null,
)
