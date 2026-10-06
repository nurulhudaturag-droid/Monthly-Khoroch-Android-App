package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyExpenseGroup
import com.example.data.model.Expense
import com.example.data.model.MonthYear
import com.example.data.model.MonthlySummary
import com.example.ui.components.BudgetProgressBar
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DailyGroupCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.components.StatusBadge
import com.example.ui.dialogs.MonthPickerDialog
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.util.BanglaFormatter

@Composable
fun MonthScreen(
    currentMonthYear: MonthYear,
    summary: MonthlySummary,
    expenses: List<Expense>,
    currencySymbol: String = "৳",
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSetBudgetClick: () -> Unit,
    onBulkBudgetClick: () -> Unit,
    onCopyPreviousBudgetClick: () -> Unit,
    onPickMonth: (MonthYear) -> Unit = {},
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onAddExpenseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val groupedExpenses = remember(expenses) {
        expenses.groupBy { it.date }.map { (date, list) ->
            DailyExpenseGroup(
                date = date,
                totalPoisha = list.sumOf { it.totalPoisha },
                expenses = list
            )
        }
    }

    if (expenseToDelete != null) {
        ConfirmDeleteDialog(
            itemDescription = "\"${expenseToDelete?.productName}\" খরচটি",
            onConfirm = {
                expenseToDelete?.let { onDeleteExpense(it) }
                expenseToDelete = null
            },
            onDismiss = {
                expenseToDelete = null
            }
        )
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            current = currentMonthYear,
            onSelect = { my ->
                onPickMonth(my)
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("month_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "মাসভিত্তিক বিশ্লেষণ",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Month navigation selector
            MonthSelectorHeader(
                currentMonthYear = currentMonthYear,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
                onOpenPicker = { showMonthPicker = true }
            )
        }

        // Budget & Financial Overview Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("month_overview_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "এই মাসের বাজেট",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (summary.budgetPoisha > 0L) {
                                    BanglaFormatter.formatCurrency(summary.budgetPoisha, currencySymbol)
                                } else "বাজেট সেট করা হয়নি",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        StatusBadge(status = summary.status)
                    }

                    if (summary.budgetPoisha > 0L) {
                        Spacer(modifier = Modifier.height(14.dp))
                        BudgetProgressBar(percentage = summary.usedPercentage)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ব্যয়: ${BanglaFormatter.formatCurrency(summary.totalExpensePoisha, currencySymbol)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = BanglaFormatter.formatPercentage(summary.usedPercentage),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.usedPercentage >= 100.0) StatusRed else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Remaining / Exceeded Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            val isExceeded = summary.remainingPoisha < 0
                            Text(
                                text = if (isExceeded) "বাজেট অতিক্রম করেছে" else "অবশিষ্ট বাজেট",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isExceeded) StatusRed else StatusGreen
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isExceeded) {
                                    BanglaFormatter.formatCurrency(Math.abs(summary.remainingPoisha), currencySymbol)
                                } else {
                                    BanglaFormatter.formatCurrency(summary.remainingPoisha, currencySymbol)
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isExceeded) StatusRed else StatusGreen
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "মোট খরচ সংখ্যা",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${BanglaFormatter.toBanglaDigits(summary.expenseCount.toString())} টি",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons: Set Budget, Copy Prev, Bulk
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSetBudgetClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_month_set_budget"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(if (summary.budgetPoisha > 0L) "বাজেট পরিবর্তন" else "বাজেট নির্ধারণ")
                        }

                        OutlinedButton(
                            onClick = onBulkBudgetClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_month_bulk_budget"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text("একাধিক মাস")
                        }
                    }

                    // Copy previous month budget quick button
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = onCopyPreviousBudgetClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_month_copy_prev")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        val prevMonth = currentMonthYear.previous()
                        Text("আগের মাসের (${BanglaFormatter.getMonthName(prevMonth.month)}) বাজেট কপি করুন")
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "${BanglaFormatter.getMonthName(currentMonthYear.month)}-এর খরচসমূহ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Expense entries for this month
        if (groupedExpenses.isEmpty()) {
            item {
                EmptyStateView(
                    message = "এই মাসে এখনো কোনো খরচ যোগ করা হয়নি।",
                    buttonText = "+ প্রথম খরচ যোগ করুন",
                    onButtonClick = onAddExpenseClick
                )
            }
        } else {
            items(groupedExpenses, key = { it.date }) { group ->
                DailyGroupCard(
                    date = group.date,
                    totalPoisha = group.totalPoisha,
                    expenses = group.expenses,
                    currencySymbol = currencySymbol,
                    onEditExpense = onEditExpense,
                    onDeleteExpense = { expenseToDelete = it }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
