package dev.autopsy.queue

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for job producer logic.
 * Redis integration tests will use Testcontainers in the integration test suite.
 */
class JobProducerTest {

    @Test
    fun `stage progression CLONE to EXTRACT`() {
        assertEquals(Stage.EXTRACT, Stage.CLONE.next())
    }

    @Test
    fun `stage progression DELIVER has no next`() {
        assertNull(Stage.DELIVER.next())
    }

    @Test
    fun `job message toMap includes all fields`() {
        val msg = JobMessage("run-1", "repo-1", "org-1", Stage.CLONE)
        val map = msg.toMap()

        assertEquals("run-1", map["runId"])
        assertEquals("repo-1", map["repoId"])
        assertEquals("org-1", map["orgId"])
        assertEquals("CLONE", map["stage"])
    }

    @Test
    fun `job message with payload round-trips`() {
        val msg = JobMessage(
            "r", "rp", "o", Stage.EXTRACT,
            payload = mapOf("commitRange" to "abc..def"),
        )
        val restored = JobMessage.fromMap(msg.toMap())

        assertEquals("abc..def", restored.payload["commitRange"])
    }
}
