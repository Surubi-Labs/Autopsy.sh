package dev.autopsy.db.entity

import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

data class GitHubUser(
    val id: UUID,
    val githubUserId: Long,
    val githubLogin: String,
    val orgId: UUID,
    val accessToken: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun fromRow(rs: ResultSet): GitHubUser = GitHubUser(
            id = rs.getObject("id", UUID::class.java),
            githubUserId = rs.getLong("github_user_id"),
            githubLogin = rs.getString("github_login"),
            orgId = rs.getObject("org_id", UUID::class.java),
            accessToken = rs.getString("access_token"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            updatedAt = rs.getTimestamp("updated_at").toInstant(),
        )
    }
}
