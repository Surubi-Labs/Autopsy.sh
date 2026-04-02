package dev.autopsy.report

import dev.autopsy.metrics.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertTrue

class ReportFormatterTest {

    private val healthScore = HealthScore(
        overall = 72.5,
        breakdown = mapOf(
            "churn" to 80.0,
            "busFactor" to 66.6,
            "dependencies" to 95.0,
            "testCoverage" to 50.0,
            "hotspots" to 90.0,
        ),
    )

    private val modules = listOf("src/auth", "src/payments")

    private val churn = mapOf(
        "src/auth" to listOf(
            WeeklyChurn(LocalDate.of(2026, 3, 9), 50, 20, 70),
            WeeklyChurn(LocalDate.of(2026, 3, 16), 30, 10, 40),
        ),
        "src/payments" to listOf(
            WeeklyChurn(LocalDate.of(2026, 3, 9), 100, 50, 150),
        ),
    )

    private val busFactor = mapOf(
        "src/auth" to BusFactorResult(3, listOf("alice", "bob", "carol"), 0.5),
        "src/payments" to BusFactorResult(1, listOf("alice"), 1.0),
    )

    private val hotspots = listOf(
        HotspotResult("src/payments/handler.kt", 15, 3, 45.0),
        HotspotResult("src/auth/login.kt", 8, 2, 16.0),
    )

    private val staleness = DependencyStalenessResult(
        overallScore = 95.0,
        staleDeps = listOf(StaleDep("old-lib", 400, 0, "critical")),
        cveCount = 0,
    )

    private val testCoverage = mapOf(
        "src/auth" to listOf(
            WeeklyTestRatio(LocalDate.of(2026, 3, 9), 3, 7, 0.3),
        ),
    )

    @Test
    fun `report contains health score section`() {
        val report = ReportFormatter.format(
            repoPath = "/path/to/repo",
            healthScore = healthScore,
            modules = modules,
            churn = churn,
            busFactor = busFactor,
            hotspots = hotspots,
            staleness = staleness,
            testCoverage = testCoverage,
        )
        assertTrue(report.contains("# Health Report"))
        assertTrue(report.contains("72.5"))
    }

    @Test
    fun `report contains module health section`() {
        val report = ReportFormatter.format(
            "/repo", healthScore, modules, churn, busFactor, hotspots, staleness, testCoverage,
        )
        assertTrue(report.contains("## Module Health"))
        assertTrue(report.contains("src/auth"))
        assertTrue(report.contains("src/payments"))
    }

    @Test
    fun `report contains hotspots section`() {
        val report = ReportFormatter.format(
            "/repo", healthScore, modules, churn, busFactor, hotspots, staleness, testCoverage,
        )
        assertTrue(report.contains("## Hotspots"))
        assertTrue(report.contains("handler.kt"))
    }

    @Test
    fun `report contains dependency section`() {
        val report = ReportFormatter.format(
            "/repo", healthScore, modules, churn, busFactor, hotspots, staleness, testCoverage,
        )
        assertTrue(report.contains("## Dependencies"))
        assertTrue(report.contains("old-lib"))
    }

    @Test
    fun `report contains test coverage section`() {
        val report = ReportFormatter.format(
            "/repo", healthScore, modules, churn, busFactor, hotspots, staleness, testCoverage,
        )
        assertTrue(report.contains("## Test Coverage"))
    }

    @Test
    fun `report with empty metrics still produces valid markdown`() {
        val emptyScore = HealthScore(50.0, mapOf(
            "churn" to 50.0, "busFactor" to 50.0, "dependencies" to 50.0,
            "testCoverage" to 50.0, "hotspots" to 50.0,
        ))
        val report = ReportFormatter.format(
            "/repo", emptyScore, emptyList(), emptyMap(), emptyMap(),
            emptyList(), DependencyStalenessResult(100.0, emptyList(), 0), emptyMap(),
        )
        assertTrue(report.contains("# Health Report"))
        assertTrue(report.contains("50.0"))
    }
}
