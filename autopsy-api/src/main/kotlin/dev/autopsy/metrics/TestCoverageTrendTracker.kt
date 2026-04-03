package dev.autopsy.metrics

import dev.autopsy.extraction.CommitExtraction

object TestCoverageTrendTracker {

    /**
     * Track test coverage trend as weekly test/source file change ratio.
     *
     * For each week and module, counts file changes classified as test vs source.
     * Ratio = testChanges / (testChanges + sourceChanges).
     */
    fun trackTestCoverage(
        commits: List<CommitExtraction>,
        testFiles: List<String>,
        sourceFiles: List<String>,
        modules: List<String>,
    ): Map<String, List<WeeklyTestRatio>> {
        val testSet = testFiles.toSet()

        val moduleWeekData = mutableMapOf<String, MutableMap<java.time.LocalDate, Pair<Int, Int>>>()

        for (commit in commits) {
            val weekStart = toWeekStart(commit.timestamp)
            for (fc in commit.filesChanged) {
                val module = resolveModule(fc.path, modules)
                val weekData = moduleWeekData.getOrPut(module) { mutableMapOf() }
                val current = weekData.getOrDefault(weekStart, 0 to 0)

                val isTest = testSet.contains(fc.path)
                val updated = if (isTest) {
                    (current.first + 1) to current.second
                } else {
                    current.first to (current.second + 1)
                }

                weekData[weekStart] = updated
            }
        }

        return moduleWeekData.mapValues { (_, weekMap) ->
            weekMap.entries
                .sortedBy { it.key }
                .map { (week, counts) ->
                    val (testCount, sourceCount) = counts
                    val total = testCount + sourceCount
                    val ratio = if (total > 0) testCount.toDouble() / total else 0.0
                    WeeklyTestRatio(week, testCount, sourceCount, ratio)
                }
        }
    }
}
