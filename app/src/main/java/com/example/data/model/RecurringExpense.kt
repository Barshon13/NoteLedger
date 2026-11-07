package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(tableName = "recurring_expenses")
data class RecurringExpense(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val amount: Double = 0.0,
    val category: String = ExpenseCategory.BILLS.displayName,
    val frequency: String = RecurringFrequency.MONTHLY.displayName,
    val nextDueDate: String = LocalDate.now().toString(), // YYYY-MM-DD
    val isActive: Boolean = true,
    val note: String = "",
    val createdAt: String = Instant.now().toString()
)

enum class RecurringFrequency(val displayName: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly");

    companion object {
        fun fromString(value: String): RecurringFrequency {
            return entries.find { it.displayName.equals(value, ignoreCase = true) } ?: MONTHLY
        }
    }
}
