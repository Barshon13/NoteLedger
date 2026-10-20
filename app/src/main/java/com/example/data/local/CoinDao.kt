package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CoinBalanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoinDao {
    @Query("SELECT * FROM coin_balance WHERE id = 1")
    fun getBalance(): Flow<CoinBalanceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateBalance(balance: CoinBalanceEntity)
}
