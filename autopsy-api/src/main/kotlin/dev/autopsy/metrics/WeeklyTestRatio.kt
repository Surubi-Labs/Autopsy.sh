package dev.autopsy.metrics

import java.time.LocalDate

data class WeeklyTestRatio(
    val weekStart: LocalDate,
    val testChanges: Int,
    val sourceChanges: Int,
    val ratio: Double,
)
