package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.ScannedItem
import com.example.data.model.TransactionType
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val amount: Double,
    val category: String,
    val date: Long,
    val type: String,
    val note: String,
    val receiptUri: String?,
    val merchantName: String?,
    val isReceiptScanned: Boolean,
    val itemsJson: String?,
    val taxAmount: Double,
    val isRecurring: Boolean = false,
    val recurringFrequency: String? = null,
    val deviceId: String,
    val updatedAt: Long
) {
    fun toExpense(moshi: Moshi): Expense {
        val items: List<ScannedItem> = try {
            if (!itemsJson.isNullOrBlank()) {
                val listType = Types.newParameterizedType(List::class.java, Map::class.java)
                val adapter = moshi.adapter<List<Map<String, Any>>>(listType)
                val rawList = adapter.fromJson(itemsJson) ?: emptyList()
                rawList.mapNotNull { map ->
                    val name = map["name"] as? String ?: return@mapNotNull null
                    val price = (map["price"] as? Number)?.toDouble() ?: 0.0
                    ScannedItem(name, price)
                }
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        return Expense(
            id = id,
            title = title,
            amount = amount,
            category = ExpenseCategory.fromString(category),
            date = date,
            type = try { TransactionType.valueOf(type) } catch (_: Exception) { TransactionType.EXPENSE },
            note = note,
            receiptUri = receiptUri,
            merchantName = merchantName,
            isReceiptScanned = isReceiptScanned,
            items = items,
            taxAmount = taxAmount,
            isRecurring = isRecurring,
            recurringFrequency = recurringFrequency,
            deviceId = deviceId,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromExpense(expense: Expense, moshi: Moshi): ExpenseEntity {
            val itemsJson = try {
                if (expense.items.isNotEmpty()) {
                    val listType = Types.newParameterizedType(List::class.java, Map::class.java)
                    val adapter = moshi.adapter<List<Map<String, Any>>>(listType)
                    val mapped = expense.items.map { mapOf("name" to it.name, "price" to it.price) }
                    adapter.toJson(mapped)
                } else null
            } catch (_: Exception) {
                null
            }

            return ExpenseEntity(
                id = expense.id,
                title = expense.title,
                amount = expense.amount,
                category = expense.category.name,
                date = expense.date,
                type = expense.type.name,
                note = expense.note,
                receiptUri = expense.receiptUri,
                merchantName = expense.merchantName,
                isReceiptScanned = expense.isReceiptScanned,
                itemsJson = itemsJson,
                taxAmount = expense.taxAmount,
                isRecurring = expense.isRecurring,
                recurringFrequency = expense.recurringFrequency,
                deviceId = expense.deviceId,
                updatedAt = expense.updatedAt
            )
        }
    }
}
