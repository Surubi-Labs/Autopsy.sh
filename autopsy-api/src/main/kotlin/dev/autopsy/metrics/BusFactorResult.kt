package dev.autopsy.metrics

data class BusFactorResult(
    val busFactor: Int,
    val authors: List<String>,
    val dominantAuthorPct: Double,
)
