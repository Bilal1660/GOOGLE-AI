package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ExpenseCategory
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.RecurringExpense
import com.example.data.model.TransactionType

@Entity(tableName = "recurring_expenses")
data class RecurringExpenseEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val amount: Double,
    val category: String,
    val type: String,
    val frequency: String,
    val startDate: Long,
    val nextDueDate: Long,
    val lastGeneratedDate: Long?,
    val isActive: Boolean,
    val note: String,
    val updatedAt: Long
) {
    fun toRecurringExpense(): RecurringExpense {
        return RecurringExpense(
            id = id,
            title = title,
            amount = amount,
            category = ExpenseCategory.fromString(category),
            type = try { TransactionType.valueOf(type) } catch (_: Exception) { TransactionType.EXPENSE },
            frequency = RecurrenceFrequency.fromString(frequency),
            startDate = startDate,
            nextDueDate = nextDueDate,
            lastGeneratedDate = lastGeneratedDate,
            isActive = isActive,
            note = note,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromRecurringExpense(recurring: RecurringExpense): RecurringExpenseEntity {
            return RecurringExpenseEntity(
                id = recurring.id,
                title = recurring.title,
                amount = recurring.amount,
                category = recurring.category.name,
                type = recurring.type.name,
                frequency = recurring.frequency.name,
                startDate = recurring.startDate,
                nextDueDate = recurring.nextDueDate,
                lastGeneratedDate = recurring.lastGeneratedDate,
                isActive = recurring.isActive,
                note = recurring.note,
                updatedAt = recurring.updatedAt
            )
        }
    }
}
