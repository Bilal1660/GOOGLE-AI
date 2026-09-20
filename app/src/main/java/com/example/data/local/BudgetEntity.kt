package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Budget

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val id: String, // e.g. "OVERALL_2026-09" or "FOOD_2026-09"
    val category: String,
    val monthlyLimit: Double,
    val monthYear: String,
    val updatedAt: Long
) {
    fun toBudget(): Budget = Budget(
        id = id,
        category = category,
        monthlyLimit = monthlyLimit,
        monthYear = monthYear,
        updatedAt = updatedAt
    )

    companion object {
        fun fromBudget(budget: Budget): BudgetEntity = BudgetEntity(
            id = budget.id,
            category = budget.category,
            monthlyLimit = budget.monthlyLimit,
            monthYear = budget.monthYear,
            updatedAt = budget.updatedAt
        )
    }
}
