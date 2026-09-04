package com.example.data.model

import java.time.LocalDate

/**
 * Data structure representing aggregated daily spending telemetry for trend visualization
 */
data class DailySpendingTrend(
    val date: LocalDate,
    val dayOfMonth: Int,
    val dayLabel: String,
    val totalAmount: Double,
    val transactionCount: Int,
    val expenses: List<Expense> = emptyList(),
    val isToday: Boolean = false,
    val isFuture: Boolean = false
)

enum class TrendTimeRange(val label: String, val shortLabel: String) {
    FULL_MONTH("Full Month", "Month"),
    LAST_7_DAYS("Past 7 Days", "7D"),
    LAST_14_DAYS("Past 14 Days", "14D"),
    LAST_30_DAYS("Past 30 Days", "30D")
}

enum class TrendChartType(val label: String) {
    AREA_CURVE("Smooth Area"),
    BARS("Daily Bars"),
    CUMULATIVE("Cumulative")
}
