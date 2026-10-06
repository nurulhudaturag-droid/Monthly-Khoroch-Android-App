package com.example.data.repository

import com.example.data.cache.KhorochMemoryCache
import com.example.data.db.BudgetDao
import com.example.data.db.ExpenseDao
import com.example.data.model.Budget
import com.example.data.model.BudgetStatus
import com.example.data.model.Expense
import com.example.data.model.MonthYear
import com.example.data.model.MonthlySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach

class KhorochRepository(
    private val budgetDao: BudgetDao,
    private val expenseDao: ExpenseDao,
    private val memoryCache: KhorochMemoryCache = KhorochMemoryCache()
) {

    fun getAllBudgets(): Flow<List<Budget>> = budgetDao.getAllBudgets()

    fun getBudgetForMonth(year: Int, month: Int): Flow<Budget?> =
        budgetDao.getBudgetForMonth(year, month).onEach { budget ->
            if (budget != null) {
                memoryCache.putBudget(year, month, budget)
            }
        }

    suspend fun getBudgetForMonthSync(year: Int, month: Int): Budget? {
        val cached = memoryCache.getBudget(year, month)
        if (cached != null) return cached
        val budget = budgetDao.getBudgetForMonthSync(year, month)
        if (budget != null) memoryCache.putBudget(year, month, budget)
        return budget
    }

    suspend fun setBudget(year: Int, month: Int, amountPoisha: Long): Long {
        val existing = budgetDao.getBudgetForMonthSync(year, month)
        val budget = if (existing != null) {
            existing.copy(budgetAmountPoisha = amountPoisha, updatedAt = System.currentTimeMillis())
        } else {
            Budget(year = year, month = month, budgetAmountPoisha = amountPoisha)
        }
        val id = budgetDao.insertBudget(budget)
        // Invalidate in-memory cache immediately on budget modification
        memoryCache.invalidateMonth(year, month)
        return id
    }

    suspend fun bulkSetBudgets(budgets: List<Budget>): List<Long> {
        val ids = budgetDao.insertBudgets(budgets)
        budgets.forEach { memoryCache.invalidateMonth(it.year, it.month) }
        return ids
    }

    suspend fun copyBudgetFromPreviousMonth(targetYear: Int, targetMonth: Int): Boolean {
        val prev = MonthYear(targetYear, targetMonth).previous()
        val prevBudget = budgetDao.getBudgetForMonthSync(prev.year, prev.month) ?: return false
        setBudget(targetYear, targetMonth, prevBudget.budgetAmountPoisha)
        memoryCache.invalidateMonth(targetYear, targetMonth)
        return true
    }

    suspend fun deleteBudget(year: Int, month: Int) {
        budgetDao.deleteBudgetForMonth(year, month)
        memoryCache.invalidateMonth(year, month)
    }

    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()

    suspend fun getAllExpensesSync(): List<Expense> = expenseDao.getAllExpensesSync()

    fun getExpensesForMonth(year: Int, month: Int): Flow<List<Expense>> =
        expenseDao.getExpensesForMonth(year, month).onEach { expenses ->
            memoryCache.putRecentExpenses(year, month, expenses)
        }

    fun searchExpenses(query: String): Flow<List<Expense>> =
        expenseDao.searchExpenses(query)

    suspend fun insertExpense(expense: Expense): Long {
        val id = expenseDao.insertExpense(expense)
        // Invalidate in-memory cache immediately on new expense
        memoryCache.invalidateMonth(expense.year, expense.month)
        return id
    }

    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
        // Invalidate in-memory cache immediately on expense update
        memoryCache.invalidateMonth(expense.year, expense.month)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
        // Invalidate in-memory cache immediately on expense deletion
        memoryCache.invalidateMonth(expense.year, expense.month)
    }

    suspend fun deleteExpenseById(id: Long) {
        expenseDao.deleteExpenseById(id)
        memoryCache.clear()
    }

    /**
     * Observes monthly summary dynamically combining budget & expenses for given month
     * Synchronizes with in-memory cache for fast UI access.
     */
    fun observeMonthlySummary(year: Int, month: Int): Flow<MonthlySummary> {
        return combine(
            budgetDao.getBudgetForMonth(year, month),
            expenseDao.getExpensesForMonth(year, month)
        ) { budget, expenses ->
            val summary = calculateSummary(year, month, budget, expenses)
            // Cache frequently accessed summary, budget & expense state
            memoryCache.putMonthlySummary(year, month, summary)
            if (budget != null) memoryCache.putBudget(year, month, budget)
            memoryCache.putRecentExpenses(year, month, expenses)
            summary
        }
    }

    /**
     * Observes recent 4 months summaries without loading all historical database records
     */
    fun observeRecentMonthlySummaries(currentMY: MonthYear): Flow<List<MonthlySummary>> {
        val m0 = currentMY
        val m1 = m0.previous()
        val m2 = m1.previous()
        val m3 = m2.previous()

        return combine(
            observeMonthlySummary(m0.year, m0.month),
            observeMonthlySummary(m1.year, m1.month),
            observeMonthlySummary(m2.year, m2.month),
            observeMonthlySummary(m3.year, m3.month)
        ) { s0, s1, s2, s3 ->
            listOf(s0, s1, s2, s3)
        }
    }

    suspend fun getMonthlySummarySync(year: Int, month: Int): MonthlySummary {
        val cached = memoryCache.getMonthlySummary(year, month)
        if (cached != null) return cached

        val budget = budgetDao.getBudgetForMonthSync(year, month)
        val expenses = expenseDao.getExpensesForMonth(year, month).firstOrNull() ?: emptyList()
        val summary = calculateSummary(year, month, budget, expenses)
        memoryCache.putMonthlySummary(year, month, summary)
        return summary
    }

    // Direct cache accessors for immediate ViewModel / UI access
    fun getCachedMonthlySummary(year: Int, month: Int): MonthlySummary? =
        memoryCache.getMonthlySummary(year, month)

    fun getCachedRecentExpenses(year: Int, month: Int): List<Expense>? =
        memoryCache.getRecentExpenses(year, month)

    fun getCachedBudget(year: Int, month: Int): Budget? =
        memoryCache.getBudget(year, month)

    suspend fun restoreBackup(budgets: List<Budget>, expenses: List<Expense>, replaceMode: Boolean) {
        if (replaceMode) {
            budgetDao.clearAllBudgets()
            expenseDao.clearAllExpenses()
        }
        if (budgets.isNotEmpty()) {
            budgetDao.insertBudgets(budgets)
        }
        if (expenses.isNotEmpty()) {
            expenseDao.insertExpenses(expenses)
        }
        memoryCache.clear()
    }

    suspend fun clearAllData() {
        budgetDao.clearAllBudgets()
        expenseDao.clearAllExpenses()
        memoryCache.clear()
    }

    companion object {
        fun calculateSummary(
            year: Int,
            month: Int,
            budget: Budget?,
            expenses: List<Expense>
        ): MonthlySummary {
            val budgetPoisha = budget?.budgetAmountPoisha ?: 0L
            val totalExpensePoisha = expenses.sumOf { it.totalPoisha }
            val remainingPoisha = budgetPoisha - totalExpensePoisha
            val usedPercentage = if (budgetPoisha > 0L) {
                (totalExpensePoisha.toDouble() / budgetPoisha.toDouble()) * 100.0
            } else {
                0.0
            }

            val status = when {
                budgetPoisha == 0L -> BudgetStatus.NO_BUDGET
                usedPercentage >= 100.0 -> BudgetStatus.EXCEEDED_BUDGET
                usedPercentage >= 80.0 -> BudgetStatus.NEAR_BUDGET
                else -> BudgetStatus.UNDER_BUDGET
            }

            // Find highest spending day
            val expensesByDate = expenses.groupBy { it.date }
            val highestDayEntry = expensesByDate.maxByOrNull { entry -> entry.value.sumOf { it.totalPoisha } }
            val highestDay = highestDayEntry?.key
            val highestDayPoisha = highestDayEntry?.value?.sumOf { it.totalPoisha } ?: 0L

            return MonthlySummary(
                year = year,
                month = month,
                budgetPoisha = budgetPoisha,
                totalExpensePoisha = totalExpensePoisha,
                remainingPoisha = remainingPoisha,
                usedPercentage = usedPercentage,
                status = status,
                expenseCount = expenses.size,
                highestSpendingDay = highestDay,
                highestSpendingDayPoisha = highestDayPoisha
            )
        }
    }
}
