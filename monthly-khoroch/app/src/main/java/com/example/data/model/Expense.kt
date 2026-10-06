package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["date"]),
        Index(value = ["year", "month"]),
        Index(value = ["year", "month", "date"]),
        Index(value = ["productName"])
    ]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // "yyyy-MM-dd" e.g. "2026-10-03"
    val year: Int,
    val month: Int, // 1 to 12
    val day: Int,
    val productName: String,
    val quantity: Double = 1.0,
    val unit: String = "",
    val unitPricePoisha: Long = 0L,
    val totalPoisha: Long, // Actual total amount paid for this expense in poisha
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
