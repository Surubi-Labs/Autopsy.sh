package dev.autopsy.metrics

data class StaleDep(
    val name: String,
    val outdatedDays: Int?,
    val cveCount: Int,
    val severity: String,
)

data class DependencyStalenessResult(
    val overallScore: Double,
    val staleDeps: List<StaleDep>,
    val cveCount: Int,
)
