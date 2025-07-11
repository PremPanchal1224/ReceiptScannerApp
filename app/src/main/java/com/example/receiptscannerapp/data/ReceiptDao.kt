package com.example.receiptscannerapp.data

import androidx.room.*

@Dao
interface ReceiptDao {

    @Insert
    suspend fun insertReceipt(receipt: ReceiptEntity)

    @Query("SELECT * FROM receipts ORDER BY date DESC")
    suspend fun getAllReceipts(): List<ReceiptEntity>

    @Query("DELETE FROM receipts")
    suspend fun clearAll()

    // ✅ Add this
    @Delete
    suspend fun deleteReceipt(receipt: ReceiptEntity)

    // ✅ Add this
    @Update
    suspend fun updateReceipt(receipt: ReceiptEntity)
}
