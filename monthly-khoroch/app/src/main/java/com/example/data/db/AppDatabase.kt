package com.example.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Budget
import com.example.data.model.Expense

@Database(
    entities = [Budget::class, Expense::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun budgetDao(): BudgetDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * v2 -> v3: [Expense.quantity] changed from REAL (Double) to TEXT so
         * users can type free-text quantities like "1 KG" or "২pcs". Room
         * cannot alter a column type, so we rebuild the table while preserving
         * every row. Stored numeric values are kept as text; the old default
         * 1.0 (meant "no quantity") becomes blank.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `expenses_new` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`date` TEXT NOT NULL, `year` INTEGER NOT NULL, `month` INTEGER NOT NULL, " +
                        "`day` INTEGER NOT NULL, `productName` TEXT NOT NULL, `quantity` TEXT NOT NULL, " +
                        "`unit` TEXT NOT NULL, `unitPricePoisha` INTEGER NOT NULL, " +
                        "`totalPoisha` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL" +
                        ")"
                )

                val selectSQL =
                    "SELECT `id`, `date`, `year`, `month`, `day`, `productName`, `quantity`, `unit`, " +
                        "`unitPricePoisha`, `totalPoisha`, `createdAt`, `updatedAt` FROM `expenses`"
                db.query(selectSQL)?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val values = ContentValues().apply {
                            put("id", cursor.getLong(0))
                            put("date", cursor.getString(1))
                            put("year", cursor.getInt(2))
                            put("month", cursor.getInt(3))
                            put("day", cursor.getInt(4))
                            put("productName", cursor.getString(5))
                            put("quantity", migrateQuantity(cursor.getString(6)))
                            put("unit", cursor.getString(7))
                            put("unitPricePoisha", cursor.getLong(8))
                            put("totalPoisha", cursor.getLong(9))
                            put("createdAt", cursor.getLong(10))
                            put("updatedAt", cursor.getLong(11))
                        }
                        db.insert("expenses_new", SQLiteDatabase.CONFLICT_NONE, values)
                    }
                }

                db.execSQL("DROP TABLE `expenses`")
                db.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")

                // Recreate the entity indices after the table rebuild
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_date` ON `expenses` (`date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_year_month` ON `expenses` (`year`, `month`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_year_month_date` ON `expenses` (`year`, `month`, `date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_productName` ON `expenses` (`productName`)")
            }
        }

        private fun migrateQuantity(raw: String): String {
            val d = raw.toDoubleOrNull()
            if (d == null) return raw
            // 1.0 was the "no quantity entered" default -> blank
            if (d == 1.0) return ""
            // Whole numbers -> "10", fractions stay "2.5"
            return if (d == Math.floor(d)) d.toLong().toString() else d.toString()
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "monthly_khoroch.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
