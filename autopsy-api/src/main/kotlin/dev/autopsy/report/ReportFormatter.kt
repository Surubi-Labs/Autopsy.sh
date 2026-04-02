package dev.autopsy.report

import dev.autopsy.metrics.*

object ReportFormatter {

    fun format(
        repoPath: String,
        healthScore: HealthScore,
        modules: List<String>,
        churn: Map<String, List<WeeklyChurn>>,
        busFactor: Map<String, BusFactorResult>,
        hotspots: List<HotspotResult>,
        staleness: DependencyStalenessResult,
        testCoverage: Map<String, List<WeeklyTestRatio>>,
    ): String = buildString {
        appendLine("# Health Report")
        appendLine()
        appendLine("**Repository:** `$repoPath`")
        appendLine()
        appendLine("**Overall Health Score: ${healthScore.overall}/100**")
        appendLine()
        formatBreakdown(healthScore)
        appendLine()
        formatModuleHealth(modules, churn, busFactor)
        appendLine()
        formatChurnTrends(churn)
        appendLine()
        formatBusFactor(busFactor)
        appendLine()
        formatHotspots(hotspots)
        appendLine()
        formatDependencies(staleness)
        appendLine()
        formatTestCoverage(testCoverage)
    }

    private fun StringBuilder.formatBreakdown(healthScore: HealthScore) {
        appendLine("### Score Breakdown")
        appendLine()
        appendLine("| Component | Score |")
        appendLine("|-----------|-------|")
        for ((component, score) in healthScore.breakdown) {
            appendLine("| $component | ${"%.1f".format(score)} |")
        }
    }

    private fun StringBuilder.formatModuleHealth(
        modules: List<String>,
        churn: Map<String, List<WeeklyChurn>>,
        busFactor: Map<String, BusFactorResult>,
    ) {
        appendLine("## Module Health")
        appendLine()
        if (modules.isEmpty()) {
            appendLine("No modules detected.")
            return
        }
        appendLine("| Module | Bus Factor | Dominant Author % | Recent Churn |")
        appendLine("|--------|-----------|-------------------|-------------|")
        for (module in modules) {
            val bf = busFactor[module]
            val moduleChurn = churn[module]?.lastOrNull()
            val bfStr = bf?.busFactor?.toString() ?: "N/A"
            val domStr = bf?.let { "${"%.0f".format(it.dominantAuthorPct * 100)}%" } ?: "N/A"
            val churnStr = moduleChurn?.totalChurn?.toString() ?: "0"
            appendLine("| $module | $bfStr | $domStr | $churnStr |")
        }
    }

    private fun StringBuilder.formatChurnTrends(churn: Map<String, List<WeeklyChurn>>) {
        appendLine("## Code Churn Trends")
        appendLine()
        if (churn.isEmpty()) {
            appendLine("No churn data available.")
            return
        }
        for ((module, weeks) in churn) {
            appendLine("### $module")
            appendLine()
            appendLine("| Week | Additions | Deletions | Total |")
            appendLine("|------|-----------|-----------|-------|")
            for (week in weeks) {
                appendLine("| ${week.weekStart} | +${week.additions} | -${week.deletions} | ${week.totalChurn} |")
            }
            appendLine()
        }
    }

    private fun StringBuilder.formatBusFactor(busFactor: Map<String, BusFactorResult>) {
        appendLine("## Ownership & Bus Factor")
        appendLine()
        if (busFactor.isEmpty()) {
            appendLine("No ownership data available.")
            return
        }
        for ((module, bf) in busFactor) {
            val risk = when {
                bf.busFactor == 0 -> "No recent activity"
                bf.busFactor == 1 -> "**HIGH RISK** - Single maintainer"
                bf.busFactor == 2 -> "Moderate risk"
                else -> "Healthy"
            }
            appendLine("- **$module**: Bus factor ${bf.busFactor} ($risk)")
            if (bf.authors.isNotEmpty()) {
                appendLine("  - Authors: ${bf.authors.joinToString(", ")}")
            }
        }
    }

    private fun StringBuilder.formatHotspots(hotspots: List<HotspotResult>) {
        appendLine("## Hotspots")
        appendLine()
        if (hotspots.isEmpty()) {
            appendLine("No hotspots detected.")
            return
        }
        appendLine("| File | Changes | Authors | Score |")
        appendLine("|------|---------|---------|-------|")
        for (hs in hotspots) {
            appendLine("| ${hs.filePath} | ${hs.changeCount} | ${hs.uniqueAuthors} | ${"%.0f".format(hs.score)} |")
        }
    }

    private fun StringBuilder.formatDependencies(staleness: DependencyStalenessResult) {
        appendLine("## Dependencies")
        appendLine()
        appendLine("**Dependency Health Score: ${"%.1f".format(staleness.overallScore)}/100**")
        appendLine()
        if (staleness.cveCount > 0) {
            appendLine("**${staleness.cveCount} known vulnerabilities (CVEs)**")
            appendLine()
        }
        if (staleness.staleDeps.isEmpty()) {
            appendLine("All dependencies are up to date.")
        } else {
            appendLine("| Dependency | Days Outdated | CVEs | Severity |")
            appendLine("|-----------|--------------|------|----------|")
            for (dep in staleness.staleDeps) {
                val days = dep.outdatedDays?.toString() ?: "N/A"
                appendLine("| ${dep.name} | $days | ${dep.cveCount} | ${dep.severity} |")
            }
        }
    }

    private fun StringBuilder.formatTestCoverage(testCoverage: Map<String, List<WeeklyTestRatio>>) {
        appendLine("## Test Coverage Trends")
        appendLine()
        if (testCoverage.isEmpty()) {
            appendLine("No test coverage data available.")
            return
        }
        for ((module, weeks) in testCoverage) {
            appendLine("### $module")
            appendLine()
            appendLine("| Week | Test Changes | Source Changes | Ratio |")
            appendLine("|------|-------------|---------------|-------|")
            for (week in weeks) {
                appendLine("| ${week.weekStart} | ${week.testChanges} | ${week.sourceChanges} | ${"%.1f".format(week.ratio * 100)}% |")
            }
            appendLine()
        }
    }
}
