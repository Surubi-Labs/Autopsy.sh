package dev.autopsy.auth

import dev.autopsy.db.entity.Organization
import dev.autopsy.db.repository.GitHubUserRepository
import dev.autopsy.db.repository.OrganizationRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class GitHubOAuthService(
    @Value("\${autopsy.github.client-id}") private val clientId: String,
    @Value("\${autopsy.github.client-secret}") private val clientSecret: String,
    private val gitHubUserRepo: GitHubUserRepository,
    private val orgRepo: OrganizationRepository,
) {

    private val restClient = RestClient.create()

    fun authenticateOrRegister(code: String): AuthenticatedOrg {
        val accessToken = exchangeCode(code)
        val ghUser = getUserInfo(accessToken)

        val existing = gitHubUserRepo.findByGithubUserId(ghUser.id)

        if (existing != null) {
            gitHubUserRepo.updateAccessToken(ghUser.id, accessToken)
            val org = orgRepo.findByIdOrNull(existing.orgId)
                ?: throw GitHubAuthException("Organization not found for user ${ghUser.login}")
            return AuthenticatedOrg(
                orgId = requireNotNull(org.id) { "Organization ID must not be null" },
                orgName = org.name,
                githubUserId = ghUser.id,
                githubLogin = ghUser.login,
            )
        }

        // New user — create org and github_user
        val org = orgRepo.save(Organization(name = ghUser.name ?: ghUser.login))
        val orgId = requireNotNull(org.id) { "Organization ID must not be null after save" }
        gitHubUserRepo.save(
            dev.autopsy.db.entity.GitHubUser(
                githubUserId = ghUser.id,
                githubLogin = ghUser.login,
                orgId = orgId,
                accessToken = accessToken,
            ),
        )

        return AuthenticatedOrg(
            orgId = orgId,
            orgName = org.name,
            githubUserId = ghUser.id,
            githubLogin = ghUser.login,
        )
    }

    internal fun exchangeCode(code: String): String {
        val response = restClient.post()
            .uri("https://github.com/login/oauth/access_token")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(mapOf("client_id" to clientId, "client_secret" to clientSecret, "code" to code))
            .retrieve()
            .body(GitHubTokenResponse::class.java)
            ?: throw GitHubAuthException("Failed to exchange code for access token")

        if (response.accessToken.isNullOrBlank()) {
            throw GitHubAuthException("GitHub returned empty access token")
        }

        return response.accessToken
    }

    internal fun getUserInfo(accessToken: String): GitHubUser {
        return restClient.get()
            .uri("https://api.github.com/user")
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/vnd.github+json")
            .retrieve()
            .body(GitHubUser::class.java)
            ?: throw GitHubAuthException("Failed to fetch GitHub user info")
    }
}

data class GitHubTokenResponse(
    @com.fasterxml.jackson.annotation.JsonProperty("access_token")
    val accessToken: String? = null,
    @com.fasterxml.jackson.annotation.JsonProperty("token_type")
    val tokenType: String? = null,
    val scope: String? = null,
)

data class GitHubUser(
    val id: Long = 0,
    val login: String = "",
    val name: String? = null,
)

class GitHubAuthException(message: String) : RuntimeException(message)
