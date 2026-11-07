package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coin_balance")
data class CoinBalanceEntity(
    @PrimaryKey val id: Int = 1,
    val balance: Double
)
