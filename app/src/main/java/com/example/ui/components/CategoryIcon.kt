package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.ExpenseCategory

fun getCategoryIconVector(category: ExpenseCategory): ImageVector {
    return when (category) {
        ExpenseCategory.FOOD -> Icons.Default.Restaurant
        ExpenseCategory.GROCERIES -> Icons.Default.ShoppingCart
        ExpenseCategory.SHOPPING -> Icons.Default.ShoppingBag
        ExpenseCategory.TRANSPORT -> Icons.Default.DirectionsCar
        ExpenseCategory.BILLS -> Icons.Default.ReceiptLong
        ExpenseCategory.ENTERTAINMENT -> Icons.Default.Movie
        ExpenseCategory.HEALTH -> Icons.Default.Favorite
        ExpenseCategory.TRAVEL -> Icons.Default.Flight
        ExpenseCategory.EDUCATION -> Icons.Default.School
        ExpenseCategory.INCOME -> Icons.Default.AccountBalanceWallet
        ExpenseCategory.OTHER -> Icons.Default.MoreHoriz
    }
}

@Composable
fun CategoryIcon(
    category: ExpenseCategory,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(category.color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getCategoryIconVector(category),
            contentDescription = category.displayName,
            tint = category.color,
            modifier = Modifier.size(iconSize)
        )
    }
}
