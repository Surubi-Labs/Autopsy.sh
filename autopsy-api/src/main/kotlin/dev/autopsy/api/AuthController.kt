package dev.autopsy.api

import dev.autopsy.api.dto.AuthResponse
import dev.autopsy.api.dto.GitHubAuthRequest
import dev.autopsy.auth.GitHubAuthException
import dev.autopsy.auth.GitHubOAuthService
import dev.autopsy.auth.JwtService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val githubService: GitHubOAuthService,
    private val jwtService: JwtService,
) {

    @PostMapping("/github")
    fun githubAuth(@RequestBody request: GitHubAuthRequest): ResponseEntity<AuthResponse> {
        if (request.code.isBlank()) {
            return ResponseEntity.badRequest().build()
        }

        return try {
            val auth = githubService.authenticateOrRegister(request.code)
            val token = jwtService.generateToken(auth)
            ResponseEntity.ok(AuthResponse(token, auth.orgId, auth.orgName))
        } catch (e: GitHubAuthException) {
            ResponseEntity.status(401).build()
        }
    }
}
