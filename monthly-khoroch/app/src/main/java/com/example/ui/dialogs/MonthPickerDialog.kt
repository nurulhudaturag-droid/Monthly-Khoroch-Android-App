package com.example.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.MonthYear
import com.example.util.BanglaFormatter
import java.util.Calendar

@Composable
fun MonthPickerDialog(
    current: MonthYear,
    onSelect: (MonthYear) -> Unit,
    onDismiss: () -> Unit
) {
    var year by remember(current.year) { mutableStateOf(current.year) }
    val today = remember { Calendar.getInstance() }
    val todayMonth = MonthYear(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "মাস নির্বাচন করুন",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                // Year stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { year -= 1 },
                        modifier = Modifier.testTag("btn_pick_prev_year")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "আগের বছর",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = BanglaFormatter.toBanglaDigits(year.toString()),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { year += 1 },
                        modifier = Modifier.testTag("btn_pick_next_year")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "পরের বছর",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 12-month grid (4 rows x 3 columns)
                for (rowStart in 1..10 step 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (m in rowStart until rowStart + 3) {
                            val selected = year == current.year && m == current.month
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelect(MonthYear(year, m)) }
                                    .testTag("btn_pick_month_$m")
                            ) {
                                Text(
                                    text = BanglaFormatter.getMonthName(m),
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSelect(todayMonth) },
                modifier = Modifier.testTag("btn_go_today_month")
            ) {
                Text("চলতি মাসে যান")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_close_month_picker")
            ) {
                Text("বন্ধ")
            }
        }
    )
}