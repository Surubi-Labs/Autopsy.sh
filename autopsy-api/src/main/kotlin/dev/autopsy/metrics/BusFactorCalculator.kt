package dev.autopsy.metrics

import dev.autopsy.extraction.CommitExtraction
import java.time.OffsetDateTime

object BusFactorCalculator {

    /**
     * Calculate bus factor per module.
     *
     * Bus factor = number of distinct authors in the window.
     * Dominant author % = max author's commit share.
     */
    fun calculateBusFactor(
        commits: List<CommitExtraction>,
        modules: List<String>,
        windowDays: Int = 90,
    ): Map<String, BusFactorResult> {
        val cutoff = OffsetDateTime.now().minusDays(windowDays.toLong())
        val recentCommits = commits.filter {
            OffsetDateTime.parse(it.timestamp).isAfter(cutoff)
        }

        val moduleAuthors = mutableMapOf<String, MutableMap<String, Int>>()

        for (commit in recentCommits) {
            for (fc in commit.filesChanged) {
                val module = resolveModule(fc.path, modules)
                moduleAuthors
                    .getOrPut(module) { mutableMapOf() }
                    .merge(commit.authorEmail, 1) { a, b -> a + b }
            }
        }

        return modules.associateWith { module ->
            val authors = moduleAuthors[module] ?: emptyMap()
            if (authors.isEmpty()) {
                BusFactorResult(busFactor = 0, authors = emptyList(), dominantAuthorPct = 0.0)
            } else {
                val totalCommits = authors.values.sum()
                val maxCommits = authors.values.max()
                BusFactorResult(
                    busFactor = authors.size,
                    authors = authors.keys.sorted(),
                    dominantAuthorPct = maxCommits.toDouble() / totalCommits,
                )
            }
        }
    }
}
