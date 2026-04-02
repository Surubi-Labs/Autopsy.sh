package dev.autopsy.db.entity

import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

data class RepoEntity(
    val id: UUID,
    val orgId: UUID,
    val name: String,
    val gitUrl: String,
    val defaultBranch: String,
    val githubInstallId: Long?,
    val lastAnalyzedSha: String?,
    val lastAnalyzedAt: Instant?,
    val schedule: String,
    val active: Boolean,
    val createdAt: Instant,
) {
    companion object {
        fun fromRow(rs: ResultSet): RepoEntity = RepoEntity(
            id = rs.getObject("id", UUID::class.java),
            orgId = rs.getObject("org_id", UUID::class.java),
            name = rs.getString("name"),
            gitUrl = rs.getString("git_url"),
            defaultBranch = rs.getString("default_branch"),
            githubInstallId = rs.getObject("github_install_id") as? Long,
            lastAnalyzedSha = rs.getString("last_analyzed_sha"),
            lastAnalyzedAt = rs.getTimestamp("last_analyzed_at")?.toInstant(),
            schedule = rs.getString("schedule"),
            active = rs.getBoolean("active"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
    }
}
