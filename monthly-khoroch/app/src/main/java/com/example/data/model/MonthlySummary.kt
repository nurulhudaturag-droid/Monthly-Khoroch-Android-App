package com.example.data.model

enum class BudgetStatus {
    UNDER_BUDGET,     // 0% - 79.99% -> Green
    NEAR_BUDGET,      // 80% - 99.99% -> Amber/Orange
    EXCEEDED_BUDGET,  // 100%+ -> Red
    NO_BUDGET         // Budget = 0
}

data class MonthYear(
    val year: Int,
    val month: Int // 1 to 12
) : Comparable<MonthYear> {
    override fun compareTo(other: MonthYear): Int {
        val yDiff = this.year.compareTo(other.year)
        return if (yDiff != 0) yDiff else this.month.compareTo(other.month)
    }

    fun previous(): MonthYear {
        return if (month == 1) MonthYear(year - 1, 12) else MonthYear(year, month - 1)
    }

    fun next(): MonthYear {
        return if (month == 12) MonthYear(year + 1, 1) else MonthYear(year, month + 1)
    }
}

data class DailyExpenseGroup(
    val date: String,
    val totalPoisha: Long,
    val expenses: List<Expense>
)

data class MonthlySummary(
    val year: Int,
    val month: Int,
    val budgetPoisha: Long,
    val totalExpensePoisha: Long,
    val remainingPoisha: Long, // budgetPoisha - totalExpensePoisha
    val usedPercentage: Double,
    val status: BudgetStatus,
    val expenseCount: Int,
    val highestSpendingDay: String? = null,
    val highestSpendingDayPoisha: Long = 0L
)
