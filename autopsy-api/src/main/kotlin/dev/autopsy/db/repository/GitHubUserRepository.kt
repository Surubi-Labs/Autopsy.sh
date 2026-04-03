package dev.autopsy.db.repository

import dev.autopsy.db.entity.GitHubUser
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface GitHubUserRepository : CrudRepository<GitHubUser, UUID> {
    fun findByGithubUserId(githubUserId: Long): GitHubUser?

    @Modifying
    @Query("UPDATE github_users SET access_token = :accessToken, updated_at = now() WHERE github_user_id = :githubUserId")
    fun updateAccessToken(githubUserId: Long, accessToken: String)
}
