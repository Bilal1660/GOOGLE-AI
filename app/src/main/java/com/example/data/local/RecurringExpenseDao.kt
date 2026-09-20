package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {
    @Query("SELECT * FROM recurring_expenses ORDER BY nextDueDate ASC")
    fun getAllRecurringExpenses(): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1 ORDER BY nextDueDate ASC")
    fun getActiveRecurringExpenses(): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE id = :id")
    suspend fun getRecurringExpenseById(id: String): RecurringExpenseEntity?

    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1 AND nextDueDate <= :timestamp")
    suspend fun getDueRecurringExpenses(timestamp: Long): List<RecurringExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(recurringExpense: RecurringExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(recurringExpenses: List<RecurringExpenseEntity>)

    @Update
    suspend fun update(recurringExpense: RecurringExpenseEntity)

    @Query("UPDATE recurring_expenses SET isActive = :isActive WHERE id = :id")
    suspend fun setRecurringExpenseActive(id: String, isActive: Boolean)

    @Query("DELETE FROM recurring_expenses WHERE id = :id")
    suspend fun deleteRecurringExpenseById(id: String)
}
