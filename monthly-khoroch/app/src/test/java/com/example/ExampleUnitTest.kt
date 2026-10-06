package com.example

import com.example.data.model.Budget
import com.example.data.model.BudgetStatus
import com.example.data.model.Expense
import com.example.data.repository.KhorochRepository
import com.example.util.BanglaFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testActualTotalAmountExpenseEntry() {
        // User enters actual total amount paid (e.g. 500 Taka = 50,000 poisha)
        val expense = Expense(
            date = "2026-10-03",
            year = 2026,
            month = 10,
            day = 3,
            productName = "বাজার খরচ",
            totalPoisha = BanglaFormatter.takaToPoisha(500.0)
        )

        assertEquals("বাজার খরচ", expense.productName)
        assertEquals(50000L, expense.totalPoisha)
        assertEquals(500.0, BanglaFormatter.poishaToTaka(expense.totalPoisha), 0.001)
        assertEquals("", expense.quantity)
        assertEquals("", expense.unit)
        assertEquals(0L, expense.unitPricePoisha)
    }

    @Test
    fun testQuantityTimesUnitPriceCalculation() {
        // Quantity = 5, Unit price = 70.00 BDT (7000 poisha) -> 350.00 BDT (35000 poisha)
        val unitPricePoisha = BanglaFormatter.takaToPoisha(70.0)
        assertEquals(7000L, unitPricePoisha)

        val totalPoisha = BanglaFormatter.calculateTotalPoisha(5.0, unitPricePoisha)
        assertEquals(35000L, totalPoisha)
        assertEquals(350.0, BanglaFormatter.poishaToTaka(totalPoisha), 0.001)

        // Fractional quantity: 2.5 kg * 120.00 BDT -> 300.00 BDT
        val price120 = BanglaFormatter.takaToPoisha(120.0)
        val totalFractional = BanglaFormatter.calculateTotalPoisha(2.5, price120)
        assertEquals(30000L, totalFractional)
        assertEquals(300.0, BanglaFormatter.poishaToTaka(totalFractional), 0.001)
    }

    @Test
    fun testMonthlyCalculationsAndStatuses() {
        // Budget = 30,000 BDT (3,000,000 poisha)
        val budget = Budget(
            year = 2026,
            month = 10,
            budgetAmountPoisha = BanglaFormatter.takaToPoisha(30000.0)
        )

        val expenses = listOf(
            Expense(
                date = "2026-10-01",
                year = 2026,
                month = 10,
                day = 1,
                productName = "চাল",
                quantity = "১০ কেজি",
                unit = "কেজি",
                unitPricePoisha = 7000L,
                totalPoisha = 70000L // 700 BDT
            ),
            Expense(
                date = "2026-10-02",
                year = 2026,
                month = 10,
                day = 2,
                productName = "বাস ভাড়া",
                quantity = "১ পিস",
                unit = "পিস",
                unitPricePoisha = 30000L,
                totalPoisha = 30000L // 300 BDT
            )
        )

        val summary = KhorochRepository.calculateSummary(2026, 10, budget, expenses)

        // Total expense = 700 + 300 = 1000 BDT (100,000 poisha)
        assertEquals(100000L, summary.totalExpensePoisha)
        // Remaining = 30,000 - 1,000 = 29,000 BDT (2,900,000 poisha)
        assertEquals(2900000L, summary.remainingPoisha)
        // Percentage = (1,000 / 30,000) * 100 = 3.33%
        assertEquals(3.333, summary.usedPercentage, 0.01)
        assertEquals(BudgetStatus.UNDER_BUDGET, summary.status)
        assertEquals(2, summary.expenseCount)
        assertEquals("2026-10-01", summary.highestSpendingDay)
    }

    @Test
    fun testZeroBudgetProtection() {
        // Budget = 0 -> Avoid division by zero
        val expenses = listOf(
            Expense(
                date = "2026-10-01",
                year = 2026,
                month = 10,
                day = 1,
                productName = "ডিম",
                quantity = "১২ পিস",
                unit = "পিস",
                unitPricePoisha = 1200L,
                totalPoisha = 14400L
            )
        )

        val summary = KhorochRepository.calculateSummary(2026, 10, null, expenses)
        assertEquals(0.0, summary.usedPercentage, 0.001)
        assertEquals(BudgetStatus.NO_BUDGET, summary.status)
        assertEquals(-14400L, summary.remainingPoisha)
    }

    @Test
    fun testExceededBudgetStatus() {
        // Budget = 10,000 BDT, Expense = 12,000 BDT
        val budget = Budget(year = 2026, month = 10, budgetAmountPoisha = 1000000L)
        val expenses = listOf(
            Expense(
                date = "2026-10-05",
                year = 2026,
                month = 10,
                day = 5,
                productName = "ল্যাপটপ মেরামত",
                quantity = "১ সার্ভিস",
                unit = "সার্ভিস",
                unitPricePoisha = 1200000L,
                totalPoisha = 1200000L
            )
        )

        val summary = KhorochRepository.calculateSummary(2026, 10, budget, expenses)
        assertEquals(120.0, summary.usedPercentage, 0.01)
        assertEquals(BudgetStatus.EXCEEDED_BUDGET, summary.status)
        assertEquals(-200000L, summary.remainingPoisha)
    }

    @Test
    fun testBanglaFormatting() {
        assertEquals("১২৩৪৫", BanglaFormatter.toBanglaDigits("12345"))
        assertEquals("12345", BanglaFormatter.toEnglishDigits("১২৩৪৫"))
        assertEquals("অক্টোবর", BanglaFormatter.getMonthName(10))
        assertEquals("৩ অক্টোবর ২০২৬", BanglaFormatter.formatDateBangla("2026-10-03"))
    }

    @Test
    fun testMemoryCacheStorageAndInvalidation() {
        val cache = com.example.data.cache.KhorochMemoryCache()
        val summary = com.example.data.model.MonthlySummary(
            year = 2026,
            month = 10,
            budgetPoisha = 2000000L, // ৳20,000
            totalExpensePoisha = 850000L, // ৳8,500
            remainingPoisha = 1150000L, // ৳11,500
            usedPercentage = 42.5,
            status = BudgetStatus.UNDER_BUDGET,
            expenseCount = 5
        )

        // Put into cache
        cache.putMonthlySummary(2026, 10, summary)
        assertEquals(summary, cache.getMonthlySummary(2026, 10))

        // Invalidate month
        cache.invalidateMonth(2026, 10)
        org.junit.Assert.assertNull(cache.getMonthlySummary(2026, 10))
    }

    @Test
    fun testFinancialCalculationCorrectnessOnExpenseAddition() {
        // Budget = ৳20,000 (2,000,000 poisha)
        val budget = Budget(year = 2026, month = 10, budgetAmountPoisha = 2000000L)
        val initialExpenses = listOf(
            Expense(date = "2026-10-01", year = 2026, month = 10, day = 1, productName = "বাজার", totalPoisha = 850000L)
        )
        val initialSummary = KhorochRepository.calculateSummary(2026, 10, budget, initialExpenses)
        assertEquals(850000L, initialSummary.totalExpensePoisha) // ৳8,500
        assertEquals(1150000L, initialSummary.remainingPoisha) // ৳11,500

        // User adds a new expense of ৳500 (50,000 poisha)
        val newExpense = Expense(date = "2026-10-02", year = 2026, month = 10, day = 2, productName = "নাস্তা", totalPoisha = 50000L)
        val updatedExpenses = initialExpenses + newExpense
        val updatedSummary = KhorochRepository.calculateSummary(2026, 10, budget, updatedExpenses)

        // Updated totals:
        // Current expenses = ৳9,000 (900,000 poisha)
        assertEquals(900000L, updatedSummary.totalExpensePoisha)
        // Remaining budget = ৳11,000 (1,100,000 poisha)
        assertEquals(1100000L, updatedSummary.remainingPoisha)
        assertEquals(45.0, updatedSummary.usedPercentage, 0.01)
    }

    @Test
    fun testHighVolumeBatchSummaryPerformance() {
        val budget = Budget(year = 2026, month = 10, budgetAmountPoisha = 5000000L) // ৳50,000
        val largeExpenseList = (1..500).map { i ->
            Expense(
                id = i.toLong(),
                date = "2026-10-${(i % 28 + 1).toString().padStart(2, '0')}",
                year = 2026,
                month = 10,
                day = i % 28 + 1,
                productName = "পণ্য $i",
                quantity = (i % 5 + 1).toString(),
                totalPoisha = 10000L // ৳100 each
            )
        }

        val startTime = System.currentTimeMillis()
        val summary = KhorochRepository.calculateSummary(2026, 10, budget, largeExpenseList)
        val durationMs = System.currentTimeMillis() - startTime

        // 500 items * ৳100 = ৳50,000 (5,000,000 poisha)
        assertEquals(5000000L, summary.totalExpensePoisha)
        assertEquals(0L, summary.remainingPoisha)
        assertEquals(100.0, summary.usedPercentage, 0.001)
        assertEquals(BudgetStatus.EXCEEDED_BUDGET, summary.status)
        assertTrue("Batch calculation for 500 items must be ultra-fast (took ${durationMs}ms)", durationMs < 50)
    }

    @Test
    fun testFastDigitConversionPerformance() {
        val startTime = System.currentTimeMillis()
        for (i in 0..10000) {
            val bangla = BanglaFormatter.toBanglaDigits("1234567890")
            assertEquals("১২৩৪৫৬৭৮৯০", bangla)
            val english = BanglaFormatter.toEnglishDigits(bangla)
            assertEquals("1234567890", english)
        }
        val durationMs = System.currentTimeMillis() - startTime
        assertTrue("10,000 O(1) string conversions must execute under 200ms (took ${durationMs}ms)", durationMs < 200)
    }

    @Test
    fun testInformationalQuantityDoesNotMultiplyFinancialCalculations() {
        // Product: আলু, Quantity: "১ কেজি", Amount: ৳100 (10,000 poisha)
        val expense = Expense(
            date = "2026-10-04",
            year = 2026,
            month = 10,
            day = 4,
            productName = "আলু",
            quantity = "১ কেজি",
            totalPoisha = 10000L
        )

        val budget = Budget(year = 2026, month = 10, budgetAmountPoisha = 50000L) // ৳500
        val summary = KhorochRepository.calculateSummary(2026, 10, budget, listOf(expense))

        // Total expense MUST be ৳100 (10,000 poisha), NEVER 2.5 * 100 = ৳250
        assertEquals(10000L, summary.totalExpensePoisha)
        assertEquals(40000L, summary.remainingPoisha)
        assertEquals(20.0, summary.usedPercentage, 0.001)
    }
}
