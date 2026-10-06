package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.MonthYear
import com.example.util.BanglaFormatter

@Composable
fun SetBudgetDialog(
    monthYear: MonthYear,
    currentBudgetPoisha: Long,
    currencySymbol: String = "৳",
    onDismiss: () -> Unit,
    onSave: (amountPoisha: Long) -> Unit,
    onCopyPrevious: (() -> Unit)? = null
) {
    val initialTaka = if (currentBudgetPoisha > 0L) {
        val taka = BanglaFormatter.poishaToTaka(currentBudgetPoisha)
        if (taka == taka.toLong().toDouble()) taka.toLong().toString() else taka.toString()
    } else ""

    var budgetText by remember { mutableStateOf(initialTaka) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val monthName = BanglaFormatter.getMonthName(monthYear.month)
    val yearStr = BanglaFormatter.toBanglaDigits(monthYear.year.toString())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "$monthName $yearStr-এর বাজেট",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "এই মাসের জন্য আপনার মোট বাজেট নির্ধারণ করুন:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = budgetText,
                    onValueChange = {
                        budgetText = it
                        if (errorText != null) errorText = null
                    },
                    label = { Text("বাজেট পরিমাণ ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorText != null,
                    supportingText = errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_amount_input")
                )

                if (onCopyPrevious != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = onCopyPrevious,
                        modifier = Modifier.testTag("copy_prev_budget_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        val prevMonth = monthYear.previous()
                        Text("আগের মাসের (${BanglaFormatter.getMonthName(prevMonth.month)}) বাজেট কপি করুন")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountTaka = budgetText.toDoubleOrNull()
                    if (amountTaka == null || amountTaka < 0) {
                        errorText = "সঠিক বাজেট পরিমাণ লিখুন"
                    } else {
                        onSave(BanglaFormatter.takaToPoisha(amountTaka))
                    }
                },
                modifier = Modifier.testTag("save_budget_button")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_budget_button")
            ) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun BulkBudgetDialog(
    currentMonthYear: MonthYear,
    currencySymbol: String = "৳",
    onDismiss: () -> Unit,
    onSaveBulk: (budgets: List<Pair<MonthYear, Long>>) -> Unit
) {
    // Show 4 upcoming months starting from current month
    val months = remember(currentMonthYear) {
        listOf(
            currentMonthYear,
            currentMonthYear.next(),
            currentMonthYear.next().next(),
            currentMonthYear.next().next().next()
        )
    }

    val amounts = remember {
        mutableStateMapOf<MonthYear, String>().apply {
            months.forEach { put(it, "") }
        }
    }

    var quickFillText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "একাধিক মাসের বাজেট সেট করুন",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "পরবর্তী মাসগুলোর জন্য বাজেট একসাথে লিখুন:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick fill row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quickFillText,
                        onValueChange = { quickFillText = it },
                        label = { Text("সব মাসে একই বাজেট") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bulk_quick_fill_input")
                    )
                    Button(
                        onClick = {
                            if (quickFillText.isNotBlank()) {
                                months.forEach { amounts[it] = quickFillText }
                            }
                        },
                        modifier = Modifier.testTag("btn_apply_all_bulk")
                    ) {
                        Text("প্রয়োগ")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                months.forEach { my ->
                    val monthName = BanglaFormatter.getMonthName(my.month)
                    val yearStr = BanglaFormatter.toBanglaDigits(my.year.toString())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$monthName $yearStr",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = amounts[my] ?: "",
                            onValueChange = { amounts[my] = it },
                            label = { Text(currencySymbol) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bulk_month_${my.year}_${my.month}")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = months.mapNotNull { my ->
                        val text = amounts[my]
                        val taka = text?.toDoubleOrNull()
                        if (taka != null && taka > 0) {
                            Pair(my, BanglaFormatter.takaToPoisha(taka))
                        } else null
                    }
                    if (result.isNotEmpty()) {
                        onSaveBulk(result)
                    } else {
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_bulk_budget_btn")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_bulk_budget_btn")
            ) {
                Text("বাতিল")
            }
        }
    )
}
