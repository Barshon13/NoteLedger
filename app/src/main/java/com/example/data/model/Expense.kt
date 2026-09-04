package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val amount: Double = 0.0,
    val category: String = ExpenseCategory.OTHER.displayName,
    val note: String = "",
    val date: String = LocalDate.now().toString(), // YYYY-MM-DD
    val createdAt: String = Instant.now().toString()
)

enum class ExpenseCategory(val displayName: String) {
    FOOD("Food"),
    TRANSPORT("Transport"),
    SHOPPING("Shopping"),
    BILLS("Bills"),
    OTHER("Other");

    companion object {
        fun fromString(value: String): ExpenseCategory {
            return entries.find { it.displayName.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}
