package com.example.receiptscannerapp

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.receiptscannerapp.data.AppDatabase
import com.example.receiptscannerapp.data.ReceiptDao
import com.example.receiptscannerapp.data.ReceiptEntity
import com.example.receiptscannerapp.ui.theme.ReceiptScannerAppTheme
import com.example.receiptscannerapp.ui2.SummaryScreen
import com.example.receiptscannerapp.util.BackupUtil
import com.example.receiptscannerapp.util.CSVExporter
import com.example.receiptscannerapp.util.EmbeddingUtil
import com.example.receiptscannerapp.util.PDFExporter
import com.example.receiptscannerapp.util.SearchUtil
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : FragmentActivity() {

    private lateinit var imagePickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var receiptDao: ReceiptDao
    private val receipts = mutableStateListOf<ReceiptEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "receipt-db").build()
        receiptDao = db.receiptDao()

        lifecycleScope.launch {
            receipts.clear()
            receipts.addAll(receiptDao.getAllReceipts())
        }

        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                runImageOCR(uri)
            } else {
                Log.d("ImagePicker", "No media selected")
            }
        }

        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
        ) {
            val executor = ContextCompat.getMainExecutor(this)
            val biometricPrompt = BiometricPrompt(this as FragmentActivity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        Log.d("BiometricAuth", "Success")
                        launchAppContent()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        Toast.makeText(this@MainActivity, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        Toast.makeText(this@MainActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Fingerprint Login")
                .setSubtitle("Unlock Receipt Scanner")
                .setNegativeButtonText("Cancel")
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
            Toast.makeText(this, "Biometric not available", Toast.LENGTH_SHORT).show()
            launchAppContent()
        }
    }

    private fun launchAppContent() {
        setContent {
            val navController = rememberNavController()
            var isDarkTheme by rememberSaveable { mutableStateOf(false) }

            ReceiptScannerAppTheme(darkTheme = isDarkTheme) {
                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(
                            onUploadClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onSummaryClick = {
                                navController.navigate("summary")
                            },
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { isDarkTheme = it }
                        )
                    }

                    composable("summary") {
                        SummaryScreen(
                            receipts = receipts,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    private fun runImageOCR(imageUri: Uri) {
        try {
            val inputImage = InputImage.fromFilePath(this, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val extractedText = visionText.text
                    Log.d("OCR", "Extracted Text:\n$extractedText")

                    lifecycleScope.launch {
                        val embedding = EmbeddingUtil.getEmbedding(text = extractedText)

                        if (embedding != null) {
                            val (items, total) = parseReceiptText(extractedText)

                            val receipt = ReceiptEntity(
                                items = items,
                                total = total,
                                rawText = extractedText,
                                embedding = embedding
                            )

                            receiptDao.insertReceipt(receipt)
                            receipts.clear()
                            receipts.addAll(receiptDao.getAllReceipts())

                            Toast.makeText(this@MainActivity, "Saved to DB", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MainActivity, "Embedding failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener {
                    it.printStackTrace()
                    Toast.makeText(this, "OCR Failed", Toast.LENGTH_SHORT).show()
                }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseReceiptText(ocrText: String): Pair<List<String>, Double> {
        val lines = ocrText.split("\n")
        val itemList = mutableListOf<String>()
        var total = 0.0
        for (line in lines) {
            val lower = line.lowercase()
            if ("total" in lower || "amount" in lower) {
                Regex("""\d+(\.\d{1,2})?""").find(line)?.value?.toDoubleOrNull()?.let {
                    total = it
                }
            } else {
                val clean = line.trim()
                if (clean.isNotBlank() && clean.length <= 30) {
                    itemList.add(clean)
                }
            }
        }
        return Pair(itemList, total)
    }

    @Composable
    fun MainScreen(
        onUploadClick: () -> Unit,
        onSummaryClick: () -> Unit,
        isDarkTheme: Boolean,
        onToggleTheme: (Boolean) -> Unit
    ) {
        var searchQuery by remember { mutableStateOf("") }
        var editDialogOpen by remember { mutableStateOf(false) }
        var selectedReceipt by remember { mutableStateOf<ReceiptEntity?>(null) }
        var newTotal by remember { mutableStateOf("") }

        val context = LocalContext.current
        var filteredReceipts by remember { mutableStateOf(receipts.toList()) }

        LaunchedEffect(searchQuery, receipts) {
            if (searchQuery.isBlank()) {
                filteredReceipts = receipts
            } else {
                val queryEmbedding = EmbeddingUtil.getEmbedding(searchQuery) ?: emptyList()
                filteredReceipts = receipts
                    .asSequence()
                    .filter { it.embedding.isNotEmpty() }
                    .map { receipt ->
                        val similarity = SearchUtil.cosineSimilarity(queryEmbedding, receipt.embedding)
                        receipt to similarity
                    }
                    .sortedByDescending { it.second }
                    .take(10)
                    .map { it.first }
                    .toList()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Dark Mode")
                Switch(checked = isDarkTheme, onCheckedChange = onToggleTheme)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = onUploadClick, modifier = Modifier.fillMaxWidth()) {
                Text("Upload Receipt (Image Only)")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search receipt by item/date") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (filteredReceipts.isEmpty()) {
                    item {
                        Text("No matching receipts found.")
                    }
                } else {
                    items(filteredReceipts) { receipt ->
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Items: ${receipt.items.joinToString()} | ₹${String.format(Locale.getDefault(), "%.2f", receipt.total)}")

                            Row {
                                Button(
                                    onClick = {
                                        selectedReceipt = receipt
                                        newTotal = receipt.total.toString()
                                        editDialogOpen = true
                                    },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text("Edit")
                                }

                                Button(
                                    onClick = {
                                        lifecycleScope.launch {
                                            receiptDao.deleteReceipt(receipt)
                                            receipts.clear()
                                            receipts.addAll(receiptDao.getAllReceipts())
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }


            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { CSVExporter.exportToCSV(context, receipts) }, modifier = Modifier.fillMaxWidth()) {
                Text("Export as CSV")
            }

            Button(onClick = { PDFExporter.exportToPDF(context, receipts) }, modifier = Modifier.fillMaxWidth()) {
                Text("Export as PDF")
            }

            Button(onClick = { BackupUtil.backupToJSON(context, receipts) }, modifier = Modifier.fillMaxWidth()) {
                Text("Backup to JSON")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                val restored = BackupUtil.restoreFromJSON(context)
                if (restored != null) {
                    lifecycleScope.launch {
                        receiptDao.clearAll()
                        restored.forEach { receiptDao.insertReceipt(it) }
                        receipts.clear()
                        receipts.addAll(receiptDao.getAllReceipts())
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Restore from JSON")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = onSummaryClick, modifier = Modifier.fillMaxWidth()) {
                Text("Monthly Summary")
            }
        }

        if (editDialogOpen && selectedReceipt != null) {
            AlertDialog(
                onDismissRequest = { editDialogOpen = false },
                confirmButton = {
                    TextButton(onClick = {
                        val updatedReceipt = selectedReceipt!!.copy(
                            total = newTotal.toDoubleOrNull() ?: selectedReceipt!!.total
                        )
                        lifecycleScope.launch {
                            receiptDao.updateReceipt(updatedReceipt)
                            receipts.clear()
                            receipts.addAll(receiptDao.getAllReceipts())
                        }
                        editDialogOpen = false
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editDialogOpen = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Edit Total") },
                text = {
                    OutlinedTextField(
                        value = newTotal,
                        onValueChange = { newTotal = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Total Amount") }
                    )
                }
            )
        }
    }
}
