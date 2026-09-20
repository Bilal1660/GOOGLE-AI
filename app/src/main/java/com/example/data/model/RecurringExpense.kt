package com.example.data.model

enum class RecurrenceFrequency(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    BIWEEKLY("Bi-weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly");

    companion object {
        fun fromString(value: String): RecurrenceFrequency {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MONTHLY
        }
    }
}

data class RecurringExpense(
    val id: String,
    val title: String,
    val amount: Double,
    val category: ExpenseCategory,
    val type: TransactionType = TransactionType.EXPENSE,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val startDate: Long,
    val nextDueDate: Long,
    val lastGeneratedDate: Long? = null,
    val isActive: Boolean = true,
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
