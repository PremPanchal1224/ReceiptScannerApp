package com.example.receiptscannerapp.model

data class Receipt(
    val id: Int = 0,
    val itemList: List<String>,
    val total: Double,
    val rawText: String,
    val date: Long = System.currentTimeMillis()
)
