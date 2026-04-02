package dev.autopsy.metrics

data class HotspotResult(
    val filePath: String,
    val changeCount: Int,
    val uniqueAuthors: Int,
    val score: Double,
)
