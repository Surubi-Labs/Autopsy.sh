package dev.autopsy.metrics

import dev.autopsy.extraction.CommitExtraction
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.WeekFields

object ChurnCalculator {

    /**
     * Calculate per-module, per-week churn from commits.
     *
     * Churn = additions + deletions per module per ISO week.
     */
    fun calculateChurn(
        commits: List<CommitExtraction>,
        modules: List<String>,
    ): Map<String, List<WeeklyChurn>> {
        val result = mutableMapOf<String, MutableMap<LocalDate, MutableList<Pair<Int, Int>>>>()

        for (commit in commits) {
            val weekStart = toWeekStart(commit.timestamp)
            for (fc in commit.filesChanged) {
                val module = resolveModule(fc.path, modules)
                result
                    .getOrPut(module) { mutableMapOf() }
                    .getOrPut(weekStart) { mutableListOf() }
                    .add(fc.additions to fc.deletions)
            }
        }

        return result.mapValues { (_, weekMap) ->
            weekMap.entries
                .sortedBy { it.key }
                .map { (week, changes) ->
                    val totalAdds = changes.sumOf { it.first }
                    val totalDels = changes.sumOf { it.second }
                    WeeklyChurn(week, totalAdds, totalDels, totalAdds + totalDels)
                }
        }
    }
}

internal fun toWeekStart(timestamp: String): LocalDate {
    val date = OffsetDateTime.parse(timestamp).toLocalDate()
    val weekFields = WeekFields.ISO
    val dayOfWeek = date.get(weekFields.dayOfWeek())
    return date.minusDays((dayOfWeek - 1).toLong())
}

internal fun resolveModule(filePath: String, modules: List<String>): String {
    for (module in modules.sortedByDescending { it.length }) {
        if (filePath.startsWith("$module/")) return module
    }
    return "."
}
