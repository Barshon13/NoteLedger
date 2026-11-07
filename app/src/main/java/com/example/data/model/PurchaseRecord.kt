package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "purchase_history")
data class PurchaseRecord(
    @PrimaryKey val orderId: String,
    val productId: String,
    val coinAmount: Double,
    val price: String,
    val timestamp: Long = Instant.now().toEpochMilli()
)
