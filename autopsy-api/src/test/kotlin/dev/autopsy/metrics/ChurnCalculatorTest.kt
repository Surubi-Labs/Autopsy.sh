package dev.autopsy.metrics

import dev.autopsy.extraction.CommitExtraction
import dev.autopsy.extraction.FileChange
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChurnCalculatorTest {

    @Test
    fun `groups churn by module and week`() {
        val commits = listOf(
            commit("2026-03-10T10:00:00Z", listOf("src/auth/login.kt" to (10 to 5))),
            commit("2026-03-11T10:00:00Z", listOf("src/auth/signup.kt" to (20 to 3))),
            commit("2026-03-17T10:00:00Z", listOf("src/auth/login.kt" to (5 to 2))),
        )
        val modules = listOf("src/auth")
        val result = ChurnCalculator.calculateChurn(commits, modules)

        val authChurn = result["src/auth"]!!
        assertEquals(2, authChurn.size) // 2 weeks
        assertEquals(38, authChurn[0].totalChurn) // week 1: 10+5+20+3
        assertEquals(7, authChurn[1].totalChurn) // week 2: 5+2
    }

    @Test
    fun `empty commits returns empty map`() {
        val result = ChurnCalculator.calculateChurn(emptyList(), listOf("src"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `commit with no file changes is skipped`() {
        val commits = listOf(
            CommitExtraction("abc", "dev@test.com", "2026-03-10T10:00:00Z", "merge", emptyList())
        )
        val result = ChurnCalculator.calculateChurn(commits, listOf("src"))
        assertTrue(result.isEmpty())
    }

    private fun commit(timestamp: String, files: List<Pair<String, Pair<Int, Int>>>): CommitExtraction =
        CommitExtraction(
            sha = "abc123",
            authorEmail = "dev@test.com",
            timestamp = timestamp,
            message = "test",
            filesChanged = files.map { (path, changes) ->
                FileChange(path, changes.first, changes.second)
            },
        )
}
