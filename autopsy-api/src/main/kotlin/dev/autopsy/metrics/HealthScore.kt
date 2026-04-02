package dev.autopsy.metrics

data class HealthScore(
    val overall: Double,
    val breakdown: Map<String, Double>,
)
