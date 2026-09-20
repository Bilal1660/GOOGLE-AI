package com.example.data.model

data class Budget(
    val id: String,
    val category: String, // "ALL" or category name
    val monthlyLimit: Double,
    val monthYear: String, // e.g. "2026-09"
    val updatedAt: Long = System.currentTimeMillis()
)

data class CategoryBudgetProgress(
    val category: ExpenseCategory?,
    val isOverall: Boolean,
    val budgetLimit: Double,
    val spentAmount: Double,
    val remainingAmount: Double,
    val percentSpent: Float,
    val isExceeded: Boolean,
    val isWarning: Boolean
)
