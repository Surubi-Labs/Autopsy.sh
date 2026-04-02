package dev.autopsy.db.repository

import dev.autopsy.db.entity.GitHubUser
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JdbcGitHubUserRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) : GitHubUserRepository {

    override fun findByGithubUserId(githubUserId: Long): GitHubUser? {
        val sql = "SELECT * FROM github_users WHERE github_user_id = :githubUserId"
        return jdbc.query(sql, MapSqlParameterSource("githubUserId", githubUserId)) { rs, _ ->
            GitHubUser.fromRow(rs)
        }.firstOrNull()
    }

    override fun save(githubUserId: Long, githubLogin: String, orgId: UUID, accessToken: String?): GitHubUser {
        val sql = """
            INSERT INTO github_users (github_user_id, github_login, org_id, access_token)
            VALUES (:githubUserId, :githubLogin, :orgId, :accessToken)
            RETURNING *
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("githubUserId", githubUserId)
            .addValue("githubLogin", githubLogin)
            .addValue("orgId", orgId)
            .addValue("accessToken", accessToken)
        return jdbc.queryForObject(sql, params) { rs, _ -> GitHubUser.fromRow(rs) }!!
    }

    override fun updateAccessToken(githubUserId: Long, accessToken: String) {
        val sql = """
            UPDATE github_users SET access_token = :token, updated_at = now()
            WHERE github_user_id = :githubUserId
        """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("githubUserId", githubUserId)
                .addValue("token", accessToken),
        )
    }
}
