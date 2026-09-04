package com.example.ui.expenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.ExpenseCategory
import com.example.data.model.MonthlyBudget
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.getCategoryColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun BudgetPlannerDialog(
    currentBudget: MonthlyBudget?,
    selectedDate: LocalDate,
    onSave: (total: Double, food: Double, transport: Double, shopping: Double, bills: Double, other: Double) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val monthName = selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

    var totalText by remember {
        mutableStateOf(if ((currentBudget?.totalBudget ?: 0.0) > 0.0) String.format("%.0f", currentBudget!!.totalBudget) else "")
    }
    var foodText by remember {
        mutableStateOf(if ((currentBudget?.foodBudget ?: 0.0) > 0.0) String.format("%.0f", currentBudget!!.foodBudget) else "")
    }
    var transportText by remember {
        mutableStateOf(if ((currentBudget?.transportBudget ?: 0.0) > 0.0) String.format("%.0f", currentBudget!!.transportBudget) else "")
    }
    var shoppingText by remember {
        mutableStateOf(if ((currentBudget?.shoppingBudget ?: 0.0) > 0.0) String.format("%.0f", currentBudget!!.shoppingBudget) else "")
    }
    var billsText by remember {
        mutableStateOf(if ((currentBudget?.billsBudget ?: 0.0) > 0.0) String.format("%.0f", currentBudget!!.billsBudget) else "")
    }
    var otherText by remember {
        mutableStateOf(if ((currentBudget?.otherBudget ?: 0.0) > 0.0) String.format("%.0f", currentBudget!!.otherBudget) else "")
    }

    var showCategoryBreakdown by remember {
        mutableStateOf(
            (currentBudget?.foodBudget ?: 0.0) > 0.0 ||
            (currentBudget?.transportBudget ?: 0.0) > 0.0 ||
            (currentBudget?.shoppingBudget ?: 0.0) > 0.0 ||
            (currentBudget?.billsBudget ?: 0.0) > 0.0 ||
            (currentBudget?.otherBudget ?: 0.0) > 0.0
        )
    }

    val quickPresets = listOf(5000, 10000, 20000, 35000, 50000)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Budget Plan: $monthName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Set monthly target spending limits to stay on track and get visual alert warnings when nearing limits.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Total Monthly Budget Input
                OutlinedTextField(
                    value = totalText,
                    onValueChange = { totalText = it },
                    label = { Text("Total Monthly Budget (৳)") },
                    placeholder = { Text("e.g. 25000") },
                    leadingIcon = {
                        Text(
                            text = "৳",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_total_input")
                )

                // Quick Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPresets.take(4).forEach { preset ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { totalText = preset.toString() }
                                .testTag("budget_preset_$preset"),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = "৳${preset / 1000}k",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Toggle Category Breakdown
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCategoryBreakdown = !showCategoryBreakdown }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Category Limits (Optional)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (showCategoryBreakdown) "Hide ▲" else "Expand ▼",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (showCategoryBreakdown) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CategoryBudgetRow(
                            category = ExpenseCategory.FOOD.displayName,
                            value = foodText,
                            onValueChange = { foodText = it }
                        )
                        CategoryBudgetRow(
                            category = ExpenseCategory.TRANSPORT.displayName,
                            value = transportText,
                            onValueChange = { transportText = it }
                        )
                        CategoryBudgetRow(
                            category = ExpenseCategory.SHOPPING.displayName,
                            value = shoppingText,
                            onValueChange = { shoppingText = it }
                        )
                        CategoryBudgetRow(
                            category = ExpenseCategory.BILLS.displayName,
                            value = billsText,
                            onValueChange = { billsText = it }
                        )
                        CategoryBudgetRow(
                            category = ExpenseCategory.OTHER.displayName,
                            value = otherText,
                            onValueChange = { otherText = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val total = totalText.toDoubleOrNull() ?: 0.0
                    val food = foodText.toDoubleOrNull() ?: 0.0
                    val transport = transportText.toDoubleOrNull() ?: 0.0
                    val shopping = shoppingText.toDoubleOrNull() ?: 0.0
                    val bills = billsText.toDoubleOrNull() ?: 0.0
                    val other = otherText.toDoubleOrNull() ?: 0.0

                    // If total is 0 but category budgets are entered, calculate total automatically
                    val finalTotal = if (total <= 0.0 && (food + transport + shopping + bills + other) > 0.0) {
                        food + transport + shopping + bills + other
                    } else {
                        total
                    }

                    onSave(finalTotal, food, transport, shopping, bills, other)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_budget_button")
            ) {
                Text("Save Budget")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (currentBudget != null && currentBudget.totalBudget > 0.0) {
                    TextButton(
                        onClick = {
                            onClear()
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reset")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun CategoryBudgetRow(
    category: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIconBadge(categoryName = category, size = 36)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = category,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(80.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("0") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("budget_input_${category.lowercase()}")
        )
    }
}
