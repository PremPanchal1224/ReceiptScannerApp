package com.example.receiptscannerapp.util

import android.content.Context
import android.widget.Toast
import com.example.receiptscannerapp.data.ReceiptEntity
import java.io.File
import java.io.FileWriter

object CSVExporter {
    fun exportToCSV(context: Context, receipts: List<ReceiptEntity>) {
        val csvHeader = "Date,Items,Total"
        val fileName = "receipts_export.csv"
        val exportDir = File(context.getExternalFilesDir(null), "")
        if (!exportDir.exists()) exportDir.mkdirs()

        val file = File(exportDir, fileName)
        try {
            FileWriter(file).use { writer ->
                writer.appendLine(csvHeader)
                for (receipt in receipts) {
                    val date = java.text.SimpleDateFormat("dd/MM/yyyy")
                        .format(receipt.date)
                    val items = receipt.items.joinToString(" | ")
                    writer.appendLine("$date,\"$items\",${receipt.total}")
                }
            }
            Toast.makeText(context, "CSV exported to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
