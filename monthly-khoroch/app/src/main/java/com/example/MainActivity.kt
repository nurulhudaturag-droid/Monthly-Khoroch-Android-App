package com.example

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Expense
import com.example.data.model.MonthYear
import com.example.ui.dialogs.AddEditExpenseDialog
import com.example.ui.dialogs.BulkBudgetDialog
import com.example.ui.dialogs.SetBudgetDialog
import com.example.ui.dialogs.UpdateDialog
import com.example.update.UpdateState
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpenseScreen
import com.example.ui.screens.MonthScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MonthlyKhorochTheme
import com.example.ui.viewmodel.MonthlyKhorochViewModel
import com.example.util.BanglaFormatter
import kotlinx.coroutines.launch

enum class MainTab {
    HOME,
    EXPENSES,
    MONTH,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: MonthlyKhorochViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isOnboarded by viewModel.isOnboarded.collectAsStateWithLifecycle()
            val updateState by viewModel.updateState.collectAsStateWithLifecycle()

            // One-shot update events (toasts for manual checks)
            LaunchedEffect(Unit) {
                viewModel.updateEvents.collect { message ->
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                }
            }

            // Hand the downloaded APK over to the system installer (user-driven, no silent install)
            LaunchedEffect(Unit) {
                viewModel.installUri.collect { uri ->
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(
                            this@MainActivity,
                            "ইনস্টল করার অ্যাপ খুঁজে পাওয়া যায়নি।",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            // Automatic update check once the app is opened (silently ignores failures)
            LaunchedEffect(isOnboarded) {
                if (isOnboarded) viewModel.checkForUpdate(userInitiated = false)
            }

            MonthlyKhorochTheme(themePreference = themeMode) {
                if (!isOnboarded) {
                    OnboardingScreen(
                        onStartClick = { viewModel.completeOnboarding() }
                    )
                } else {
                    MainAppScaffold(viewModel = viewModel)
                }

                UpdateDialog(
                    state = updateState,
                    currentVersionName = BuildConfig.VERSION_NAME,
                    onInstallClick = { viewModel.startUpdateDownload() },
                    onDismiss = { viewModel.dismissUpdateDialog() }
                )
            }
        }
    }
}

@Composable
fun MainAppScaffold(viewModel: MonthlyKhorochViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableStateOf(MainTab.HOME) }

    // Dialog states
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var showSetBudgetDialog by remember { mutableStateOf(false) }
    var showBulkBudgetDialog by remember { mutableStateOf(false) }

    // Core StateFlows needed across the scaffold and dashboard
    val selectedMonthYear by viewModel.selectedMonthYear.collectAsStateWithLifecycle()
    val currentMonthSummary by viewModel.currentMonthSummary.collectAsStateWithLifecycle()
    val recentMonthlySummaries by viewModel.recentMonthlySummaries.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    // Back button behavior: Return to HOME if on other tabs
    BackHandler(enabled = currentTab != MainTab.HOME) {
        currentTab = MainTab.HOME
    }

