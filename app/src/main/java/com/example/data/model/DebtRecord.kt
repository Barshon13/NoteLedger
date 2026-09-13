package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(tableName = "debt_records")
data class DebtRecord(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val personName: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val isOwedToMe: Boolean = true, // true = They owe me (Receivable), false = I owe them (Payable)
    val isSettled: Boolean = false,
    val date: String = LocalDate.now().toString(), // YYYY-MM-DD
    val createdAt: String = Instant.now().toString()
)
