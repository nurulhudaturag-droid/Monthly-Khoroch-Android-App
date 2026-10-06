package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyExpenseGroup
import com.example.data.model.Expense
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DailyGroupCard
import com.example.ui.components.EmptyStateView
import com.example.ui.viewmodel.ExpenseFilter
import com.example.util.BanglaFormatter

@Composable
fun ExpenseScreen(
    expenses: List<Expense>,
    searchQuery: String,
    currentFilter: ExpenseFilter,
    currencySymbol: String = "৳",
    onSearchChange: (String) -> Unit,
    onFilterChange: (ExpenseFilter) -> Unit,
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onAddExpenseClick: () -> Unit,
    onViewSummary: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    // Group expenses by date
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("expense_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "খরচের তালিকা",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Search text field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("খরচ খুঁজুন (যেমন: চাল, তেল, Rice)...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "খুঁজুন",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "পরিষ্কার করুন")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter row: all / highest / lowest in one line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = currentFilter == ExpenseFilter.ALL,
                    onClick = { onFilterChange(ExpenseFilter.ALL) },
                    label = { Text("সকল খরচ", fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("filter_chip_all")
                )
                FilterChip(
                    selected = currentFilter == ExpenseFilter.HIGHEST_FIRST,
                    onClick = {
                        onFilterChange(
                            if (currentFilter == ExpenseFilter.HIGHEST_FIRST) ExpenseFilter.ALL else ExpenseFilter.HIGHEST_FIRST
                        )
                    },
                    label = { Text("সর্বোচ্চ", fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("filter_chip_highest")
                )
                FilterChip(
                    selected = currentFilter == ExpenseFilter.LOWEST_FIRST,
                    onClick = {
                        onFilterChange(
                            if (currentFilter == ExpenseFilter.LOWEST_FIRST) ExpenseFilter.ALL else ExpenseFilter.LOWEST_FIRST
                        )
                    },
                    label = { Text("সর্বনিম্ন", fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("filter_chip_lowest")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onViewSummary,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_view_summary"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("সারসংক্ষেপ দেখুন")
            }
        }

        if (groupedExpenses.isEmpty()) {
            item {
                Spacer(modifier = Modifier.height(30.dp))
                if (searchQuery.isNotBlank()) {
                    EmptyStateView(
                        message = "\"$searchQuery\" দিয়ে কোনো খরচ পাওয়া যায়নি।",
                        buttonText = "সার্চ মুছুন",
                        onButtonClick = { onSearchChange("") }
                    )
                } else {
                    EmptyStateView(
                        message = "এখনো কোনো খরচ যোগ করা হয়নি।",
                        buttonText = "+ প্রথম খরচ যোগ করুন",
                        onButtonClick = onAddExpenseClick
                    )
                }
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
            Spacer(modifier = Modifier.height(80.dp)) // Safe padding for bottom bar
        }
    }
}
