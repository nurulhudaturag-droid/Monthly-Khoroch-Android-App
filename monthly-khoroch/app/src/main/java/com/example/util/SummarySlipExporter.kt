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

/**
 * Generates A4 slip PNGs (one per page) summarizing a month: a compact
 * headline block followed by an itemized expense list laid out as a
 * 3-column table (item, quantity, amount). Expenses are grouped by date with
 * a date band above each group. Each page uses two body columns holding up to
 * [MAX_PER_COLUMN] rows apiece (60 per page); when a month has more expenses a
 * second (or further) page is produced. Fixed line advances guarantee no overlap.
 */
object SummarySlipExporter {

    const val SLIP_WIDTH = 1240   // A4 @ ~150 dpi portrait
    const val SLIP_HEIGHT = 1754

    private const val MAX_PER_COLUMN = 30

    private const val PADDING = 50f
    private const val COL_GAP = 34f
    private const val COL_WIDTH = (SLIP_WIDTH - PADDING * 2f - COL_GAP) / 2f
    private const val ITEM_WIDTH = COL_WIDTH - 110f - 150f
    private const val QTY_RIGHT = PADDING + ITEM_WIDTH + 110f
    private const val ROW_RIGHT = PADDING + COL_WIDTH
    private const val COL_HEADER_HEIGHT = 32f
    private const val DATE_BAND_HEIGHT = 36f
    private const val NAME_LINE_STEP = 23f
    private const val ROW_PAD = 8f

    private val NAVY = Color.rgb(0x0A, 0x19, 0x31)
    private val INK = Color.rgb(0x11, 0x11, 0x11)
    private val MUTED = Color.rgb(0x55, 0x55, 0x55)
    private val DIVIDER = Color.rgb(0xD9, 0xD9, 0xD9)
    private val CARD_BG = Color.rgb(0xF3, 0xF7, 0xFB)

