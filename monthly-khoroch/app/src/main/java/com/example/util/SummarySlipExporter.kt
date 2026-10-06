package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.Expense
import com.example.data.model.MonthYear
import com.example.data.model.MonthlySummary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates a one-page A4 slip (PNG) summarizing a month: headline numbers
 * followed by an itemized expense list in two columns (left column first,
 * then right column from the top). Fixed line advances guarantee no overlap.
 */
object SummarySlipExporter {

    const val SLIP_WIDTH = 1240   // A4 @ ~150 dpi portrait
    const val SLIP_HEIGHT = 1754

    private const val PADDING = 60f
    private const val COL_GAP = 40f
    private const val NAVY = Color.rgb(0x0A, 0x19, 0x31)
    private const val INK = Color.rgb(0x11, 0x11, 0x11)
    private const val MUTED = Color.rgb(0x55, 0x55, 0x55)
    private const val DIVIDER = Color.rgb(0xD9, 0xD9, 0xD9)
    private const val CARD_BG = Color.rgb(0xF3, 0xF7, 0xFB)

    fun createSlipBitmap(
        monthYear: MonthYear,
        summary: MonthlySummary,
        expenses: List<Expense>,
        currencySymbol: String = "৳"
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(SLIP_WIDTH, SLIP_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        var y = PADDING + 8f
        val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.DEFAULT_BOLD
            color = NAVY
        }
        val regular = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.DEFAULT
            color = INK
        }

        // ---- Header ----
        bold.textSize = 56f
        bold.textAlign = Paint.Align.CENTER
        canvas.drawText("মাসিক খরচ", SLIP_WIDTH / 2f, y, bold)
        y += 66f

        bold.textSize = 34f
        val monthLabel = "${BanglaFormatter.getMonthName(monthYear.month)} " +
            BanglaFormatter.toBanglaDigits(monthYear.year.toString())
        canvas.drawText(monthLabel, SLIP_WIDTH / 2f, y, bold.copy(color = INK))
        y += 40f

        regular.textSize = 26f
        regular.textAlign = Paint.Align.CENTER
        val stamp = "প্রস্তুত: ${BanglaFormatter.toBanglaDigits(SimpleDateFormat("dd.MM.yyyy, hh:mm", Locale.US).format(Date()))}"
        canvas.drawText(stamp, SLIP_WIDTH / 2f, y, regular.copy(color = MUTED))
        y += 50f
        regular.textAlign = Paint.Align.LEFT

        canvas.drawRect(PADDING, y, SLIP_WIDTH - PADDING, y + 4f, regular.copy(color = NAVY))
        y += 30f
        bold.textAlign = Paint.Align.LEFT

