package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ExpenseCategory
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.TransactionType
import com.example.ui.components.CategoryIcon
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.TealSecondary

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        category: ExpenseCategory,
        type: TransactionType,
        note: String
    ) -> Unit,
    onSaveRecurring: ((
        title: String,
        amount: Double,
        category: ExpenseCategory,
        type: TransactionType,
        frequency: RecurrenceFrequency,
        note: String
    ) -> Unit)? = null,
    initialTitle: String = "",
    initialAmount: Double = 0.0,
    initialCategory: ExpenseCategory = ExpenseCategory.FOOD,
    initialNote: String = ""
) {
    var title by remember { mutableStateOf(initialTitle) }
    var amountText by remember { mutableStateOf(if (initialAmount > 0) "%.2f".format(initialAmount) else "") }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var note by remember { mutableStateOf(initialNote) }
    var isRecurring by remember { mutableStateOf(false) }
    var selectedFrequency by remember { mutableStateOf(RecurrenceFrequency.MONTHLY) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("add_expense_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Transaction",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_add_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Expense vs Income Segmented Control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedType == TransactionType.EXPENSE) ExpenseRed else Color.Transparent)
                            .clickable {
                                selectedType = TransactionType.EXPENSE
                                if (selectedCategory == ExpenseCategory.INCOME) {
                                    selectedCategory = ExpenseCategory.FOOD
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Expense",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedType == TransactionType.EXPENSE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedType == TransactionType.INCOME) EmeraldPrimary else Color.Transparent)
                            .clickable {
                                selectedType = TransactionType.INCOME
                                selectedCategory = ExpenseCategory.INCOME
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Income",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedType == TransactionType.INCOME) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amountText = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Amount") },
                    prefix = { Text("$ ", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Merchant / Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null
                    },
                    label = { Text(if (selectedType == TransactionType.EXPENSE) "Merchant or Payee" else "Income Source") },
                    placeholder = { Text(if (selectedType == TransactionType.EXPENSE) "e.g. Trader Joe's, Coffee" else "e.g. Salary, Freelance") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                if (selectedType == TransactionType.EXPENSE) {
                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category horizontal chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExpenseCategory.entries.filter { it != ExpenseCategory.INCOME }.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.displayName) },
                                leadingIcon = {
                                    CategoryIcon(category = cat, size = 24.dp, iconSize = 14.dp)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Optional Notes
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Additional details...") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_note_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Recurring Subscription / Expense Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRecurring) TealSecondary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = if (isRecurring) androidx.compose.foundation.BorderStroke(1.dp, TealSecondary.copy(alpha = 0.4f)) else null
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = null,
                                    tint = if (isRecurring) TealSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Recurring Expense / Subscription",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Repeat periodically (e.g. rent, Netflix)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isRecurring,
                                onCheckedChange = { isRecurring = it },
                                modifier = Modifier.testTag("recurring_expense_switch")
                            )
                        }

                        if (isRecurring) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Repeat Frequency",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TealSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RecurrenceFrequency.entries.forEach { freq ->
                                    FilterChip(
                                        selected = selectedFrequency == freq,
                                        onClick = { selectedFrequency = freq },
                                        label = { Text(freq.displayName) },
                                        modifier = Modifier.testTag("freq_chip_${freq.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        if (amount == null || amount <= 0.0) {
                            errorMessage = "Please enter a valid amount"
                            return@Button
                        }
                        val finalTitle = title.ifBlank {
                            if (selectedType == TransactionType.INCOME) "Income" else selectedCategory.displayName
                        }
                        if (isRecurring && onSaveRecurring != null) {
                            onSaveRecurring(finalTitle, amount, selectedCategory, selectedType, selectedFrequency, note)
                        } else {
                            onSave(finalTitle, amount, selectedCategory, selectedType, note)
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_expense_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (isRecurring) "Schedule Recurring Expense" else "Save Transaction",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
