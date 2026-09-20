package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryBudgetProgress
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.sync.SyncState
import com.example.data.sync.SyncStatus
import com.example.ui.MonthlyFinanceSummary
import com.example.ui.components.BudgetProgressBar
import com.example.ui.components.CategoryIcon
import com.example.ui.components.DeleteTransactionConfirmationDialog
import com.example.ui.components.ExpenseCard
import com.example.ui.components.MonthlySpendingVisualSummary
import com.example.ui.components.ReceiptDetailDialog
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    summary: MonthlyFinanceSummary,
    recentExpenses: List<Expense>,
    categoryProgresses: List<CategoryBudgetProgress>,
    syncStatus: SyncStatus,
    onScanReceiptClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onViewBudgetsClick: () -> Unit,
    onViewSyncClick: () -> Unit,
    onViewAllTransactionsClick: () -> Unit,
    onDeleteExpense: (String) -> Unit
) {
    var selectedExpenseForDetail by remember { mutableStateOf<Expense?>(null) }
    var transactionPendingDelete by remember { mutableStateOf<Expense?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Executive Balance Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("executive_balance_card"),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF0F172A),
                                    Color(0xFF064E3B),
                                    Color(0xFF047857)
                                )
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Top bar inside card: Title + Real-time Sync Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly Overview",
                                color = Color(0xFFD1FAE5),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Clickable sync status pill
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Black.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(onClick = onViewSyncClick)
                                    .testTag("dashboard_sync_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (syncStatus.state == SyncState.CONNECTED_LIVE) EmeraldLight else TealLight
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = syncStatus.syncGroupId,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = EmeraldLight,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Net Balance Display
                        Text(
                            text = "Net Cash Flow",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${if (summary.netSavings >= 0) "+" else ""}$${"%.2f".format(summary.netSavings)}",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Income and Expense Split
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black.copy(alpha = 0.25f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = EmeraldLight,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Total Income",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "$${"%.2f".format(summary.totalIncome)}",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(30.dp)
                                    .background(Color.White.copy(alpha = 0.15f))
                            )

                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = ExpenseRedLight,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Total Expenses",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "$${"%.2f".format(summary.totalExpense)}",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Actions Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.DocumentScanner,
                    label = "Scan Receipt",
                    tint = EmeraldPrimary,
                    containerColor = EmeraldPrimary.copy(alpha = 0.12f),
                    onClick = onScanReceiptClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_scan_receipt")
                )

                QuickActionButton(
                    icon = Icons.Default.AddCircle,
                    label = "Add Expense",
                    tint = TealSecondary,
                    containerColor = TealSecondary.copy(alpha = 0.12f),
                    onClick = onAddExpenseClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_add_expense")
                )

                QuickActionButton(
                    icon = Icons.Default.PieChart,
                    label = "Budgets",
                    tint = AmberAccent,
                    containerColor = AmberAccent.copy(alpha = 0.12f),
                    onClick = onViewBudgetsClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_budgets")
                )

                QuickActionButton(
                    icon = Icons.Default.CloudSync,
                    label = "Multi-Sync",
                    tint = Color(0xFF6366F1),
                    containerColor = Color(0xFF6366F1).copy(alpha = 0.12f),
                    onClick = onViewSyncClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_sync")
                )
            }
        }

        // Monthly Spending vs Budget Visual Summary (Circular Chart & Progress Bar)
        item {
            MonthlySpendingVisualSummary(
                summary = summary,
                categoryProgresses = categoryProgresses
            )
        }

        // Budget Pace Overview Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_budget_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Monthly Budget Pace",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(onClick = onViewBudgetsClick) {
                            Text("Manage", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    BudgetProgressBar(
                        spent = summary.totalExpense,
                        limit = summary.overallBudget,
                        showAmounts = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Daily Safe Spend Target:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$${"%.2f".format(summary.dailySafeSpend)} / day",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.dailySafeSpend > 0) EmeraldPrimary else ExpenseRed
                        )
                    }

                    if (summary.alerts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ExpenseRed.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = summary.alerts.first(),
                                    fontSize = 11.sp,
                                    color = ExpenseRed,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Category Spending Breakdown
        if (categoryProgresses.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Top Spending Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        categoryProgresses.take(4).forEach { progress ->
                            val cat = progress.category ?: ExpenseCategory.OTHER
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryIcon(category = cat, size = 30.dp, iconSize = 16.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = cat.displayName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                        Text(text = "$${"%.2f".format(progress.spentAmount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { progress.percentSpent.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = cat.color,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Activity Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onViewAllTransactionsClick) {
                    Text("View All (${recentExpenses.size})", fontSize = 12.sp)
                }
            }
        }

        if (recentExpenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No recent transactions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onScanReceiptClick) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan First Receipt")
                        }
                    }
                }
            }
        } else {
            items(recentExpenses.take(5), key = { it.id }) { expense ->
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

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
