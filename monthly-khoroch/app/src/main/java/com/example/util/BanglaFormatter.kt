package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale

object BanglaFormatter {

    private val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    private val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

    val banglaMonths = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    fun toBanglaDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(banglaDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toEnglishDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '০'..'৯') {
                sb.append(englishDigits[ch - '০'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun getMonthName(month: Int): String {
        return if (month in 1..12) banglaMonths[month - 1] else "অজানা মাস"
    }

    /**
     * Converts Taka (Double) to Poisha (Long) deterministically (1 Taka = 100 Poisha)
     */
    fun takaToPoisha(taka: Double): Long {
        return Math.round(taka * 100.0)
    }

    /**
     * Converts Poisha (Long) to Taka (Double)
     */
    fun poishaToTaka(poisha: Long): Double {
        return poisha / 100.0
    }

    /**
     * Calculates total Poisha from quantity (Double) and unitPricePoisha (Long)
     */
    fun calculateTotalPoisha(quantity: Double, unitPricePoisha: Long): Long {
        if (quantity <= 0.0 || unitPricePoisha <= 0L) return 0L
        return Math.round(quantity * unitPricePoisha)
    }

    /**
     * Formats poisha into currency string: e.g. ৳১৮,৪৫০ or ৳18,450
     */
    fun formatCurrency(
        poisha: Long,
        currencySymbol: String = "৳",
        useBanglaDigits: Boolean = true
    ): String {
        val isNegative = poisha < 0
        val absPoisha = Math.abs(poisha)
        val taka = absPoisha / 100.0

        val symbols = DecimalFormatSymbols(Locale.US)
        val formatter = if (absPoisha % 100L == 0L) {
            DecimalFormat("#,##,##0", symbols) // Indian/Bangladeshi comma style
        } else {
            DecimalFormat("#,##,##0.00", symbols)
        }

        val formattedNumber = formatter.format(taka)
        val sign = if (isNegative) "-" else ""
        val result = "$currencySymbol$sign$formattedNumber"

        return if (useBanglaDigits) toBanglaDigits(result) else result
    }

    /**
     * Formats date string "yyyy-MM-dd" into "৩ অক্টোবর ২০২৬"
     */
    fun formatDateBangla(dateStr: String): String {
        try {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                val year = parts[0].toIntOrNull() ?: 2026
                val month = parts[1].toIntOrNull() ?: 1
                val day = parts[2].toIntOrNull() ?: 1
                val monthName = getMonthName(month)
                return toBanglaDigits("$day $monthName $year")
            }
        } catch (_: Exception) {}
        return dateStr
    }

    /**
     * Formats a timestamp into "৩ অক্টোবর ২০২৬, ৩:০৫ PM" (Bangla month, Bangla digits)
     */
    fun formatDateTimeBangla(timeMillis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthName = getMonthName(cal.get(Calendar.MONTH) + 1)
        val year = cal.get(Calendar.YEAR)
        val hourOfDay = cal.get(Calendar.HOUR_OF_DAY)
        val hour12 = when (hourOfDay) {
            0 -> 12
            in 1..12 -> hourOfDay
            else -> hourOfDay - 12
        }
        val minute = String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE))
        val period = if (hourOfDay < 12) "AM" else "PM"
        return toBanglaDigits("$day $monthName $year, $hour12:$minute $period")
    }

    /**
     * Formats percentage: e.g. 61.5%
     */
    fun formatPercentage(percentage: Double, useBanglaDigits: Boolean = true): String {
        val formatted = String.format(Locale.US, "%.1f%%", percentage)
        return if (useBanglaDigits) toBanglaDigits(formatted) else formatted
    }
}
