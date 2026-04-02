package dev.autopsy.queue

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JobMessageTest {

    @Test
    fun `serialize to map and back`() {
        val original = JobMessage(
            runId = "run-123",
            repoId = "repo-456",
            orgId = "org-789",
            stage = Stage.EXTRACT,
            payload = mapOf("commitRange" to "abc..def"),
        )

        val map = original.toMap()
        val restored = JobMessage.fromMap(map)

        assertEquals(original, restored)
    }

    @Test
    fun `serialize with empty payload`() {
        val original = JobMessage(
            runId = "run-1",
            repoId = "repo-1",
            orgId = "org-1",
            stage = Stage.CLONE,
        )

        val map = original.toMap()
        val restored = JobMessage.fromMap(map)

        assertEquals(original, restored)
        assertEquals(emptyMap(), restored.payload)
    }

    @Test
    fun `all stages round-trip correctly`() {
        for (stage in Stage.entries) {
            val msg = JobMessage("r", "rp", "o", stage)
            val restored = JobMessage.fromMap(msg.toMap())
            assertEquals(stage, restored.stage)
        }
    }

    @Test
    fun `toMap produces expected keys`() {
        val msg = JobMessage("r", "rp", "o", Stage.METRICS)
        val map = msg.toMap()

        assertEquals("r", map["runId"])
        assertEquals("rp", map["repoId"])
        assertEquals("o", map["orgId"])
        assertEquals("METRICS", map["stage"])
    }

    @Test
    fun `next stage progression`() {
        assertEquals(Stage.EXTRACT, Stage.CLONE.next())
        assertEquals(Stage.METRICS, Stage.EXTRACT.next())
        assertEquals(Stage.EMBED, Stage.METRICS.next())
        assertEquals(Stage.REPORT, Stage.EMBED.next())
        assertEquals(Stage.DELIVER, Stage.REPORT.next())
        assertEquals(null, Stage.DELIVER.next())
    }
}
