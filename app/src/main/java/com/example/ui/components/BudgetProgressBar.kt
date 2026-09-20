package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed

@Composable
fun BudgetProgressBar(
    spent: Double,
    limit: Double,
    modifier: Modifier = Modifier,
    label: String? = null,
    showAmounts: Boolean = true
) {
    val ratio = if (limit > 0) (spent / limit).toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1f),
        label = "budget_progress"
    )

    val progressColor = when {
        spent > limit -> ExpenseRed
        ratio >= 0.8f -> AmberAccent
        else -> EmeraldPrimary
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (showAmounts) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$${"%.2f".format(spent)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (spent > limit) ExpenseRed else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " / $${"%.0f".format(limit)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Bar container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(4.dp))
                    .background(progressColor)
            )
        }

        if (spent > limit) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Over budget by $${"%.2f".format(spent - limit)}",
                color = ExpenseRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