    private fun titlePaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = 40f
        color = NAVY
        textAlign = Paint.Align.CENTER
    }

    private fun monthPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = 24f
        color = INK
        textAlign = Paint.Align.CENTER
    }

    private fun stampPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT
        textSize = 18f
        color = MUTED
        textAlign = Paint.Align.CENTER
    }

    private fun labelPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT
        textSize = 22f
        color = MUTED
    }

    private fun valuePaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = 22f
        color = NAVY
    }

    private fun headingPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = 24f
        color = NAVY
    }

    private fun colHeaderPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = 19f
        color = MUTED
    }

    private fun datePaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = 21f
        color = NAVY
    }

    private fun namePaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = 19f
        color = INK
    }

    private fun qtyPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT
        textSize = 19f
        color = MUTED
    }

    private fun amountPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = 19f
        color = NAVY
    }

    private fun footerPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = 22f
        color = NAVY
        textAlign = Paint.Align.CENTER
    }

    private fun bodyTopY(): Float =
        PADDING + 6f + 48f + 32f + 36f + 22f + summaryCardHeight() + 22f + 28f + 20f

    private fun summaryCardHeight(): Float = 28f + 6f * 32f + 14f

    private fun bodyBottomY(): Float = SLIP_HEIGHT - PADDING - 22f

    private fun rowHeight(expense: Expense): Float {
        val lines = wrapText(expense.productName, namePaint(), ITEM_WIDTH).size
        return lines * NAME_LINE_STEP + ROW_PAD
    }

    /** Splits expenses into pages: each page holds up to 2 columns, each
     *  column up to [MAX_PER_COLUMN] rows (bounded also by vertical space). */
    private fun partitionExpenses(
        expenses: List<Expense>,
        available: Float
    ): List<List<List<Expense>>> {
        val pages = mutableListOf<List<List<Expense>>>()

        fun newPage() = mutableListOf<MutableList<Expense>>().apply { add(mutableListOf()) }
        var page = newPage()
        var lastDate: String? = null
        var used = 0f

        for (expense in expenses) {
            var placed = false
            var guard = 0
            while (!placed && guard < 6) {
                guard++
                val dateBand = if (expense.date != lastDate) DATE_BAND_HEIGHT else 0f
                val colHeader = if (page.last().isEmpty()) COL_HEADER_HEIGHT else 0f
                val need = colHeader + dateBand + rowHeight(expense)
                val fitsSpace = used + need <= available

                if (page.last().size < MAX_PER_COLUMN && fitsSpace) {
                    page.last().add(expense)
                    used += need
                    lastDate = expense.date
                    placed = true
                } else if (page.size < 2) {
                    page.add(mutableListOf())
                    used = 0f
                    lastDate = null
                } else if (page.isNotEmpty() && page.flatten().isNotEmpty()) {
                    pages.add(page)
                    page = newPage()
                    used = 0f
                    lastDate = null
                } else {
                    // A single expense taller than a fresh column: force it in.
                    page.last().add(expense)
                    used = need
                    lastDate = expense.date
                    placed = true
                }
            }
        }
        if (page.flatten().isNotEmpty()) pages.add(page)
        return pages
    }

    fun createSlipBitmaps(
        monthYear: MonthYear,
        summary: MonthlySummary,
        expenses: List<Expense>,
        currencySymbol: String = "৳"
    ): List<Bitmap> {
        val available = bodyBottomY() - bodyTopY()
        val pages = partitionExpenses(expenses, available).ifEmpty { listOf(emptyList()) }
        val totalPages = pages.size
        return pages.mapIndexed { index, page ->
            renderPage(page, index + 1, totalPages, monthYear, summary, currencySymbol)
        }
    }

    private fun renderPage(
        page: List<List<Expense>>,
        pageNo: Int,
        totalPages: Int,
        monthYear: MonthYear,
        summary: MonthlySummary,
        currencySymbol: String
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(SLIP_WIDTH, SLIP_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        var y = PADDING + 6f

        // ---- Header ----
        canvas.drawText("মাসিক খরচ", SLIP_WIDTH / 2f, y, titlePaint())
        y += 48f

        val monthLabel = "${BanglaFormatter.getMonthName(monthYear.month)} " +
            BanglaFormatter.toBanglaDigits(monthYear.year.toString())
        canvas.drawText(monthLabel, SLIP_WIDTH / 2f, y, monthPaint())
        y += 32f

        val stamp = "প্রস্তুত: ${BanglaFormatter.formatDateTimeBangla(System.currentTimeMillis())}"
        canvas.drawText(stamp, SLIP_WIDTH / 2f, y, stampPaint())
        y += 36f

        canvas.drawRect(PADDING, y, SLIP_WIDTH - PADDING, y + 4f, Paint(stampPaint()).apply {
            textAlign = Paint.Align.LEFT
            color = NAVY
        })
        y += 22f

        // ---- Summary card ----
        val card = RectF(PADDING, y, SLIP_WIDTH - PADDING, y + summaryCardHeight())
        canvas.drawRoundRect(card, 20f, 20f, Paint().apply {
            style = Paint.Style.FILL
            color = CARD_BG
        })
        canvas.drawRoundRect(card, 20f, 20f, Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = NAVY
        })
        val label = labelPaint()
        val value = valuePaint().apply { textAlign = Paint.Align.RIGHT }
        var cy = card.top + 28f
        fun row(labelText: String, valueText: String) {
            canvas.drawText(labelText, PADDING + 30f, cy, label)
            canvas.drawText(valueText, SLIP_WIDTH - PADDING - 30f, cy, value)
            cy += 32f
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
        y = card.bottom + 22f

        // ---- Items heading ----
        canvas.drawText("খরচের তালিকা", PADDING, y, headingPaint())
        y += 28f
        canvas.drawRect(PADDING, y, SLIP_WIDTH - PADDING, y + 2f, Paint(stampPaint()).apply {
            textAlign = Paint.Align.LEFT
            color = DIVIDER
        })
        y += 20f

        // ---- Table header (one row per body column) ----
        val title = colHeaderPaint()
        val bodyTopY = y
        for (col in page.indices) {
            val ox = col * (COL_WIDTH + COL_GAP)
            canvas.drawText("পণ্য", PADDING + ox, bodyTopY, alignLeft(title))
            canvas.drawText("পরিমাণ", QTY_RIGHT + ox, bodyTopY, alignRight(title))
            canvas.drawText("দাম", ROW_RIGHT + ox, bodyTopY, alignRight(title))
        }

        // ---- Item rows grouped by date ----
        val name = namePaint()
        val qty = qtyPaint()
        val amount = amountPaint().apply { textAlign = Paint.Align.RIGHT }
        val date = datePaint()
        val divider = Paint(stampPaint()).apply {
            textAlign = Paint.Align.LEFT
            color = DIVIDER
        }

        for (col in page.indices) {
            if (page[col].isEmpty()) continue
            val ix = PADDING + col * (COL_WIDTH + COL_GAP)
            var rowY = bodyTopY + COL_HEADER_HEIGHT
            var lastDate: String? = null
            for (expense in page[col]) {
                if (expense.date != lastDate) {
                    canvas.drawText(
                        BanglaFormatter.formatDateBangla(expense.date), ix, rowY, date
                    )
                    canvas.drawRect(ix, rowY + 6f, ix + COL_WIDTH, rowY + 8f, divider)
                    rowY += DATE_BAND_HEIGHT
                    lastDate = expense.date
                }
                val lines = wrapText(expense.productName, name, ITEM_WIDTH)
                for (line in lines) {
                    canvas.drawText(line, ix, rowY, name)
                    rowY += NAME_LINE_STEP
                }
                val baseline = rowY - NAME_LINE_STEP
                val ox = col * (COL_WIDTH + COL_GAP)
                if (expense.quantity.isNotBlank()) {
                    canvas.drawText(expense.quantity, QTY_RIGHT + ox, baseline, alignRight(qty))
                }
                canvas.drawText(
                    BanglaFormatter.formatCurrency(expense.totalPoisha, currencySymbol),
                    ROW_RIGHT + ox,
                    baseline,
                    amount
                )
                rowY += ROW_PAD
            }
        }

        // ---- Page footer ----
        footerPaint().let { paint ->
            val text = "পৃষ্ঠা ${BanglaFormatter.toBanglaDigits(pageNo.toString())} / " +
                BanglaFormatter.toBanglaDigits(totalPages.toString())
            canvas.drawText(text, SLIP_WIDTH / 2f, SLIP_HEIGHT - 24f, paint)
        }

        return bitmap
    }

    private fun alignRight(paint: Paint): Paint {
        paint.textAlign = Paint.Align.RIGHT
        return paint
    }

    private fun alignLeft(paint: Paint): Paint {
        paint.textAlign = Paint.Align.LEFT
        return paint
    }

    /** Word wraps; single words wider than [maxWidth] are broken by characters. */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in text.trim().split(Regex("\\s+"))) {
            if (word.isEmpty()) continue
            if (paint.measureText(word) > maxWidth) {
                if (current.isNotEmpty()) {
                    lines.add(current.toString())
                    current = StringBuilder()
                }
                val sb = StringBuilder()
                for (ch in word) {
                    if (sb.isNotEmpty() && paint.measureText(sb.toString() + ch) > maxWidth) {
                        lines.add(sb.toString())
                        sb.clear()
                    }
                    sb.append(ch)
                }
                if (sb.isNotEmpty()) lines.add(sb.toString())
                continue
            }
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines.ifEmpty { listOf(text.trim()) }
    }

    fun fileName(monthYear: MonthYear, pageIndex: Int = 1, totalPages: Int = 1): String =
        if (totalPages > 1) {
            "Monthly_Khoroch_${monthYear.month}_${monthYear.year}_page${pageIndex}.png"
        } else {
            "Monthly_Khoroch_${monthYear.month}_${monthYear.year}.png"
        }

    /**
     * Saves all slip pages into the system Downloads folder. API 29+; returns a
     * human readable result or null when it fails or on older Android.
     */
    fun saveToDownloads(context: Context, bitmaps: List<Bitmap>, monthYear: MonthYear): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            bitmaps.forEachIndexed { index, bitmap ->
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName(monthYear, index + 1, bitmaps.size))
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
                } catch (e: Exception) {
                    resolver.delete(uri, null, null)
                    return null
                }
            }
            if (bitmaps.size > 1) {
                "Downloads ফোল্ডারে ${BanglaFormatter.toBanglaDigits(bitmaps.size.toString())} টি পৃষ্ঠা সংরক্ষণ হয়েছে।"
            } else {
                "Downloads ফোল্ডারে সংরক্ষণ হয়েছে।"
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Falls back for older Android: writes every page to the app cache and opens
     * the system share sheet (multi-image) so the user can save them anywhere.
     */
    fun shareFallback(context: Context, bitmaps: List<Bitmap>, monthYear: MonthYear): String? {
        return try {
            val uris = mutableListOf<Uri>()
            bitmaps.forEachIndexed { index, bitmap ->
                val file = File(context.cacheDir, "updates/${fileName(monthYear, index + 1, bitmaps.size)}")
                file.parentFile?.mkdirs()
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
            }
            val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/png"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, java.util.ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "সারসংক্ষেপ সংরক্ষণ করুন"))
            "শেয়ার মেনু খোলা হয়েছে।"
        } catch (e: Exception) {
            null
        }
    }
}