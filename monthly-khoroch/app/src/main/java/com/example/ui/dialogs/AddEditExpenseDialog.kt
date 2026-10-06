package com.example.ui.dialogs

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Expense
import com.example.ui.components.FutureDateConfirmDialog
import com.example.util.BanglaFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AddEditExpenseDialog(
    initialExpense: Expense? = null,
    defaultDate: String? = null,
    currencySymbol: String = "৳",
    onDismiss: () -> Unit,
    onSave: (
        date: String,
        year: Int,
        month: Int,
        day: Int,
        productName: String,
        quantity: Double,
        unit: String,
        unitPricePoisha: Long,
        totalPoisha: Long
    ) -> Unit
) {
    val context = LocalContext.current
    val todayCal = Calendar.getInstance()
    val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayStr = isoDateFormat.format(todayCal.time)

    var productName by remember { mutableStateOf(initialExpense?.productName ?: "") }
    var selectedDate by remember { mutableStateOf(initialExpense?.date ?: defaultDate ?: todayStr) }

    // User enters the actual total amount paid directly. No unit price, no multiplication!
    var amountText by remember {
        mutableStateOf(
            initialExpense?.let {
                val taka = BanglaFormatter.poishaToTaka(it.totalPoisha)
                if (taka == taka.toLong().toDouble()) taka.toLong().toString()
                else taka.toString()
            } ?: ""
        )
    }

    // Optional quantity: does NOT multiply by price
    var quantityText by remember {
        mutableStateOf(
            initialExpense?.let {
                if (it.quantity > 1.0) {
                    if (it.quantity == it.quantity.toLong().toDouble()) it.quantity.toLong().toString()
                    else it.quantity.toString()
                } else ""
            } ?: ""
        )
    }

    var nameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var quantityError by remember { mutableStateOf<String?>(null) }
    var showFutureWarning by remember { mutableStateOf(false) }

    // Direct calculation of total from user entered amount
    val parsedAmountTaka = amountText.toDoubleOrNull() ?: 0.0
    val totalPoisha = BanglaFormatter.takaToPoisha(parsedAmountTaka)
    val parsedQty = quantityText.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 1.0

    fun performSave() {
        val parts = selectedDate.split("-")
        val year = parts[0].toIntOrNull() ?: todayCal.get(Calendar.YEAR)
        val month = parts[1].toIntOrNull() ?: (todayCal.get(Calendar.MONTH) + 1)
        val day = parts[2].toIntOrNull() ?: todayCal.get(Calendar.DAY_OF_MONTH)

        onSave(
            selectedDate,
            year,
            month,
            day,
            productName.trim(),
            parsedQty,
            "", // Unit completely removed
            0L, // Unit price completely removed
            totalPoisha // User entered actual total amount paid
        )
    }

    fun validateAndProceed() {
        var hasError = false
        if (productName.isBlank()) {
            nameError = "পণ্যের নাম বা বিবরণ লিখুন"
            hasError = true
        } else {
            nameError = null
        }

        if (amountText.isBlank() || parsedAmountTaka <= 0.0) {
            amountError = "সঠিক খরচের পরিমাণ লিখুন (০ এর বেশি হতে হবে)"
            hasError = true
        } else {
            amountError = null
        }

        if (quantityText.isNotBlank()) {
            val q = quantityText.toDoubleOrNull()
            if (q == null || q <= 0.0) {
                quantityError = "সঠিক পরিমাণ লিখুন (যেমন: ১, ২, ৫)"
                hasError = true
            } else {
                quantityError = null
            }
        } else {
            quantityError = null
        }

        if (hasError) return

        // Check if date is in the future
        try {
            val dateObj = isoDateFormat.parse(selectedDate)
            val todayMidnight = isoDateFormat.parse(todayStr)
            if (dateObj != null && todayMidnight != null && dateObj.after(todayMidnight)) {
                showFutureWarning = true
                return
            }
        } catch (_: Exception) {}

        performSave()
    }

    if (showFutureWarning) {
        FutureDateConfirmDialog(
            dateString = selectedDate,
            onConfirm = {
                showFutureWarning = false
                performSave()
            },
            onDismiss = {
                showFutureWarning = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialExpense == null) "নতুন খরচ যোগ করুন" else "খরচ সম্পাদনা করুন",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Product Name Field
                OutlinedTextField(
                    value = productName,
                    onValueChange = {
                        productName = it
                        if (nameError != null) nameError = null
                    },
                    label = { Text("পণ্যের নাম বা বিবরণ") },
                    placeholder = { Text("যেমন: চাল, তেল, বাড়ি ভাড়া, বিদ্যুৎ বিল...") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_product_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Date Picker field
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cal = Calendar.getInstance()
                            val parts = selectedDate.split("-")
                            if (parts.size == 3) {
                                cal.set(Calendar.YEAR, parts[0].toIntOrNull() ?: cal.get(Calendar.YEAR))
                                cal.set(Calendar.MONTH, (parts[1].toIntOrNull() ?: 1) - 1)
                                cal.set(Calendar.DAY_OF_MONTH, parts[2].toIntOrNull() ?: 1)
                            }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    selectedDate = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .testTag("expense_date_picker_button"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "তারিখ",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = BanglaFormatter.formatDateBangla(selectedDate),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "তারিখ পরিবর্তন করুন",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actual Total Amount Paid Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        if (amountError != null) amountError = null
                    },
                    label = { Text("মোট খরচের পরিমাণ ($currencySymbol)") },
                    placeholder = { Text("যেমন: ৫০০") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_price_input")
                        .testTag("expense_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Optional Quantity Field (does not multiply by price)
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it
                        if (quantityError != null) quantityError = null
                    },
                    label = { Text("পরিমাণ (ঐচ্ছিক)") },
                    placeholder = { Text("যেমন: ১, ২, ৫") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = quantityError != null,
                    supportingText = quantityError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_quantity_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Total Amount Paid Summary Card
                if (totalPoisha > 0L) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "পরিশোধিত মোট টাকা:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = BanglaFormatter.formatCurrency(totalPoisha, currencySymbol),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { validateAndProceed() },
                modifier = Modifier.testTag("save_expense_button")
            ) {
                Text(if (initialExpense == null) "সংরক্ষণ করুন" else "আপডেট করুন")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_expense_button")
            ) {
                Text("বাতিল")
            }
        }
    )
}
