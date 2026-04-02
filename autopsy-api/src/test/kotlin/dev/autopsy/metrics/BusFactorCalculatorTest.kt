package dev.autopsy.metrics

import dev.autopsy.extraction.CommitExtraction
import dev.autopsy.extraction.FileChange
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.test.assertEquals

class BusFactorCalculatorTest {

    private val now = OffsetDateTime.now()

    @Test
    fun `single author has bus factor 1 and 100 pct dominant`() {
        val commits = listOf(
            commit("dev@test.com", daysAgo(5), listOf("src/auth/login.kt")),
            commit("dev@test.com", daysAgo(10), listOf("src/auth/signup.kt")),
        )
        val result = BusFactorCalculator.calculateBusFactor(commits, listOf("src/auth"))

        assertEquals(1, result["src/auth"]!!.busFactor)
        assertEquals(1.0, result["src/auth"]!!.dominantAuthorPct)
    }

    @Test
    fun `multiple authors increase bus factor`() {
        val commits = listOf(
            commit("dev1@test.com", daysAgo(5), listOf("src/auth/login.kt")),
            commit("dev2@test.com", daysAgo(10), listOf("src/auth/login.kt")),
            commit("dev3@test.com", daysAgo(15), listOf("src/auth/login.kt")),
        )
        val result = BusFactorCalculator.calculateBusFactor(commits, listOf("src/auth"))

        assertEquals(3, result["src/auth"]!!.busFactor)
    }

    @Test
    fun `commits outside window are excluded`() {
        val commits = listOf(
            commit("dev1@test.com", daysAgo(5), listOf("src/auth/login.kt")),
            commit("dev2@test.com", daysAgo(100), listOf("src/auth/login.kt")),
        )
        val result = BusFactorCalculator.calculateBusFactor(commits, listOf("src/auth"), windowDays = 90)

        assertEquals(1, result["src/auth"]!!.busFactor)
    }

    @Test
    fun `module with no recent commits has bus factor 0`() {
        val commits = listOf(
            commit("dev@test.com", daysAgo(100), listOf("src/auth/login.kt")),
        )
        val result = BusFactorCalculator.calculateBusFactor(commits, listOf("src/auth"), windowDays = 90)

        assertEquals(0, result["src/auth"]!!.busFactor)
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
