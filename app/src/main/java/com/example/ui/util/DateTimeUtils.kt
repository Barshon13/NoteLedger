package com.example.ui.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {
    private val dayMonthYearFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())
    private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    fun formatExpenseDateHeader(dateStr: String): String {
        return try {
            val date = LocalDate.parse(dateStr)
            val today = LocalDate.now()
            when (date) {
                today -> "Today (${date.format(DateTimeFormatter.ofPattern("MMM d"))})"
                today.minusDays(1) -> "Yesterday (${date.format(DateTimeFormatter.ofPattern("MMM d"))})"
                today.plusDays(1) -> "Tomorrow (${date.format(DateTimeFormatter.ofPattern("MMM d"))})"
                else -> date.format(dayMonthYearFormatter)
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatNoteTimestamp(isoTimestamp: String): String {
        return try {
            val instant = Instant.parse(isoTimestamp)
            val ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            val date = ldt.toLocalDate()
            val today = LocalDate.now()

            when {
                date == today -> "Today at " + ldt.format(timeFormatter)
                date == today.minusDays(1) -> "Yesterday at " + ldt.format(timeFormatter)
                date.year == today.year -> ldt.format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
                else -> ldt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
            }
        } catch (e: Exception) {
            "Recently"
        }
    }

    fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "৳" + format.format(amount)
    }

    fun formatMonthYear(yearMonth: String): String {
        return try {
            val parts = yearMonth.split("-")
            val date = LocalDate.of(parts[0].toInt(), parts[1].toInt(), 1)
            date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        } catch (e: Exception) {
            yearMonth
        }
    }
}
