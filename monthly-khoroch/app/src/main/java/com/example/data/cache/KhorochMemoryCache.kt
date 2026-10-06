package com.example.data.cache

import com.example.data.model.Budget
import com.example.data.model.Expense
import com.example.data.model.MonthlySummary
import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight, thread-safe in-memory cache for frequently accessed Monthly Khoroch data.
 *
 * ARCHITECTURE RULES:
 * 1. Room Database remains the single source of truth.
 * 2. Caches only frequently accessed data:
 *    - Current / active month's budget
 *    - Current / active month's total expense & remaining budget
 *    - Current / active month's expense summary
 *    - Recently displayed expense list for the active month
 * 3. Does not cache the entire database or historical archives.
 * 4. Invalidated / refreshed immediately on any expense or budget mutation.
 * 5. Volatile in-memory: safe across cold restarts (falls back seamlessly to Room).
 */
class KhorochMemoryCache {

    private val summaryCache = ConcurrentHashMap<String, MonthlySummary>()
    private val budgetCache = ConcurrentHashMap<String, Budget>()
    private val recentExpensesCache = ConcurrentHashMap<String, List<Expense>>()

    private fun key(year: Int, month: Int): String = "$year-$month"

    fun getMonthlySummary(year: Int, month: Int): MonthlySummary? {
        return summaryCache[key(year, month)]
    }

    fun putMonthlySummary(year: Int, month: Int, summary: MonthlySummary) {
        summaryCache[key(year, month)] = summary
    }

    fun getBudget(year: Int, month: Int): Budget? {
        return budgetCache[key(year, month)]
    }

    fun putBudget(year: Int, month: Int, budget: Budget) {
        budgetCache[key(year, month)] = budget
    }

    fun getRecentExpenses(year: Int, month: Int): List<Expense>? {
        return recentExpensesCache[key(year, month)]
    }

    fun putRecentExpenses(year: Int, month: Int, expenses: List<Expense>) {
        recentExpensesCache[key(year, month)] = expenses
    }

    /**
     * Invalidate all cached data for the specified month so subsequent reads / flows
     * query directly from Room.
     */
    fun invalidateMonth(year: Int, month: Int) {
        val k = key(year, month)
        summaryCache.remove(k)
        budgetCache.remove(k)
        recentExpensesCache.remove(k)
    }

    /**
     * Clears all in-memory caches (e.g. on database restore or data clear).
     */
    fun clear() {
        summaryCache.clear()
        budgetCache.clear()
        recentExpensesCache.clear()
    }
}
