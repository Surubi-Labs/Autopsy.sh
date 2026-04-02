package dev.autopsy.metrics

import dev.autopsy.extraction.CommitExtraction
import dev.autopsy.extraction.FileChange
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HotspotDetectorTest {

    private val now = OffsetDateTime.now()

    @Test
    fun `file with high changes and authors scores highest`() {
        val commits = listOf(
            commit("dev1@test.com", daysAgo(1), listOf("hot.kt")),
            commit("dev2@test.com", daysAgo(2), listOf("hot.kt")),
            commit("dev3@test.com", daysAgo(3), listOf("hot.kt")),
            commit("dev1@test.com", daysAgo(1), listOf("cold.kt")),
        )
        val hotspots = HotspotDetector.detectHotspots(commits, windowDays = 30)

        assertEquals("hot.kt", hotspots[0].filePath)
        assertEquals(3, hotspots[0].changeCount)
        assertEquals(3, hotspots[0].uniqueAuthors)
        assertEquals(9.0, hotspots[0].score) // 3 * 3
    }

    @Test
    fun `respects limit parameter`() {
        val commits = (1..20).map { i ->
            commit("dev@test.com", daysAgo(1), listOf("file$i.kt"))
        }
        val hotspots = HotspotDetector.detectHotspots(commits, limit = 5)

        assertEquals(5, hotspots.size)
    }

    @Test
    fun `empty commits returns empty list`() {
        val hotspots = HotspotDetector.detectHotspots(emptyList())
        assertTrue(hotspots.isEmpty())
    }

    private fun commit(author: String, timestamp: String, files: List<String>): CommitExtraction =
        CommitExtraction(
            sha = "abc",
            authorEmail = author,
            timestamp = timestamp,
            message = "test",
            filesChanged = files.map { FileChange(it, 10, 5) },
        )

    private fun daysAgo(days: Int): String = now.minusDays(days.toLong()).toString()
}
