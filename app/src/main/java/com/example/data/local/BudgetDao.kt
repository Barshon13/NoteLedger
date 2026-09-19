package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.MonthlyBudget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM monthly_budgets WHERE monthYear = :monthYear LIMIT 1")
    fun getBudgetForMonth(monthYear: String): Flow<MonthlyBudget?>

    @Query("SELECT * FROM monthly_budgets")
    fun getAllBudgets(): Flow<List<MonthlyBudget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: MonthlyBudget)

    @Query("DELETE FROM monthly_budgets WHERE monthYear = :monthYear")
    suspend fun deleteBudget(monthYear: String)

    @Query("DELETE FROM monthly_budgets")
    suspend fun deleteAllBudgets()

    @Query("SELECT * FROM monthly_budgets")
    suspend fun getAllBudgetsDirect(): List<MonthlyBudget>
}
