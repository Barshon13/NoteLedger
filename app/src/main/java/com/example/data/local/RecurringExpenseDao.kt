package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.RecurringExpense
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {
    @Query("SELECT * FROM recurring_expenses ORDER BY isActive DESC, nextDueDate ASC")
    fun getAllRecurringExpenses(): Flow<List<RecurringExpense>>

    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1 ORDER BY nextDueDate ASC")
    fun getActiveRecurringExpenses(): Flow<List<RecurringExpense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringExpense(expense: RecurringExpense)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringExpenses(expenses: List<RecurringExpense>)

    @Update
    suspend fun updateRecurringExpense(expense: RecurringExpense)

    @Delete
    suspend fun deleteRecurringExpense(expense: RecurringExpense)

    @Query("DELETE FROM recurring_expenses WHERE id = :id")
    suspend fun deleteRecurringExpenseById(id: String)

    @Query("DELETE FROM recurring_expenses")
    suspend fun deleteAllRecurringExpenses()

    @Query("SELECT * FROM recurring_expenses")
    suspend fun getAllRecurringExpensesDirect(): List<RecurringExpense>
}
