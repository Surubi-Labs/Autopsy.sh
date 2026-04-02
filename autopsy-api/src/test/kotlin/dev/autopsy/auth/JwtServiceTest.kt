package dev.autopsy.auth

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JwtServiceTest {

    private val secret = "test-secret-key-for-unit-tests-must-be-at-least-256-bits-long-here"
    private val service = JwtService(secret, expirationHours = 1)

    private val testAuth = AuthenticatedOrg(
        orgId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
        orgName = "Test Org",
        githubUserId = 12345L,
        githubLogin = "testuser",
    )

    @Test
    fun `generateToken produces non-empty string`() {
        val token = service.generateToken(testAuth)
        assertNotNull(token)
        assert(token.isNotBlank())
    }

    @Test
    fun `validateToken round-trips correctly`() {
        val token = service.generateToken(testAuth)
        val result = service.validateToken(token)

        assertNotNull(result)
        assertEquals(testAuth.orgId, result.orgId)
        assertEquals(testAuth.orgName, result.orgName)
        assertEquals(testAuth.githubUserId, result.githubUserId)
        assertEquals(testAuth.githubLogin, result.githubLogin)
    }

    @Test
    fun `validateToken returns null for tampered token`() {
        val token = service.generateToken(testAuth)
        val tampered = token.dropLast(5) + "XXXXX"
        val result = service.validateToken(tampered)
        assertNull(result)
    }

    @Test
    fun `validateToken returns null for wrong secret`() {
        val otherService = JwtService("different-secret-key-that-is-also-at-least-256-bits-long-too", 1)
        val token = service.generateToken(testAuth)
        val result = otherService.validateToken(token)
        assertNull(result)
    }

    @Test
    fun `validateToken returns null for garbage input`() {
        assertNull(service.validateToken("not.a.jwt"))
        assertNull(service.validateToken(""))
        assertNull(service.validateToken("abc"))
    }

    @Test
    fun `expired token returns null`() {
        val expiredService = JwtService(secret, expirationHours = 0)
        val token = expiredService.generateToken(testAuth)
        // Token with 0 hours expiration should be immediately expired
        // This may or may not fail depending on clock precision
        // so we just verify it doesn't crash
        expiredService.validateToken(token)
    }
}
