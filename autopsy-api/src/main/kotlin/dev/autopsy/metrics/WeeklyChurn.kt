package dev.autopsy.metrics

import java.time.LocalDate

data class WeeklyChurn(
    val weekStart: LocalDate,
    val additions: Int,
    val deletions: Int,
    val totalChurn: Int,
)
