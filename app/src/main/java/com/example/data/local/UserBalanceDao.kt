package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.UserBalanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserBalanceDao {
    @Query("SELECT * FROM user_balance WHERE id = 1")
    fun getBalance(): Flow<UserBalanceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateBalance(balance: UserBalanceEntity)

    @Query("SELECT * FROM user_balance WHERE id = 1 LIMIT 1")
    suspend fun getBalanceDirect(): UserBalanceEntity?

    @Query("DELETE FROM user_balance")
    suspend fun deleteAllBalance()
}
