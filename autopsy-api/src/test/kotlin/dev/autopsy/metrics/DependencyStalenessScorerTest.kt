package dev.autopsy.metrics

import dev.autopsy.extraction.DependencyExtraction
import dev.autopsy.extraction.DependencyInfo
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DependencyStalenessScorerTest {

    @Test
    fun `all fresh deps score 100`() {
        val deps = listOf(
            depExtraction(listOf(
                dep("express", outdatedDays = 10),
                dep("lodash", outdatedDays = 5),
            ))
        )
        val result = DependencyStalenessScorer.scoreStaleness(deps)
        assertTrue(result.overallScore > 90.0)
        assertEquals(0, result.cveCount)
    }

    @Test
    fun `stale dep reduces score`() {
        val deps = listOf(
            depExtraction(listOf(
                dep("old-lib", outdatedDays = 400),
            ))
        )
        val result = DependencyStalenessScorer.scoreStaleness(deps)
        assertTrue(result.overallScore < 100.0)
        assertTrue(result.staleDeps.isNotEmpty())
    }

    @Test
    fun `dep with CVE adds penalty`() {
        val deps = listOf(
            depExtraction(listOf(
                dep("vuln-lib", cves = listOf("CVE-2021-1234")),
            ))
        )
        val result = DependencyStalenessScorer.scoreStaleness(deps)
        assertTrue(result.overallScore < 100.0)
        assertEquals(1, result.cveCount)
    }

    @Test
    fun `no deps scores 100`() {
        val result = DependencyStalenessScorer.scoreStaleness(emptyList())
        assertEquals(100.0, result.overallScore)
    }

    @Test
    fun `null outdated days are skipped from scoring`() {
        val deps = listOf(
            depExtraction(listOf(
                dep("unknown-lib", outdatedDays = null),
            ))
        )
        val result = DependencyStalenessScorer.scoreStaleness(deps)
        assertEquals(100.0, result.overallScore)
    }

    private fun dep(
        name: String,
        outdatedDays: Int? = null,
        cves: List<String> = emptyList(),
    ) = DependencyInfo(name = name, current = "1.0.0", outdatedDays = outdatedDays, cves = cves)

    private fun depExtraction(deps: List<DependencyInfo>) = DependencyExtraction(
        file = "package.json",
        manager = "npm",
        dependencies = deps,
    )
}
