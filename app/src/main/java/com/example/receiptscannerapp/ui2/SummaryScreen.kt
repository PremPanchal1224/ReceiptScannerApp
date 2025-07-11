package com.example.receiptscannerapp.ui2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.receiptscannerapp.data.ReceiptEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    receipts: List<ReceiptEntity>,
    onBackClick: () -> Unit
) {
    val monthlyGrouped = receipts.groupBy {
        val date = Date(it.date)
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date)
    }

    val monthlySums = monthlyGrouped.mapValues { entry ->
        entry.value.sumOf { it.total }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monthly Summary") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (monthlySums.isEmpty()) {
                Text("No receipts to summarize.")


                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    items(monthlySums.entries.toList()) { (month, total) ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "$month: ₹${String.format(Locale.getDefault(), "%.2f", total)}",
                                style = MaterialTheme.typography.titleMedium
                            )

                            val itemsThisMonth = monthlyGrouped[month]?.flatMap { it.items } ?: emptyList()
                            val suggestion = generateMonthlyTip(itemsThisMonth, total)

                            if (suggestion.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "💡 $suggestion",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

fun categorizeItem(item: String): String {
    val lowerItem = item.lowercase()

    return when {
        listOf("burger", "pizza", "fries", "momos", "roll", "shawarma", "sandwich").any { it in lowerItem } -> "fast_food"
        listOf("chips", "snack", "kurkure", "lays", "biscuit", "namkeen").any { it in lowerItem } -> "snacks"
        listOf("milk", "bread", "rice", "vegetable", "fruit", "grocery").any { it in lowerItem } -> "groceries"
        listOf("coffee", "tea", "cafe").any { it in lowerItem } -> "beverages"
        else -> "others"
    }
}

// 💡 Spending Tip Generator
fun generateMonthlyTip(items: List<String>, total: Double): String {
    val categoryTotals = mutableMapOf<String, Double>()

    items.forEach { item ->
        val category = categorizeItem(item)
        categoryTotals[category] = categoryTotals.getOrDefault(category, 0.0) + 1.0 // Simple proxy count per item
    }

    return when {
        (categoryTotals["snacks"] ?: 0.0) >= 5 -> "You bought snacks multiple times this month. Try limiting evening takeouts."
        (categoryTotals["fast_food"] ?: 0.0) >= 4 -> "You spent often on fast food. Consider healthier alternatives."
        (categoryTotals["beverages"] ?: 0.0) >= 5 -> "Frequent beverage purchases. Brew at home to save money."
        total > 5000 -> "You’ve spent over ₹5000. Review your expenses to optimize savings."
        else -> ""
    }
}
