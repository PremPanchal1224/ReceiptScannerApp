package com.example.receiptscannerapp.util

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import com.example.receiptscannerapp.data.ReceiptEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PDFExporter {

    fun exportToPDF(context: Context, receipts: List<ReceiptEntity>) {
        val pdfDocument = PdfDocument()
        val paint = android.graphics.Paint()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        var y = 50
        paint.textSize = 14f
        receipts.forEachIndexed { index, receipt ->
            canvas.drawText("Receipt ${index + 1}:", 10f, y.toFloat(), paint)
            y += 20
            canvas.drawText("Items: ${receipt.items.joinToString(", ")}", 10f, y.toFloat(), paint)
            y += 20
            canvas.drawText("Total: ₹${String.format("%.2f", receipt.total)}", 10f, y.toFloat(), paint)
            y += 30
            if (y > 800) { // Add more pages if needed
                pdfDocument.finishPage(page)
                pdfDocument.startPage(pageInfo)
                canvas.drawColor(android.graphics.Color.WHITE)
                y = 50
            }
        }

        pdfDocument.finishPage(page)

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Receipts_$timeStamp.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(context, "PDF exported: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error exporting PDF", Toast.LENGTH_SHORT).show()
        }

        pdfDocument.close()
    }
}
