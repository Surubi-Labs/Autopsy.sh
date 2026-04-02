package dev.autopsy.metrics

import dev.autopsy.extraction.CommitExtraction

object ModuleDetector {

    /**
     * Detect modules from commit file paths.
     *
     * Uses first-level directory as module. If "src/" is the only top-level
     * directory, uses second-level directories instead.
     * Files at root level belong to the "." module.
     */
    fun detectModules(commits: List<CommitExtraction>): List<String> {
        val allPaths = commits.flatMap { it.filesChanged.map { fc -> fc.path } }
        if (allPaths.isEmpty()) return emptyList()

        val topDirs = allPaths.mapNotNull { path ->
            val parts = path.split("/")
            if (parts.size > 1) parts[0] else null
        }.toSet()

        // If src/ is the only top-level directory, use second-level
        val useSrcPrefix = topDirs.size == 1 && topDirs.first() == "src"

        val modules = allPaths.map { path ->
            val parts = path.split("/")
            when {
                parts.size <= 1 -> "."
                useSrcPrefix && parts.size >= 3 -> "${parts[0]}/${parts[1]}"
                parts.size >= 2 -> {
                    if (parts[0] == "src" && parts.size >= 3) {
                        "${parts[0]}/${parts[1]}"
                    } else {
                        parts[0]
                    }
                }
                else -> "."
            }
        }.toSortedSet()

        return modules.toList()
    }
}
