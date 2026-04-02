package dev.autopsy.auth

import java.util.UUID

data class AuthenticatedOrg(
    val orgId: UUID,
    val orgName: String,
    val githubUserId: Long,
    val githubLogin: String,
)
