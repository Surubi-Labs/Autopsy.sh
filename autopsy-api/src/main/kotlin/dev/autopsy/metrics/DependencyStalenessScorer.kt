package dev.autopsy.metrics

import dev.autopsy.extraction.DependencyExtraction

object DependencyStalenessScorer {

    /**
     * Score dependency staleness from 0-100.
     *
     * 100 = all fresh, 0 = critically outdated.
     * Each dep > 365 days = "critical", > 180 days = "warning".
     * Each CVE adds a penalty.
     */
    fun scoreStaleness(
        dependencies: List<DependencyExtraction>,
    ): DependencyStalenessResult {
        val allDeps = dependencies.flatMap { it.dependencies }
        if (allDeps.isEmpty()) {
            return DependencyStalenessResult(overallScore = 100.0, staleDeps = emptyList(), cveCount = 0)
        }

        var totalPenalty = 0.0
        val staleDeps = mutableListOf<StaleDep>()
        var cveCount = 0

        for (dep in allDeps) {
            val outdated = dep.outdatedDays
            val cves = dep.cves.size
            cveCount += cves

            if (outdated == null && cves == 0) continue

            val severity = when {
                cves > 0 -> "critical"
                outdated != null && outdated > 365 -> "critical"
                outdated != null && outdated > 180 -> "warning"
                else -> "ok"
            }

            if (severity != "ok") {
                staleDeps.add(StaleDep(dep.name, outdated, cves, severity))
            }

            // Staleness penalty: max 10 per dep
            if (outdated != null) {
                totalPenalty += minOf(outdated.toDouble() / 365.0 * 10.0, 10.0)
            }

            // CVE penalty: 15 per CVE
            totalPenalty += cves * 15.0
        }

        val maxPenalty = allDeps.size * 10.0
        val score = maxOf(0.0, 100.0 - (totalPenalty / maxPenalty * 100.0))

        return DependencyStalenessResult(
            overallScore = score,
            staleDeps = staleDeps,
            cveCount = cveCount,
        )
    }
}
