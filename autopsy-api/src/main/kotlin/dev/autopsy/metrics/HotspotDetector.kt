package dev.autopsy.metrics

import dev.autopsy.extraction.CommitExtraction
import java.time.OffsetDateTime

object HotspotDetector {

    /**
     * Detect file hotspots — files changed frequently by many authors.
     *
     * Score = changeCount * uniqueAuthors.
     * Returns top N files by score.
     */
    fun detectHotspots(
        commits: List<CommitExtraction>,
        windowDays: Int = 30,
        limit: Int = 10,
    ): List<HotspotResult> {
        val cutoff = OffsetDateTime.now().minusDays(windowDays.toLong())
        val recentCommits = commits.filter {
            OffsetDateTime.parse(it.timestamp).isAfter(cutoff)
        }

        val fileChanges = mutableMapOf<String, Int>()
        val fileAuthors = mutableMapOf<String, MutableSet<String>>()

        for (commit in recentCommits) {
            for (fc in commit.filesChanged) {
                fileChanges.merge(fc.path, 1) { a, b -> a + b }
                fileAuthors.getOrPut(fc.path) { mutableSetOf() }.add(commit.authorEmail)
            }
        }

        return fileChanges.map { (path, count) ->
            val authors = fileAuthors[path]?.size ?: 0
            HotspotResult(
                filePath = path,
                changeCount = count,
                uniqueAuthors = authors,
                score = (count * authors).toDouble(),
            )
        }
            .sortedByDescending { it.score }
            .take(limit)
    }
}
