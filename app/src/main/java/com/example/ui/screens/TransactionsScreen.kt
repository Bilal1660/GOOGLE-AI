package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.widget.Toast
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.RecurringExpense
import com.example.data.model.TransactionType
import com.example.ui.components.CategoryIcon
import com.example.ui.components.DeleteTransactionConfirmationDialog
import com.example.ui.components.ExpenseCard
import com.example.ui.components.ReceiptDetailDialog
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.TealSecondary
import com.example.util.CsvExportUtil

@Composable
fun TransactionsScreen(
    expenses: List<Expense>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: ExpenseCategory?,
    onCategoryChange: (ExpenseCategory?) -> Unit,
    selectedType: TransactionType?,
    onTypeChange: (TransactionType?) -> Unit,
    onDeleteExpense: (String) -> Unit,
    recurringExpenses: List<RecurringExpense> = emptyList(),
    filterOnlyRecurring: Boolean = false,
    onToggleRecurringFilter: (Boolean) -> Unit = {},
    onDeleteRecurring: (String) -> Unit = {},
    onToggleRecurringActive: (String, Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var selectedExpenseForDetail by remember { mutableStateOf<Expense?>(null) }
    var transactionPendingDelete by remember { mutableStateOf<Expense?>(null) }
    var showExportMenu by remember { mutableStateOf(false) }

    // SAF Create Document Launcher for saving CSV file
    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    CsvExportUtil.writeCsvToStream(outputStream, expenses)
                }
                Toast.makeText(context, "CSV report saved successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save CSV: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val totalExpenseSum = expenses.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val totalIncomeSum = expenses.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with Export Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "All Transactions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Search, filter & review your expenses and income",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    FilledTonalButton(
                        onClick = { showExportMenu = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("export_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export CSV",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export CSV", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share CSV Report") },
                            leadingIcon = {
                                Icon(Icons.Default.Share, contentDescription = null, tint = EmeraldPrimary)
                            },
                            onClick = {
                                showExportMenu = false
                                if (expenses.isEmpty()) {
                                    Toast.makeText(context, "No expenses to export", Toast.LENGTH_SHORT).show()
                                    return@DropdownMenuItem
                                }
                                try {
                                    val file = CsvExportUtil.createCsvFile(context, expenses)
                                    val shareIntent = CsvExportUtil.createShareIntent(context, file)
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Financial Report"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error exporting CSV: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("export_share_csv_item")
                        )

                        DropdownMenuItem(
                            text = { Text("Save CSV to Device...") },
                            leadingIcon = {
                                Icon(Icons.Default.SaveAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            onClick = {
                                showExportMenu = false
                                if (expenses.isEmpty()) {
                                    Toast.makeText(context, "No expenses to export", Toast.LENGTH_SHORT).show()
                                    return@DropdownMenuItem
                                }
                                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US).format(java.util.Date())
                                saveCsvLauncher.launch("expenses_report_$timestamp.csv")
                            },
                            modifier = Modifier.testTag("export_save_csv_item")
                        )
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search by merchant, note, or item...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_search_bar"),
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Filter chips (Expense vs Income vs All vs Recurring)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == null && !filterOnlyRecurring,
                    onClick = {
                        onTypeChange(null)
                        onToggleRecurringFilter(false)
                    },
                    label = { Text("All Types") }
                )
                FilterChip(
                    selected = selectedType == TransactionType.EXPENSE && !filterOnlyRecurring,
                    onClick = {
                        onTypeChange(TransactionType.EXPENSE)
                        onToggleRecurringFilter(false)
                    },
                    label = { Text("Expenses Only") }
                )
                FilterChip(
                    selected = selectedType == TransactionType.INCOME && !filterOnlyRecurring,
                    onClick = {
                        onTypeChange(TransactionType.INCOME)
                        onToggleRecurringFilter(false)
                    },
                    label = { Text("Income Only") }
                )
                FilterChip(
                    selected = filterOnlyRecurring,
                    onClick = { onToggleRecurringFilter(!filterOnlyRecurring) },
                    label = { Text("Recurring Only") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("filter_recurring_chip")
                )
            }
        }

        // Active Recurring Schedules Summary Card (shown if schedules exist)
        if (recurringExpenses.isNotEmpty()) {
            item {
                var isExpanded by remember { mutableStateOf(false) }
                val activeCount = recurringExpenses.count { it.isActive }
                val monthlyCommitment = recurringExpenses.filter { it.isActive }.sumOf { rec ->
                    when (rec.frequency) {
                        com.example.data.model.RecurrenceFrequency.DAILY -> rec.amount * 30
                        com.example.data.model.RecurrenceFrequency.WEEKLY -> rec.amount * 4.33
                        com.example.data.model.RecurrenceFrequency.BIWEEKLY -> rec.amount * 2.16
                        com.example.data.model.RecurrenceFrequency.MONTHLY -> rec.amount
                        com.example.data.model.RecurrenceFrequency.YEARLY -> rec.amount / 12.0
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recurring_schedules_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = TealSecondary.copy(alpha = 0.08f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TealSecondary.copy(alpha = 0.35f))
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
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = TealSecondary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Repeat,
                                            contentDescription = null,
                                            tint = TealSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Scheduled Subscriptions & Bills",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$activeCount active • ~$${"%.2f".format(monthlyCommitment)}/mo committed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = { isExpanded = !isExpanded },
                                modifier = Modifier.testTag("toggle_recurring_schedules_expand")
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = TealSecondary
                                )
                            }
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = TealSecondary.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                recurringExpenses.forEach { rec ->
                                    val nextDateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(rec.nextDueDate))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 1.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CategoryIcon(category = rec.category, size = 32.dp, iconSize = 16.dp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = rec.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "${rec.frequency.displayName} • Next: $nextDateStr",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "$${"%.2f".format(rec.amount)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Switch(
                                                checked = rec.isActive,
                                                onCheckedChange = { onToggleRecurringActive(rec.id, it) },
                                                modifier = Modifier.testTag("switch_rec_${rec.id}")
                                            )
                                            IconButton(
                                                onClick = { onDeleteRecurring(rec.id) },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .testTag("delete_rec_${rec.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete Recurring Rule",
                                                    tint = ExpenseRed,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategoryChange(null) },
                    label = { Text("All Categories") }
                )
                ExpenseCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onCategoryChange(if (selectedCategory == cat) null else cat) },
                        label = { Text(cat.displayName) },
                        leadingIcon = { CategoryIcon(category = cat, size = 20.dp, iconSize = 12.dp) }
                    )
                }
            }
        }

        // Filter Summary Banner
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${expenses.size} results",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (totalExpenseSum > 0) {
                        Text(
                            text = "Spend: $${"%.2f".format(totalExpenseSum)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (totalIncomeSum > 0) {
                        Text(
                            text = "Income: +$${"%.2f".format(totalIncomeSum)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }
            }
        }

        // Empty state or Items
        if (expenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching transactions found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try adjusting your search query or filters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(expenses, key = { it.id }) { expense ->
                ExpenseCard(
                    expense = expense,
                    onClick = { selectedExpenseForDetail = expense },
                    onDelete = { transactionPendingDelete = expense }
                )
            }
        }
    }

    // Detail dialog
    val currentDetail = selectedExpenseForDetail
    if (currentDetail != null) {
        ReceiptDetailDialog(
            expense = currentDetail,
            onDismiss = { selectedExpenseForDetail = null },
            onDelete = {
                val toDelete = currentDetail
                selectedExpenseForDetail = null
                transactionPendingDelete = toDelete
            }
        )
    }

    // Custom Delete Confirmation AlertDialog
    val pendingDelete = transactionPendingDelete
    if (pendingDelete != null) {
        DeleteTransactionConfirmationDialog(
            expense = pendingDelete,
            onConfirmDelete = {
                onDeleteExpense(pendingDelete.id)
                transactionPendingDelete = null
            },
            onDismiss = {
                transactionPendingDelete = null
            }
        )
    }
}
