package dev.autopsy.metrics

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HealthScoreComputerTest {

    @Test
    fun `all healthy inputs score near 100`() {
        val score = HealthScoreComputer.computeHealthScore(
            churn = mapOf("src" to listOf(
                WeeklyChurn(LocalDate.of(2026, 3, 10), 10, 5, 15),
            )),
            busFactor = mapOf("src" to BusFactorResult(3, listOf("a", "b", "c"), 0.4)),
            staleness = DependencyStalenessResult(95.0, emptyList(), 0),
            testCoverage = mapOf("src" to listOf(
                WeeklyTestRatio(LocalDate.of(2026, 3, 10), 5, 10, 0.33),
            )),
            hotspots = emptyList(),
        )
        assertTrue(score.overall > 80.0, "Expected > 80, got ${score.overall}")
    }

    @Test
    fun `all null components use neutral score 50`() {
        val score = HealthScoreComputer.computeHealthScore(
            churn = null,
            busFactor = null,
            staleness = null,
            testCoverage = null,
            hotspots = null,
        )
        assertEquals(50.0, score.overall)
    }

    @Test
    fun `breakdown contains all components`() {
        val score = HealthScoreComputer.computeHealthScore(null, null, null, null, null)
        assertTrue(score.breakdown.containsKey("churn"))
        assertTrue(score.breakdown.containsKey("busFactor"))
        assertTrue(score.breakdown.containsKey("dependencies"))
        assertTrue(score.breakdown.containsKey("testCoverage"))
        assertTrue(score.breakdown.containsKey("hotspots"))
    }

    @Test
    fun `score is clamped to 0-100`() {
        val score = HealthScoreComputer.computeHealthScore(
            churn = mapOf("src" to listOf(
                WeeklyChurn(LocalDate.of(2026, 3, 10), 5000, 5000, 10000),
            )),
            busFactor = mapOf("src" to BusFactorResult(0, emptyList(), 0.0)),
            staleness = DependencyStalenessResult(0.0, emptyList(), 10),
            testCoverage = mapOf("src" to listOf(
                WeeklyTestRatio(LocalDate.of(2026, 3, 10), 0, 100, 0.0),
            )),
            hotspots = listOf(HotspotResult("bad.kt", 50, 10, 500.0)),
        )
        assertTrue(score.overall >= 0.0)
        assertTrue(score.overall <= 100.0)
    }
}
