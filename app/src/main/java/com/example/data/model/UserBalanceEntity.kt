package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_balance")
data class UserBalanceEntity(
    @PrimaryKey val id: Int = 1,
    val coinBalance: Double = 0.0,
    val freeNotesRemaining: Int = 10
)
