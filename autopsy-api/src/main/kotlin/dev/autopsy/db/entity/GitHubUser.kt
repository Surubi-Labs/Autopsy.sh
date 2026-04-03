package dev.autopsy.db.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("github_users")
data class GitHubUser(
    @Id val id: UUID? = null,
    val githubUserId: Long,
    val githubLogin: String,
    val orgId: UUID,
    val accessToken: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)