        // ---- Summary card ----
        val cardTop = y
        val card = RectF(PADDING, cardTop, SLIP_WIDTH - PADDING, cardTop + 340f)
        canvas.drawRoundRect(card, 20f, 20f, Paint().apply {
            style = Paint.Style.FILL
            color = CARD_BG
        })
        canvas.drawRoundRect(card, 20f, 20f, Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = NAVY
        })

        val labelPaint = regular.copy().apply {
            textSize = 30f
            color = MUTED
        }
        val valuePaint = bold.copy(color = NAVY).apply { textSize = 30f }
        val x = PADDING + 40f
        val xRight = SLIP_WIDTH - PADDING - 40f
        var cy = cardTop + 50f

        fun row(label: String, value: String) {
            canvas.drawText(label, x, cy, labelPaint)
            canvas.drawText(value, xRight, cy, valuePaint.textAlignRight())
            cy += 54f
        }

        row("বাজেট", BanglaFormatter.formatCurrency(summary.budgetPoisha, currencySymbol))
        row("মোট খরচ", BanglaFormatter.formatCurrency(summary.totalExpensePoisha, currencySymbol))
        row(
            if (summary.remainingPoisha < 0) "অবশিষ্ট (অতিরিক্ত)" else "অবশিষ্ট",
            BanglaFormatter.formatCurrency(Math.abs(summary.remainingPoisha), currencySymbol)
        )
        row("ব্যবহার", BanglaFormatter.formatPercentage(summary.usedPercentage))
        row("মোট এন্ট্রি", "${BanglaFormatter.toBanglaDigits(summary.expenseCount.toString())} টি")
        row(
            "সর্বোচ্চ খরচের দিন",
            summary.highestSpendingDay?.let { BanglaFormatter.formatDateBangla(it) } ?: "তথ্য নেই"
        )

        y = cardTop + 380f

        // ---- Items heading ----
        bold.copy(color = NAVY).apply { textSize = 36f }.let { paint ->
            canvas.drawText("খরচের তালিকা", PADDING, y, paint)
        }
        y += 22f
        canvas.drawRect(PADDING, y, SLIP_WIDTH - PADDING, y + 2f, regular.copy(color = DIVIDER))
        y += 44f

        // ---- Itemized list: two columns, left first, then right ----
        val colWidth = (SLIP_WIDTH - PADDING * 2f - COL_GAP) / 2f
        val topY = y + 10f
        var col = 0
        var itemY = topY

        val namePaint = bold.copy(color = INK).apply { textSize = 28f }
        val metaPaint = regular.copy(color = MUTED).apply { textSize = 24f }
        val amountPaint = bold.copy(color = NAVY).apply { textSize = 28f }

        fun colX() = PADDING + col * (colWidth + COL_GAP)

        for (expense in expenses) {
            val nameLines = wrapText(expense.productName, namePaint, colWidth)
            val meta = if (expense.quantity > 1.0) {
                "${BanglaFormatter.formatDateBangla(expense.date)} • পরিমাণ: ${BanglaFormatter.formatQuantity(expense.quantity)}"
            } else {
                BanglaFormatter.formatDateBangla(expense.date)
            }
            val amount = BanglaFormatter.formatCurrency(expense.totalPoisha, currencySymbol)
            val itemHeight = nameLines.size * 36f + 48f

            if (itemY + itemHeight > SLIP_HEIGHT - PADDING) {
                if (col == 0) {
                    col = 1
                    itemY = topY
                } else {
                    break // right column is full -> keep slip to one page
                }
            }
            if (itemY + itemHeight > SLIP_HEIGHT - PADDING) break

            val ix = colX()
            for ((index, line) in nameLines.withIndex()) {
                canvas.drawText(line, ix, itemY, namePaint)
                itemY += 36f
            }
            itemY += 2f
            val rightX = ix + colWidth
            metaPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(meta, rightX, itemY, metaPaint)
            itemY += 34f
            amountPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(amount, rightX, itemY, amountPaint)
            itemY += 12f
        }

        return bitmap
    }

    private fun Paint.textAlignRight(): Paint {
        textAlign = Paint.Align.RIGHT
        return this
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth || current.isEmpty()) {
                current = StringBuilder(candidate)
            } else {
                lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines.ifEmpty { listOf(text) }
    }

    fun fileName(monthYear: MonthYear): String =
        "Monthly_Khoroch_${monthYear.month}_${monthYear.year}.png"

    /**
     * Saves the slip PNG into the system Downloads folder. API 29+; returns a
     * human readable result or null when it fails or on older Android.
     */
    fun saveToDownloads(context: Context, bitmap: Bitmap, monthYear: MonthYear): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            val name = fileName(monthYear)
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                } ?: run {
                    resolver.delete(uri, null, null)
                    return null
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                "Downloads ফোল্ডারে সংরক্ষণ হয়েছে।"
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Falls back for older Android: writes the slip to the app cache and opens
     * the system share sheet so the user can save it anywhere.
     */
    fun shareFallback(context: Context, bitmap: Bitmap, monthYear: MonthYear): String? {
        return try {
            val file = File(context.cacheDir, "updates/${fileName(monthYear)}")
            file.parentFile?.mkdirs()
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "সারসংক্ষেপ সংরক্ষণ করুন"))
            "শেয়ার মেনু খোলা হয়েছে।"
        } catch (e: Exception) {
            null
        }
    }
}