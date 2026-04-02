package dev.autopsy.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${autopsy.jwt.secret}") private val secret: String,
    @Value("\${autopsy.jwt.expiration-hours}") private val expirationHours: Long,
) {

    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateToken(auth: AuthenticatedOrg): String {
        val now = Date()
        val expiry = Date(now.time + expirationHours * 3600 * 1000)

        return Jwts.builder()
            .subject(auth.orgId.toString())
            .claim("orgName", auth.orgName)
            .claim("githubUserId", auth.githubUserId)
            .claim("githubLogin", auth.githubLogin)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): AuthenticatedOrg? {
        return try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload

            AuthenticatedOrg(
                orgId = UUID.fromString(claims.subject),
                orgName = claims["orgName"] as? String ?: "",
                githubUserId = (claims["githubUserId"] as? Number)?.toLong() ?: 0L,
                githubLogin = claims["githubLogin"] as? String ?: "",
            )
        } catch (_: Exception) {
            null
        }
    }
}
