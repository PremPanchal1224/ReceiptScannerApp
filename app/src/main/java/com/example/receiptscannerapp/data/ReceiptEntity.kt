package com.example.receiptscannerapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val items: List<String>,
    val total: Double,
    val rawText: String,
    val date: Long = System.currentTimeMillis(),
    val embedding: List<Float> = emptyList()  // ✅ Changed from embeddingJson:String
)
