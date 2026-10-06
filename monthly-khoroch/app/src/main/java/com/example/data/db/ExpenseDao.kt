package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    suspend fun getAllExpensesSync(): List<Expense>

    @Query("SELECT * FROM expenses WHERE year = :year AND month = :month ORDER BY date DESC, id DESC")
    fun getExpensesForMonth(year: Int, month: Int): Flow<List<Expense>>

    @Query("SELECT COALESCE(SUM(totalPoisha), 0) FROM expenses WHERE year = :year AND month = :month")
    fun getTotalExpenseForMonth(year: Int, month: Int): Flow<Long>

    @Query("SELECT COUNT(*) FROM expenses WHERE year = :year AND month = :month")
    fun getExpenseCountForMonth(year: Int, month: Int): Flow<Int>

    @Query("SELECT * FROM expenses WHERE date = :date ORDER BY id DESC")
    fun getExpensesForDate(date: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE productName LIKE '%' || :query || '%' ORDER BY date DESC, id DESC")
    fun searchExpenses(query: String): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>): List<Long>

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("DELETE FROM expenses")
    suspend fun clearAllExpenses()
}
