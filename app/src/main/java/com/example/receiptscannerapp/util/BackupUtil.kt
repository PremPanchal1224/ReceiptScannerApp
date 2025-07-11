package com.example.receiptscannerapp.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.receiptscannerapp.data.ReceiptEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.lang.reflect.Type

object BackupUtil {
    private const val FILE_NAME = "receipt_backup.json"

    fun backupToJSON(context: Context, data: List<ReceiptEntity>) {
        try {
            val json = Gson().toJson(data)
            val file = File(context.filesDir, FILE_NAME)
            file.writeText(json)
            Toast.makeText(context, "Backup saved!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("BackupUtil", "Backup failed: ${e.message}", e)
            Toast.makeText(context, "Backup failed", Toast.LENGTH_SHORT).show()
        }
    }

    fun restoreFromJSON(context: Context): List<ReceiptEntity>? {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) {
                Toast.makeText(context, "No backup found", Toast.LENGTH_SHORT).show()
                null
            } else {
                val json = file.readText()
                val type: Type = object : TypeToken<List<ReceiptEntity>>() {}.type
                Gson().fromJson(json, type)
            }
        } catch (e: Exception) {
            Log.e("BackupUtil", "Restore failed: ${e.message}", e)
            Toast.makeText(context, "Restore failed", Toast.LENGTH_SHORT).show()
            null
        }
    }
}
