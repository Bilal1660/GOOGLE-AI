package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.model.CategoryBudgetProgress
import com.example.data.model.ExpenseCategory
import com.example.ui.MonthlyFinanceSummary
import com.example.ui.components.BudgetProgressBar
import com.example.ui.components.CategoryIcon
import com.example.ui.components.MonthlySpendingVisualSummary
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed

@Composable
fun BudgetScreen(
    summary: MonthlyFinanceSummary,
    categoryProgresses: List<CategoryBudgetProgress>,
    onSetBudget: (categoryName: String, limit: Double) -> Unit
) {
    var showEditBudgetDialog by remember { mutableStateOf(false) }
    var selectedCategoryForEdit by remember { mutableStateOf<String>("ALL") }
    var initialLimitForEdit by remember { mutableDoubleStateOf(summary.overallBudget) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    selectedCategoryForEdit = "ALL"
                    initialLimitForEdit = summary.overallBudget
                    showEditBudgetDialog = true
                },
                icon = { Icon(Icons.Default.Tune, contentDescription = null) },
                text = { Text("Adjust Budgets") },
                modifier = Modifier.testTag("adjust_budget_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Text(
                    text = "Budget Planner & Tracker",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Control your spending targets & daily pacing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Visual Summary Component (Circular Chart & Progress Bar)
            item {
                MonthlySpendingVisualSummary(
                    summary = summary,
                    categoryProgresses = categoryProgresses
                )
            }

            // Overall Monthly Target Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("overall_budget_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Monthly Spending Target",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${summary.daysRemaining} days left in this cycle",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    selectedCategoryForEdit = "ALL"
                                    initialLimitForEdit = summary.overallBudget
                                    showEditBudgetDialog = true
                                },
                                modifier = Modifier.testTag("edit_overall_budget_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Budget", tint = EmeraldPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        BudgetProgressBar(
                            spent = summary.totalExpense,
                            limit = summary.overallBudget,
                            showAmounts = true
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Daily Safe Spend & Remaining Metrics
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Daily Safe Spend",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$${"%.2f".format(summary.dailySafeSpend)}/day",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (summary.dailySafeSpend > 0) EmeraldPrimary else ExpenseRed
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Remaining Balance",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$${"%.2f".format(summary.overallRemaining)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (summary.overallRemaining >= 0) MaterialTheme.colorScheme.onSurface else ExpenseRed
                                )
                            }
                        }
                    }
                }
            }

            // Budget Alerts (if any)
            if (summary.alerts.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Budget Alerts & Warnings",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                        summary.alerts.forEach { alert ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Alert",
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = alert,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = ExpenseRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Category Budgets Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category Limits (${categoryProgresses.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = {
                            selectedCategoryForEdit = ExpenseCategory.FOOD.name
                            initialLimitForEdit = 400.0
                            showEditBudgetDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Limit")
                    }
                }
            }

            // Category Budgets List
            if (categoryProgresses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No category limits configured yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(onClick = {
                                selectedCategoryForEdit = ExpenseCategory.FOOD.name
                                initialLimitForEdit = 400.0
                                showEditBudgetDialog = true
                            }) {
                                Text("Set Category Budget")
                            }
                        }
                    }
                }
            } else {
                items(categoryProgresses) { progress ->
                    val cat = progress.category ?: ExpenseCategory.OTHER
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedCategoryForEdit = cat.name
                                initialLimitForEdit = progress.budgetLimit
                                showEditBudgetDialog = true
                            }
                            .testTag("budget_item_${cat.name}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CategoryIcon(category = cat, size = 38.dp, iconSize = 20.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = cat.displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (progress.budgetLimit > 0) {
                                                "${(progress.percentSpent * 100).toInt()}% used"
                                            } else "No limit set",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = when {
                                                progress.isExceeded -> ExpenseRed
                                                progress.isWarning -> AmberAccent
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$${"%.2f".format(progress.spentAmount)}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (progress.isExceeded) ExpenseRed else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (progress.budgetLimit > 0) "of $${"%.0f".format(progress.budgetLimit)}" else "spent",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (progress.budgetLimit > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                BudgetProgressBar(
                                    spent = progress.spentAmount,
                                    limit = progress.budgetLimit,
                                    showAmounts = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Budget Editor Dialog
    if (showEditBudgetDialog) {
        BudgetEditDialog(
            initialCategory = selectedCategoryForEdit,
            initialLimit = initialLimitForEdit,
            onDismiss = { showEditBudgetDialog = false },
            onSave = { category, limit ->
                onSetBudget(category, limit)
                showEditBudgetDialog = false
            }
        )
    }
}

@Composable
fun BudgetEditDialog(
    initialCategory: String,
    initialLimit: Double,
    onDismiss: () -> Unit,
    onSave: (category: String, limit: Double) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var limitText by remember { mutableStateOf(if (initialLimit > 0) "%.0f".format(initialLimit) else "500") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (selectedCategory == "ALL") "Set Overall Monthly Budget" else "Set Category Limit",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Category Picker (if not overall)
                Text("Select Scope:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == "ALL",
                        onClick = { selectedCategory = "ALL" },
                        label = { Text("Overall Total") }
                    )
                    FilterChip(
                        selected = selectedCategory != "ALL",
                        onClick = {
                            if (selectedCategory == "ALL") selectedCategory = ExpenseCategory.FOOD.name
                        },
                        label = { Text("Category Specific") }
                    )
                }

                if (selectedCategory != "ALL") {
                    Spacer(modifier = Modifier.height(10.dp))
                    var expanded by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val currentCat = ExpenseCategory.fromString(selectedCategory)
                        CategoryIcon(category = currentCat, size = 22.dp, iconSize = 14.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(currentCat.displayName)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ExpenseCategory.entries.filter { it != ExpenseCategory.INCOME }.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName) },
                                leadingIcon = { CategoryIcon(category = cat, size = 24.dp, iconSize = 14.dp) },
                                onClick = {
                                    selectedCategory = cat.name
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = limitText,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            limitText = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Monthly Budget Limit") },
                    prefix = { Text("$ ", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_limit_input")
                )

                // Quick preset buttons
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(200, 500, 1000, 2500).forEach { preset ->
                        SuggestionChip(
                            onClick = { limitText = preset.toString() },
                            label = { Text("$$preset", fontSize = 11.sp) }
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = limitText.toDoubleOrNull()
                    if (limit == null || limit <= 0) {
                        errorMessage = "Please enter a valid limit"
                        return@Button
                    }
                    onSave(selectedCategory, limit)
                },
                modifier = Modifier.testTag("save_budget_button")
            ) {
                Text("Save Limit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
