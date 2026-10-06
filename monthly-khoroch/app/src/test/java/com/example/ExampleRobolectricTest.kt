package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.model.Budget
import com.example.data.model.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("মাসিক খরচ", appName)
    }

    @Test
    fun testBackupExportAndValidation() {
        val budgets = listOf(Budget(year = 2026, month = 10, budgetAmountPoisha = 3000000L))
        val expenses = listOf(
            Expense(
                date = "2026-10-03",
                year = 2026,
                month = 10,
                day = 3,
                productName = "চাল",
                quantity = 5.0,
                unit = "কেজি",
                unitPricePoisha = 7000L,
                totalPoisha = 35000L
            )
        )

        val json = BackupManager.createBackupJson(budgets, expenses, "৳")
        val validation = BackupManager.validateAndParseBackup(json)

        assertTrue(validation.isValid)
        assertEquals(1, validation.budgetCount)
        assertEquals(1, validation.expenseCount)
        assertEquals("চাল", validation.expenses[0].productName)
        assertEquals(35000L, validation.expenses[0].totalPoisha)

        // Invalid JSON validation test
        val corruptValidation = BackupManager.validateAndParseBackup("{ invalid json content }")
        assertFalse(corruptValidation.isValid)

        // Non-matching app validation test
        val fakeAppValidation = BackupManager.validateAndParseBackup("{\"app\":\"OtherApp\",\"backupVersion\":1}")
        assertFalse(fakeAppValidation.isValid)
    }
}
