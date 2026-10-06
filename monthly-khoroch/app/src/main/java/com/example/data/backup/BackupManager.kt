package com.example.data.backup

import com.example.data.model.Budget
import com.example.data.model.Expense
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val budgetCount: Int = 0,
    val expenseCount: Int = 0,
    val exportedAt: String? = null,
    val budgets: List<Budget> = emptyList(),
    val expenses: List<Expense> = emptyList()
)

object BackupManager {

    private const val APP_IDENTIFIER = "Monthly Khoroch"
    private const val CURRENT_BACKUP_VERSION = 1

    fun generateBackupFileName(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = dateFormat.format(Date())
        return "MonthlyKhoroch_Backup_$today.json"
    }

    /**
     * Serializes all budgets, expenses, and settings into JSON.
     */
    fun createBackupJson(
        budgets: List<Budget>,
        expenses: List<Expense>,
        currencySymbol: String = "৳"
    ): String {
        val root = JSONObject()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

        root.put("app", APP_IDENTIFIER)
        root.put("backupVersion", CURRENT_BACKUP_VERSION)
        root.put("exportedAt", isoFormat.format(Date()))

        // Budgets array
        val budgetsArray = JSONArray()
        for (b in budgets) {
            val bObj = JSONObject()
            bObj.put("year", b.year)
            bObj.put("month", b.month)
            bObj.put("budgetAmountPoisha", b.budgetAmountPoisha)
            bObj.put("createdAt", b.createdAt)
            bObj.put("updatedAt", b.updatedAt)
            budgetsArray.put(bObj)
        }
        root.put("budgets", budgetsArray)

        // Expenses array
        val expensesArray = JSONArray()
        for (e in expenses) {
            val eObj = JSONObject()
            eObj.put("date", e.date)
            eObj.put("year", e.year)
            eObj.put("month", e.month)
            eObj.put("day", e.day)
            eObj.put("productName", e.productName)
            eObj.put("quantity", e.quantity)
            eObj.put("totalPoisha", e.totalPoisha)
            eObj.put("createdAt", e.createdAt)
            eObj.put("updatedAt", e.updatedAt)
            expensesArray.put(eObj)
        }
        root.put("expenses", expensesArray)

        // Settings object
        val settingsObj = JSONObject()
        settingsObj.put("currencySymbol", currencySymbol)
        root.put("settings", settingsObj)

        return root.toString(2)
    }

    /**
     * Validates and parses a JSON backup file content.
     */
    fun validateAndParseBackup(jsonContent: String): BackupValidationResult {
        if (jsonContent.isBlank()) {
            return BackupValidationResult(
                isValid = false,
                errorMessage = "ব্যাকআপ ফাইলটি খালি।"
            )
        }

        try {
            val root = JSONObject(jsonContent)

            // Validate app signature
            val app = root.optString("app")
            if (app != APP_IDENTIFIER) {
                return BackupValidationResult(
                    isValid = false,
                    errorMessage = "এই ফাইলটি মাসিক খরচ (Monthly Khoroch)-এর ব্যাকআপ ফাইল নয়।"
                )
            }

            val backupVersion = root.optInt("backupVersion", -1)
            if (backupVersion <= 0) {
                return BackupValidationResult(
                    isValid = false,
                    errorMessage = "ব্যাকআপ ফাইলের সংস্করণটি সঠিক নয়।"
                )
            }

            val exportedAt = root.optString("exportedAt", "")

            // Parse budgets
            val budgetsList = mutableListOf<Budget>()
            val budgetsArray = root.optJSONArray("budgets") ?: JSONArray()
            for (i in 0 until budgetsArray.length()) {
                val bObj = budgetsArray.getJSONObject(i)
                val year = bObj.getInt("year")
                val month = bObj.getInt("month")
                val amount = bObj.getLong("budgetAmountPoisha")
                if (month in 1..12 && year in 2000..2100 && amount >= 0) {
                    budgetsList.add(
                        Budget(
                            year = year,
                            month = month,
                            budgetAmountPoisha = amount,
                            createdAt = bObj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = bObj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Parse expenses
            val expensesList = mutableListOf<Expense>()
            val expensesArray = root.optJSONArray("expenses") ?: JSONArray()
            for (i in 0 until expensesArray.length()) {
                val eObj = expensesArray.getJSONObject(i)
                val date = eObj.getString("date")
                val year = eObj.getInt("year")
                val month = eObj.getInt("month")
                val day = eObj.getInt("day")
                val productName = eObj.getString("productName")
                val quantity = eObj.optDouble("quantity", 1.0)
                val unit = eObj.optString("unit", "")
                val unitPricePoisha = eObj.optLong("unitPricePoisha", 0L)
                val totalPoisha = if (eObj.has("totalPoisha")) {
                    eObj.getLong("totalPoisha")
                } else {
                    (quantity * unitPricePoisha).toLong()
                }

                if (productName.isNotBlank() && totalPoisha >= 0) {
                    expensesList.add(
                        Expense(
                            date = date,
                            year = year,
                            month = month,
                            day = day,
                            productName = productName,
                            quantity = quantity,
                            unit = unit,
                            unitPricePoisha = unitPricePoisha,
                            totalPoisha = totalPoisha,
                            createdAt = eObj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = eObj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            return BackupValidationResult(
                isValid = true,
                budgetCount = budgetsList.size,
                expenseCount = expensesList.size,
                exportedAt = exportedAt,
                budgets = budgetsList,
                expenses = expensesList
            )

        } catch (e: Exception) {
            return BackupValidationResult(
                isValid = false,
                errorMessage = "এই Backup ফাইলটি সঠিক নয় বা ক্ষতিগ্রস্ত: ${e.localizedMessage}"
            )
        }
    }
}
