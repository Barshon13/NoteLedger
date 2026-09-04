package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "monthly_budgets")
data class MonthlyBudget(
    @PrimaryKey
    val monthYear: String = LocalDate.now().toString().substring(0, 7), // YYYY-MM
    val totalBudget: Double = 0.0,
    val foodBudget: Double = 0.0,
    val transportBudget: Double = 0.0,
    val shoppingBudget: Double = 0.0,
    val billsBudget: Double = 0.0,
    val otherBudget: Double = 0.0
) {
    fun getBudgetForCategory(category: String): Double {
        return when (ExpenseCategory.fromString(category)) {
            ExpenseCategory.FOOD -> foodBudget
            ExpenseCategory.TRANSPORT -> transportBudget
            ExpenseCategory.SHOPPING -> shoppingBudget
            ExpenseCategory.BILLS -> billsBudget
            ExpenseCategory.OTHER -> otherBudget
        }
    }
}
