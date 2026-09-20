package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ExpenseCategory

/**
 * Room Entity representing expense and income categories stored locally on the device.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String, // e.g. "FOOD", "GROCERIES", or unique ID for custom category
    val displayName: String,
    val iconName: String,
    val colorHex: String,
    val isDefault: Boolean = false,
    val defaultBudgetLimit: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromExpenseCategory(category: ExpenseCategory, defaultLimit: Double = 0.0): CategoryEntity {
            return CategoryEntity(
                id = category.name,
                displayName = category.displayName,
                iconName = category.iconName,
                colorHex = String.format("#%08X", (category.color.value.toLong() and 0xFFFFFFFFL)),
                isDefault = true,
                defaultBudgetLimit = defaultLimit,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}
