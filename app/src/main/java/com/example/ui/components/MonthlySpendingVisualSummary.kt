package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryBudgetProgress
import com.example.data.model.ExpenseCategory
import com.example.ui.MonthlyFinanceSummary
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.TealSecondary

enum class ChartDisplayMode {
    CIRCULAR_CHART,
    PROGRESS_BAR
}

@Composable
fun MonthlySpendingVisualSummary(
    summary: MonthlyFinanceSummary,
    categoryProgresses: List<CategoryBudgetProgress>,
    modifier: Modifier = Modifier,
    initialMode: ChartDisplayMode = ChartDisplayMode.CIRCULAR_CHART
) {
    var displayMode by remember { mutableStateOf(initialMode) }
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }

    val spent = summary.totalExpense
    val budget = summary.overallBudget
    val ratio = if (budget > 0) (spent / budget).toFloat() else 0f
    val isOverBudget = spent > budget && budget > 0
    val isNearBudget = ratio >= 0.8f && !isOverBudget

    val statusColor = when {
        isOverBudget -> ExpenseRed
        isNearBudget -> AmberAccent
        else -> EmeraldPrimary
    }

    val statusLabel = when {
        isOverBudget -> "Over Budget by $${"%.2f".format(spent - budget)}"
        isNearBudget -> "Caution: ${(ratio * 100).toInt()}% Used"
        else -> "On Track: ${(ratio * 100).toInt()}% Used"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_spending_visual_summary_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row: Title and Mode Switcher Segmented Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Spending vs. Budget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Visual Summary & Category Allocations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Mode Toggle Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (displayMode == ChartDisplayMode.CIRCULAR_CHART) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .clickable { displayMode = ChartDisplayMode.CIRCULAR_CHART }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = "Circular Chart",
                                tint = if (displayMode == ChartDisplayMode.CIRCULAR_CHART) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (displayMode == ChartDisplayMode.PROGRESS_BAR) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .clickable { displayMode = ChartDisplayMode.PROGRESS_BAR }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewStream,
                                contentDescription = "Progress Bar",
                                tint = if (displayMode == ChartDisplayMode.PROGRESS_BAR) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Visual Graphic: Circular Gauge or Multi-Segment Progress Bar
            AnimatedContent(
                targetState = displayMode,
                label = "chart_switch_animation"
            ) { mode ->
                when (mode) {
                    ChartDisplayMode.CIRCULAR_CHART -> {
                        CircularSpendingChart(
                            spent = spent,
                            budget = budget,
                            ratio = ratio,
                            statusColor = statusColor,
                            statusLabel = statusLabel,
                            categoryProgresses = categoryProgresses,
                            selectedCategory = selectedCategory
                        )
                    }
                    ChartDisplayMode.PROGRESS_BAR -> {
                        StackedProgressBarSummary(
                            spent = spent,
                            budget = budget,
                            ratio = ratio,
                            statusColor = statusColor,
                            statusLabel = statusLabel,
                            categoryProgresses = categoryProgresses
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Key Metrics Pill Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricColumn(
                    title = "Budget Target",
                    value = "$${"%.0f".format(budget)}",
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
                MetricColumn(
                    title = "Total Spent",
                    value = "$${"%.2f".format(spent)}",
                    color = statusColor
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
                MetricColumn(
                    title = if (isOverBudget) "Over Budget" else "Remaining",
                    value = if (isOverBudget) "+$${"%.2f".format(spent - budget)}" else "$${"%.2f".format(budget - spent)}",
                    color = if (isOverBudget) ExpenseRed else EmeraldPrimary
                )
            }

            // Category Legend
            if (categoryProgresses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Category Breakdown",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable / Wrapped Category Legend Chips
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categoryProgresses.filter { it.spentAmount > 0 }.forEach { catProg ->
                        val cat = catProg.category ?: ExpenseCategory.OTHER
                        val percentOfTotal = if (spent > 0) (catProg.spentAmount / spent) * 100 else 0.0
                        val isSelected = selectedCategory == cat

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) cat.color.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable {
                                    selectedCategory = if (isSelected) null else cat
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(cat.color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cat.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$${"%.2f".format(catProg.spentAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${"%.1f".format(percentOfTotal)}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularSpendingChart(
    spent: Double,
    budget: Double,
    ratio: Float,
    statusColor: Color,
    statusLabel: String,
    categoryProgresses: List<CategoryBudgetProgress>,
    selectedCategory: ExpenseCategory?
) {
    val animatedRatio by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1f),
        animationSpec = tween(1000),
        label = "circular_ratio"
    )

    Box(
        modifier = Modifier
            .size(230.dp)
            .testTag("circular_spending_chart"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(210.dp)) {
            val strokeWidth = 20.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val radius = diameter / 2f
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            // Background track
            drawArc(
                color = Color(0xFFE2E8F0),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Check if we render category segments or a unified spending progress arc
            val activeCategories = categoryProgresses.filter { it.spentAmount > 0 }
            if (activeCategories.isNotEmpty() && spent > 0) {
                var currentAngle = -90f
                val totalSweep = 360f * animatedRatio

                activeCategories.forEach { catProg ->
                    val cat = catProg.category ?: ExpenseCategory.OTHER
                    val sliceFraction = (catProg.spentAmount / spent).toFloat()
                    val sweep = totalSweep * sliceFraction
                    val isHighlighted = selectedCategory == null || selectedCategory == cat
                    val arcColor = if (isHighlighted) cat.color else cat.color.copy(alpha = 0.25f)

                    drawArc(
                        color = arcColor,
                        startAngle = currentAngle,
                        sweepAngle = sweep.coerceAtLeast(1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = if (selectedCategory == cat) strokeWidth * 1.25f else strokeWidth,
                            cap = StrokeCap.Butt
                        )
                    )
                    currentAngle += sweep
                }
            } else {
                // Single unified progress arc
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            EmeraldLight,
                            statusColor,
                            statusColor
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedRatio,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Center Text Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(
                text = "${(ratio * 100).toInt()}%",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = statusColor,
                letterSpacing = (-1).sp
            )

            Text(
                text = "$${"%.0f".format(spent)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "of $${"%.0f".format(budget)} budget",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = statusColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = if (ratio > 1f) "Over Budget" else "Pacing Safe",
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun StackedProgressBarSummary(
    spent: Double,
    budget: Double,
    ratio: Float,
    statusColor: Color,
    statusLabel: String,
    categoryProgresses: List<CategoryBudgetProgress>
) {
    val animatedRatio by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1.2f),
        animationSpec = tween(800),
        label = "progress_ratio"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stacked_progress_bar_summary")
    ) {
        // Pacing Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (ratio > 1f) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Text(
                text = "${(ratio * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = statusColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress Track with Stacked Category Segments
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val activeCategories = categoryProgresses.filter { it.spentAmount > 0 }
            if (activeCategories.isNotEmpty() && spent > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedRatio.coerceAtMost(1f))
                ) {
                    activeCategories.forEach { catProg ->
                        val cat = catProg.category ?: ExpenseCategory.OTHER
                        val weight = (catProg.spentAmount / spent).toFloat()
                        if (weight > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(weight)
                                    .background(cat.color)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedRatio.coerceAtMost(1f))
                        .background(statusColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Milestone Ticks (0%, 25%, 50%, 75%, 100%)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("$0", "25%", "50%", "75%", "$${"%.0f".format(budget)}").forEach { milestone ->
                Text(
                    text = milestone,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(
    title: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 15.sp
        )
    }
}
