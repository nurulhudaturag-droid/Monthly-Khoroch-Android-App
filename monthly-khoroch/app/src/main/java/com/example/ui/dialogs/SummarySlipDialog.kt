package com.example.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    var pages by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(monthYear.year, monthYear.month, summary.totalExpensePoisha, expenses.size) {
        pages = withContext(Dispatchers.Default) {
            SummarySlipExporter.createSlipBitmaps(monthYear, summary, expenses, currencySymbol)
                .map { it.asImageBitmap() }
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
                    if (pages.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .testTag("slip_preview")
                        ) {
                            pages.forEachIndexed { index, page ->
                                Image(
                                    bitmap = page,
                                    contentDescription = "মাসিক সারসংক্ষেপ slip",
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (index < pages.lastIndex) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pages.isEmpty()) return@Button
                    busy = true
                    val bitmaps = pages.map { it.asAndroidBitmap() }
                    val saved = SummarySlipExporter.saveToDownloads(context, bitmaps, monthYear)
                    if (saved == null) {
                        SummarySlipExporter.shareFallback(context, bitmaps, monthYear)
                    }
                    Toast.makeText(
                        context,
                        saved ?: "পুরনো Android-এ শেয়ার মেনু খোলা হয়েছে।",
                        Toast.LENGTH_LONG
                    ).show()
                    busy = false
                    onDismiss()
                },
                enabled = pages.isNotEmpty(),
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