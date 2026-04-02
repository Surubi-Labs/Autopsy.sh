package dev.autopsy.metrics

import dev.autopsy.extraction.CommitExtraction
import dev.autopsy.extraction.FileChange
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestCoverageTrendTrackerTest {

    @Test
    fun `calculates ratio of test to source changes`() {
        val commits = listOf(
            commit("2026-03-10T10:00:00Z", listOf(
                "src/auth/login.kt" to (10 to 5),
                "src/auth/signup.kt" to (20 to 3),
                "tests/test_login.kt" to (15 to 2),
            )),
        )
        val result = TestCoverageTrendTracker.trackTestCoverage(
            commits,
            testFiles = listOf("tests/test_login.kt"),
            sourceFiles = listOf("src/auth/login.kt", "src/auth/signup.kt"),
            modules = listOf("src/auth", "tests"),
        )

        // src/auth should have 2 source changes, 0 test changes
        val authRatios = result["src/auth"]!!
        assertEquals(1, authRatios.size)
        assertEquals(0, authRatios[0].testChanges)
        assertEquals(2, authRatios[0].sourceChanges)
    }

    @Test
    fun `empty commits returns empty map`() {
        val result = TestCoverageTrendTracker.trackTestCoverage(
            emptyList(), emptyList(), emptyList(), emptyList(),
        )
        assertTrue(result.isEmpty())
    }

    private fun commit(timestamp: String, files: List<Pair<String, Pair<Int, Int>>>): CommitExtraction =
        CommitExtraction(
            sha = "abc",
            authorEmail = "dev@test.com",
            timestamp = timestamp,
            message = "test",
            filesChanged = files.map { (path, changes) ->
                FileChange(path, changes.first, changes.second)
            },
        )
}
