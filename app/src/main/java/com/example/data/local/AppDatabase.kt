package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.UserBalanceEntity
import com.example.data.model.DebtRecord
import com.example.data.model.Expense
import com.example.data.model.MonthlyBudget
import com.example.data.model.Note
import com.example.data.model.RecurringExpense
import com.example.data.local.UserBalanceDao
import com.example.data.local.NoteDao
import com.example.data.local.ExpenseDao
import com.example.data.local.BudgetDao
import com.example.data.local.RecurringExpenseDao
import com.example.data.local.DebtRecordDao

import com.example.data.model.PurchaseRecord
import com.example.data.local.PurchaseDao

@Database(
    entities = [Note::class, Expense::class, MonthlyBudget::class, RecurringExpense::class, DebtRecord::class, UserBalanceEntity::class, PurchaseRecord::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun debtRecordDao(): DebtRecordDao
    abstract fun userBalanceDao(): UserBalanceDao
    abstract fun purchaseDao(): PurchaseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "offline_tracker_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
