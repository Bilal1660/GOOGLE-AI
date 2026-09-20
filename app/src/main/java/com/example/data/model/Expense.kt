package com.example.data.model

import com.example.ui.theme.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class ExpenseCategory(
    val displayName: String,
    val iconName: String,
    val color: Color
) {
    FOOD("Food & Dining", "Restaurant", CatFood),
    GROCERIES("Groceries", "ShoppingCart", CatGroceries),
    SHOPPING("Shopping", "ShoppingBag", CatShopping),
    TRANSPORT("Transport", "DirectionsCar", CatTransport),
    BILLS("Bills & Utilities", "ReceiptLong", CatBills),
    ENTERTAINMENT("Entertainment", "Movie", CatEntertainment),
    HEALTH("Health & Fitness", "Favorite", CatHealth),
    TRAVEL("Travel", "Flight", CatTravel),
    EDUCATION("Education", "School", CatEducation),
    INCOME("Salary & Income", "AccountBalanceWallet", CatIncome),
    OTHER("Other", "MoreHoriz", CatOther);

    companion object {
        fun fromString(value: String): ExpenseCategory {
            return entries.firstOrNull { 
                it.name.equals(value, ignoreCase = true) || 
                it.displayName.equals(value, ignoreCase = true) 
            } ?: OTHER
        }
    }
}

data class ScannedItem(
    val name: String,
    val price: Double
)

data class Expense(
    val id: String,
    val title: String,
    val amount: Double,
    val category: ExpenseCategory,
    val date: Long = System.currentTimeMillis(),
    val type: TransactionType = TransactionType.EXPENSE,
    val note: String = "",
    val receiptUri: String? = null,
    val merchantName: String? = null,
    val isReceiptScanned: Boolean = false,
    val items: List<ScannedItem> = emptyList(),
    val taxAmount: Double = 0.0,
    val isRecurring: Boolean = false,
    val recurringFrequency: String? = null,
    val deviceId: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
