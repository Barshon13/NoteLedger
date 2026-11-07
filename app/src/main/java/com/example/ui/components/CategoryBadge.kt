package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.data.model.ExpenseCategory
import com.example.ui.theme.CategoryBills
import com.example.ui.theme.CategoryFood
import com.example.ui.theme.CategoryNeutralBg
import com.example.ui.theme.CategoryOther
import com.example.ui.theme.CategoryShopping
import com.example.ui.theme.CategoryTransport

fun getCategoryIcon(categoryName: String): ImageVector {
    return when (ExpenseCategory.fromString(categoryName)) {
        ExpenseCategory.FOOD -> Icons.Default.Fastfood
        ExpenseCategory.TRANSPORT -> Icons.Default.DirectionsBus
        ExpenseCategory.SHOPPING -> Icons.Default.ShoppingBag
        ExpenseCategory.BILLS -> Icons.Default.ReceiptLong
        ExpenseCategory.OTHER -> Icons.Default.Category
    }
}

fun getCategoryColor(categoryName: String): Color {
    return when (ExpenseCategory.fromString(categoryName)) {
        ExpenseCategory.FOOD -> CategoryFood
        ExpenseCategory.TRANSPORT -> CategoryTransport
        ExpenseCategory.SHOPPING -> CategoryShopping
        ExpenseCategory.BILLS -> CategoryBills
        ExpenseCategory.OTHER -> CategoryOther
    }
}

@Composable
fun CategoryIconBadge(
    categoryName: String,
    modifier: Modifier = Modifier,
    size: Int = 44
) {
    val color = getCategoryColor(categoryName)
    val icon = getCategoryIcon(categoryName)

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = categoryName,
            tint = color,
            modifier = Modifier.size((size * 0.52).dp)
        )
    }
}

@Composable
fun CategoryChip(
    categoryName: String,
    modifier: Modifier = Modifier
) {
    val color = getCategoryColor(categoryName)
    val icon = getCategoryIcon(categoryName)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = categoryName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