    // Add or Edit Expense Dialog
    if (showAddExpenseDialog || expenseToEdit != null) {
        AddEditExpenseDialog(
            initialExpense = expenseToEdit,
            currencySymbol = currencySymbol,
            onDismiss = {
                showAddExpenseDialog = false
                expenseToEdit = null
            },
            onSave = { date, year, month, day, productName, quantity, unit, unitPricePoisha, totalPoisha ->
                if (expenseToEdit != null) {
                    viewModel.updateExpense(
                        id = expenseToEdit!!.id,
                        date = date,
                        year = year,
                        month = month,
                        day = day,
                        productName = productName,
                        quantity = quantity,
                        unit = unit,
                        unitPricePoisha = unitPricePoisha,
                        totalPoisha = totalPoisha
                    )
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("খরচ সফলভাবে আপডেট করা হয়েছে।")
                    }
                } else {
                    viewModel.addExpense(
                        date = date,
                        year = year,
                        month = month,
                        day = day,
                        productName = productName,
                        quantity = quantity,
                        unit = unit,
                        unitPricePoisha = unitPricePoisha,
                        totalPoisha = totalPoisha
                    )
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("নতুন খরচ যোগ করা হয়েছে।")
                    }
                }
                showAddExpenseDialog = false
                expenseToEdit = null
            }
        )
    }

    // Single Month Budget Dialog
    if (showSetBudgetDialog) {
        SetBudgetDialog(
            monthYear = selectedMonthYear,
            currentBudgetPoisha = currentMonthSummary.budgetPoisha,
            currencySymbol = currencySymbol,
            onDismiss = { showSetBudgetDialog = false },
            onSave = { poisha ->
                viewModel.setBudget(selectedMonthYear.year, selectedMonthYear.month, poisha)
                showSetBudgetDialog = false
                coroutineScope.launch {
                    val takaStr = BanglaFormatter.formatCurrency(poisha, currencySymbol)
                    snackbarHostState.showSnackbar("${BanglaFormatter.getMonthName(selectedMonthYear.month)}-এর বাজেট $takaStr সেট করা হয়েছে।")
                }
            },
            onCopyPrevious = {
                val prev = selectedMonthYear.previous()
                viewModel.copyBudgetFromPreviousMonth(selectedMonthYear.year, selectedMonthYear.month) { success ->
                    if (success) {
                        showSetBudgetDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("${BanglaFormatter.getMonthName(prev.month)}-এর বাজেট কপি করা হয়েছে।")
                        }
                    } else {
                        Toast.makeText(context, "${BanglaFormatter.getMonthName(prev.month)}-এর কোনো বাজেট পাওয়া যায়নি।", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Bulk Budget Dialog
    if (showBulkBudgetDialog) {
        BulkBudgetDialog(
            currentMonthYear = selectedMonthYear,
            currencySymbol = currencySymbol,
            onDismiss = { showBulkBudgetDialog = false },
            onSaveBulk = { list ->
                viewModel.bulkSetBudgets(list)
                showBulkBudgetDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("${BanglaFormatter.toBanglaDigits(list.size.toString())} টি মাসের বাজেট সফলভাবে সেট করা হয়েছে।")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == MainTab.HOME,
                    onClick = { currentTab = MainTab.HOME },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "হোম"
                        )
                    },
                    label = { Text("হোম", fontWeight = if (currentTab == MainTab.HOME) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_home")
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.EXPENSES,
                    onClick = { currentTab = MainTab.EXPENSES },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.EXPENSES) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                            contentDescription = "খরচ"
                        )
                    },
                    label = { Text("খরচ", fontWeight = if (currentTab == MainTab.EXPENSES) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_expenses")
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.MONTH,
                    onClick = { currentTab = MainTab.MONTH },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.MONTH) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                            contentDescription = "মাস"
                        )
                    },
                    label = { Text("মাস", fontWeight = if (currentTab == MainTab.MONTH) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_month")
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.SETTINGS,
                    onClick = { currentTab = MainTab.SETTINGS },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "সেটিংস"
                        )
                    },
                    label = { Text("সেটিংস", fontWeight = if (currentTab == MainTab.SETTINGS) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        },
        floatingActionButton = {
            if (currentTab == MainTab.HOME || currentTab == MainTab.EXPENSES || currentTab == MainTab.MONTH) {
                FloatingActionButton(
                    onClick = {
                        expenseToEdit = null
                        showAddExpenseDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .testTag("fab_add_expense")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "খরচ যোগ করুন")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.HOME -> {
                    DashboardScreen(
                        currentSummary = currentMonthSummary,
                        recentSummaries = recentMonthlySummaries,
                        currencySymbol = currencySymbol,
                        onAddExpenseClick = {
                            expenseToEdit = null
                            showAddExpenseDialog = true
                        },
                        onSetBudgetClick = { showSetBudgetDialog = true },
                        onMonthSummaryClick = { my ->
                            viewModel.selectMonthYear(my)
                            currentTab = MainTab.MONTH
                        }
                    )
                }

                MainTab.EXPENSES -> {
                    val filteredExpenses by viewModel.filteredExpenses.collectAsStateWithLifecycle()
                    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                    val filterOption by viewModel.filterOption.collectAsStateWithLifecycle()

                    ExpenseScreen(
                        expenses = filteredExpenses,
                        searchQuery = searchQuery,
                        currentFilter = filterOption,
                        currencySymbol = currencySymbol,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onFilterChange = { viewModel.setFilterOption(it) },
                        onEditExpense = { expense -> expenseToEdit = expense },
                        onDeleteExpense = { expense ->
                            viewModel.deleteExpense(expense)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("\"${expense.productName}\" মুছে ফেলা হয়েছে।")
                            }
                        },
                        onAddExpenseClick = {
                            expenseToEdit = null
                            showAddExpenseDialog = true
                        }
                    )
                }

                MainTab.MONTH -> {
                    val currentMonthExpenses by viewModel.currentMonthExpenses.collectAsStateWithLifecycle()

                    MonthScreen(
                        currentMonthYear = selectedMonthYear,
                        summary = currentMonthSummary,
                        expenses = currentMonthExpenses,
                        currencySymbol = currencySymbol,
                        onPreviousMonth = { viewModel.previousMonth() },
                        onNextMonth = { viewModel.nextMonth() },
                        onSetBudgetClick = { showSetBudgetDialog = true },
                        onBulkBudgetClick = { showBulkBudgetDialog = true },
                        onCopyPreviousBudgetClick = {
                            val prev = selectedMonthYear.previous()
                            viewModel.copyBudgetFromPreviousMonth(selectedMonthYear.year, selectedMonthYear.month) { success ->
                                if (success) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("${BanglaFormatter.getMonthName(prev.month)}-এর বাজেট কপি করা হয়েছে।")
                                    }
                                } else {
                                    Toast.makeText(context, "${BanglaFormatter.getMonthName(prev.month)}-এর কোনো বাজেট পাওয়া যায়নি।", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onEditExpense = { expense -> expenseToEdit = expense },
                        onDeleteExpense = { expense ->
                            viewModel.deleteExpense(expense)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("\"${expense.productName}\" মুছে ফেলা হয়েছে।")
                            }
                        },
                        onAddExpenseClick = {
                            expenseToEdit = null
                            showAddExpenseDialog = true
                        }
                    )
                }

                MainTab.SETTINGS -> {
                    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
                    val lastBackupTime by viewModel.lastBackupTime.collectAsStateWithLifecycle()

                    SettingsScreen(
                        currentThemeMode = themeMode,
                        lastBackupTimestamp = lastBackupTime,
                        currencySymbol = currencySymbol,
                        currentVersionName = BuildConfig.VERSION_NAME,
                        isCheckingUpdate = updateState is UpdateState.Checking,
                        onThemeChange = { viewModel.setThemeMode(it) },
                        onCheckUpdate = { viewModel.checkForUpdate(userInitiated = true) },
                        onExportBackup = { uri, callback ->
                            viewModel.exportBackupToUri(context, uri, callback)
                        },
                        onReadBackupUri = { uri ->
                            viewModel.readAndValidateBackupUri(context, uri)
                        },
                        onRestoreBackup = { result, replaceMode ->
                            viewModel.restoreBackup(result, replaceMode) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("ব্যাকআপ সফলভাবে রিস্টোর করা হয়েছে।")
                                }
                            }
                        },
                        onClearAllData = {
                            viewModel.clearAllData {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("সকল হিসাব ও বাজেট মুছে ফেলা হয়েছে।")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
