package dev.autopsy.db.repository

import dev.autopsy.db.entity.GitHubUser
import java.util.UUID

interface GitHubUserRepository {
    fun findByGithubUserId(githubUserId: Long): GitHubUser?
    fun save(githubUserId: Long, githubLogin: String, orgId: UUID, accessToken: String?): GitHubUser
    fun updateAccessToken(githubUserId: Long, accessToken: String): Unit
}
