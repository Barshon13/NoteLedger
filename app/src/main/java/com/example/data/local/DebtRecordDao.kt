package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DebtRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtRecordDao {
    @Query("SELECT * FROM debt_records ORDER BY isSettled ASC, date DESC, createdAt DESC")
    fun getAllDebtRecords(): Flow<List<DebtRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtRecord(debt: DebtRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtRecords(debts: List<DebtRecord>)

    @Update
    suspend fun updateDebtRecord(debt: DebtRecord)

    @Delete
    suspend fun deleteDebtRecord(debt: DebtRecord)

    @Query("DELETE FROM debt_records WHERE id = :id")
    suspend fun deleteDebtRecordById(id: String)

    @Query("DELETE FROM debt_records")
    suspend fun deleteAllDebtRecords()

    @Query("SELECT * FROM debt_records")
    suspend fun getAllDebtRecordsDirect(): List<DebtRecord>
}
