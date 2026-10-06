package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupValidationResult
import com.example.data.db.AppDatabase
import com.example.data.model.Budget
import com.example.data.model.BudgetStatus
import com.example.data.model.Expense
import com.example.data.model.MonthYear
import com.example.data.model.MonthlySummary
import com.example.data.preferences.PreferenceManager
import com.example.data.repository.KhorochRepository
import com.example.update.UpdateChecker
import com.example.update.UpdateState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Calendar

enum class ExpenseFilter {
    ALL,
    HIGHEST_FIRST,
    LOWEST_FIRST
}

@OptIn(ExperimentalCoroutinesApi::class)
class MonthlyKhorochViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = KhorochRepository(database.budgetDao(), database.expenseDao())
    val preferenceManager = PreferenceManager(application)
    private val app: Application = application

    private val calendar = Calendar.getInstance()
    private val initialYear = calendar.get(Calendar.YEAR)
    private val initialMonth = calendar.get(Calendar.MONTH) + 1 // 1-12

    private val _selectedMonthYear = MutableStateFlow(MonthYear(initialYear, initialMonth))
    val selectedMonthYear: StateFlow<MonthYear> = _selectedMonthYear.asStateFlow()

    // Observable budget & expenses for selected month (backed by Room with in-memory cache)
    val currentMonthSummary: StateFlow<MonthlySummary> = _selectedMonthYear
        .flatMapLatest { my ->
            repository.observeMonthlySummary(my.year, my.month)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = repository.getCachedMonthlySummary(initialYear, initialMonth)
                ?: MonthlySummary(initialYear, initialMonth, 0L, 0L, 0L, 0.0, BudgetStatus.NO_BUDGET, 0)
        )

    val currentMonthExpenses: StateFlow<List<Expense>> = _selectedMonthYear
        .flatMapLatest { my ->
            repository.getExpensesForMonth(my.year, my.month)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = repository.getCachedRecentExpenses(initialYear, initialMonth) ?: emptyList()
        )

    val allBudgets: StateFlow<List<Budget>> = repository.getAllBudgets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    // Recent 4 months summaries for Dashboard cards (scoped only to the 4 months, zero historical DB load)
    val recentMonthlySummaries: StateFlow<List<MonthlySummary>> = _selectedMonthYear
        .flatMapLatest { currentMY ->
            repository.observeRecentMonthlySummaries(currentMY)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Search and Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterOption = MutableStateFlow(ExpenseFilter.ALL)
    val filterOption: StateFlow<ExpenseFilter> = _filterOption.asStateFlow()

    val filteredExpenses: StateFlow<List<Expense>> = combine(
        currentMonthExpenses,
        _searchQuery,
        _filterOption
    ) { currentMonth, query, filter ->
        var list = currentMonth

        // Text Search (Bangla & English match)
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter { it.productName.lowercase().contains(q) }
        }

        // Sorting within the selected month's expenses
        when (filter) {
            ExpenseFilter.HIGHEST_FIRST -> list.sortedByDescending { it.totalPoisha }
            ExpenseFilter.LOWEST_FIRST -> list.sortedBy { it.totalPoisha }
            else -> list // already sorted by date DESC, id DESC
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = repository.getCachedRecentExpenses(initialYear, initialMonth) ?: emptyList()
        )

    val themeMode: StateFlow<String> = preferenceManager.themeMode
    val lastBackupTime: StateFlow<Long> = preferenceManager.lastBackupTime
    val isOnboarded: StateFlow<Boolean> = preferenceManager.isOnboarded
    val currencySymbol: StateFlow<String> = preferenceManager.currencySymbol

    fun selectMonthYear(monthYear: MonthYear) {
        _selectedMonthYear.value = monthYear
    }

    fun nextMonth() {
        _selectedMonthYear.value = _selectedMonthYear.value.next()
    }

    fun previousMonth() {
        _selectedMonthYear.value = _selectedMonthYear.value.previous()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterOption(option: ExpenseFilter) {
        _filterOption.value = option
    }

    fun setBudget(year: Int, month: Int, amountPoisha: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setBudget(year, month, amountPoisha)
        }
    }

    fun bulkSetBudgets(budgets: List<Pair<MonthYear, Long>>) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = budgets.map { (my, poisha) ->
                Budget(year = my.year, month = my.month, budgetAmountPoisha = poisha)
            }
            repository.bulkSetBudgets(list)
        }
    }

    fun copyBudgetFromPreviousMonth(targetYear: Int, targetMonth: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.copyBudgetFromPreviousMonth(targetYear, targetMonth)
            withContext(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    fun addExpense(
        date: String,
        year: Int,
        month: Int,
        day: Int,
        productName: String,
        quantity: String = "",
        unit: String = "",
        unitPricePoisha: Long = 0L,
        totalPoisha: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val expense = Expense(
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
            repository.insertExpense(expense)
        }
    }

    fun addExpense(
        date: String,
        year: Int,
        month: Int,
        day: Int,
        productName: String,
        totalPoisha: Long,
        quantity: String = ""
    ) {
        addExpense(
            date = date,
            year = year,
            month = month,
            day = day,
            productName = productName,
            quantity = quantity,
            unit = "",
            unitPricePoisha = 0L,
            totalPoisha = totalPoisha
        )
    }

    fun updateExpense(
        id: Long,
        date: String,
        year: Int,
        month: Int,
        day: Int,
        productName: String,
        quantity: String = "",
        unit: String = "",
        unitPricePoisha: Long = 0L,
        totalPoisha: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val expense = Expense(
                id = id,
                date = date,
                year = year,
                month = month,
                day = day,
                productName = productName,
                quantity = quantity,
                unit = unit,
                unitPricePoisha = unitPricePoisha,
                totalPoisha = totalPoisha,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateExpense(expense)
        }
    }

    fun updateExpense(
        id: Long,
        date: String,
        year: Int,
        month: Int,
        day: Int,
        productName: String,
        totalPoisha: Long,
        quantity: String = ""
    ) {
        updateExpense(
            id = id,
            date = date,
            year = year,
            month = month,
            day = day,
            productName = productName,
            quantity = quantity,
            unit = "",
            unitPricePoisha = 0L,
            totalPoisha = totalPoisha
        )
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExpense(expense)
        }
    }

    fun exportBackupToUri(context: Context, uri: Uri, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val budgets = repository.getAllBudgets().firstOrNull() ?: allBudgets.value
                val expenses = repository.getAllExpensesSync()
                val jsonString = BackupManager.createBackupJson(
                    budgets = budgets,
                    expenses = expenses,
                    currencySymbol = currencySymbol.value
                )

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(jsonString)
                        writer.flush()
                    }
                }

                val now = System.currentTimeMillis()
                preferenceManager.setLastBackupTime(now)
                withContext(Dispatchers.Main) {
                    onResult(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, e.localizedMessage)
                }
            }
        }
    }

    fun readAndValidateBackupUri(context: Context, uri: Uri): BackupValidationResult {
        return try {
            val sb = java.lang.StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        sb.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            }
            BackupManager.validateAndParseBackup(sb.toString())
        } catch (e: Exception) {
            BackupValidationResult(
                isValid = false,
                errorMessage = "ফাইলটি পড়া সম্ভব হয়নি: ${e.localizedMessage}"
            )
        }
    }

    fun restoreBackup(result: BackupValidationResult, replaceMode: Boolean, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.restoreBackup(result.budgets, result.expenses, replaceMode)
            preferenceManager.setLastBackupTime(System.currentTimeMillis())
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }

    fun clearAllData(onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllData()
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }

    fun setThemeMode(mode: String) {
        preferenceManager.setThemeMode(mode)
    }

    fun completeOnboarding() {
        preferenceManager.setOnboarded(true)
    }

    // ---- In-app update (GitHub Releases self-update) ----

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _updateEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val updateEvents: SharedFlow<String> = _updateEvents.asSharedFlow()

    private val _installUri = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
    val installUri: SharedFlow<Uri> = _installUri.asSharedFlow()

    fun checkForUpdate(userInitiated: Boolean) {
        when (_updateState.value) {
            is UpdateState.Checking, is UpdateState.Downloading, is UpdateState.Available -> return
            else -> Unit
        }
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            val manifest = UpdateChecker.fetchManifest()
            when {
                manifest == null -> {
                    _updateState.value = UpdateState.Idle
                    if (userInitiated) {
                        _updateEvents.tryEmit("আপডেট চেক করা যায়নি। ইন্টারনেট সংযোগ পরীক্ষা করুন।")
                    }
                }

                manifest.versionCode > BuildConfig.VERSION_CODE -> {
                    _updateState.value = UpdateState.Available(manifest)
                }

                else -> {
                    _updateState.value = UpdateState.Idle
                    if (userInitiated) {
                        _updateEvents.tryEmit("আপনি সর্বশেষ সংস্করণ ব্যবহার করছেন।")
                    }
                }
            }
        }
    }

    fun startUpdateDownload() {
        val manifest = when (val state = _updateState.value) {
            is UpdateState.Available -> state.manifest
            is UpdateState.Failed -> state.manifest
            else -> null
        } ?: return

        _updateState.value = UpdateState.Downloading(manifest, 0f)
        viewModelScope.launch {
            try {
                val dest = File(app.cacheDir, "updates/${UpdateChecker.APK_ASSET_NAME}")
                UpdateChecker.downloadFile(manifest.apkUrl, dest) { progress ->
                    _updateState.value = UpdateState.Downloading(manifest, progress)
                }
                val actualSha = withContext(Dispatchers.IO) { UpdateChecker.sha256(dest) }
                if (manifest.sha256.isNotEmpty() && !actualSha.equals(manifest.sha256, true)) {
                    dest.delete()
                    _updateState.value = UpdateState.Failed(
                        manifest,
                        "ডাউনলোড করা ফাইল যাচাই ব্যর্থ হয়েছে। আবার চেষ্টা করুন।"
                    )
                    return@launch
                }
                val uri = FileProvider.getUriForFile(
                    app,
                    "${app.packageName}.fileprovider",
                    dest
                )
                _updateState.value = UpdateState.Idle
                _installUri.emit(uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _updateState.value = UpdateState.Failed(
                    manifest,
                    "ডাউনলোড ব্যর্থ হয়েছে। আবার চেষ্টা করুন।"
                )
            }
        }
    }

    fun dismissUpdateDialog() {
        if (_updateState.value is UpdateState.Downloading) return
        _updateState.value = UpdateState.Idle
    }
}
