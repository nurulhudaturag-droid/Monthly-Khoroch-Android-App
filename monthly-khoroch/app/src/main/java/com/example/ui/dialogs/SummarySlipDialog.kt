package com.example.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Expense
import com.example.data.model.MonthYear
import com.example.data.model.MonthlySummary
import com.example.util.SummarySlipExporter
import com.example.util.BanglaFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SummarySlipDialog(
    monthYear: MonthYear,
    summary: MonthlySummary,
    expenses: List<Expense>,
    currencySymbol: String = "৳",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(monthYear.year, monthYear.month, summary.totalExpensePoisha, expenses.size) {
        bitmap = withContext(Dispatchers.Default) {
            SummarySlipExporter.createSlipBitmap(monthYear, summary, expenses, currencySymbol)
                .asImageBitmap()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${BanglaFormatter.getMonthName(monthYear.month)} · সারসংক্ষেপ",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val img = bitmap
                    if (img != null) {
                        Image(
                            bitmap = img,
                            contentDescription = "মাসিক সারসংক্ষেপ slip",
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .testTag("slip_preview")
                        )
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val img = bitmap ?: return@Button
                    busy = true
                    val bit = img.asAndroidBitmap()
                    val saved = SummarySlipExporter.saveToDownloads(context, bit, monthYear)
                    if (saved == null) {
                        SummarySlipExporter.shareFallback(context, bit, monthYear)
                    }
                    Toast.makeText(
                        context,
                        saved ?: "পুরনো Android-এ শেয়ার মেনু খোলা হয়েছে।",
                        Toast.LENGTH_LONG
                    ).show()
                    busy = false
                    onDismiss()
                },
                enabled = bitmap != null,
                modifier = Modifier.testTag("btn_download_summary")
            ) {
                Text(if (busy) "তৈরি হচ্ছে..." else "ডাউনলোড করুন")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_close_summary")
            ) {
                Text("বন্ধ করুন")
            }
        }
    )
}