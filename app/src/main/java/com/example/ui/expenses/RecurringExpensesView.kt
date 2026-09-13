package com.example.ui.expenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RecurringExpense
import com.example.data.model.RecurringFrequency
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.util.DateTimeUtils
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun RecurringExpensesView(
    recurringExpenses: List<RecurringExpense>,
    onAddRecurringClick: () -> Unit,
    onEditRecurringClick: (RecurringExpense) -> Unit,
    onDeleteRecurringClick: (RecurringExpense) -> Unit,
    onToggleActiveClick: (RecurringExpense) -> Unit,
    onLogAsPaidClick: (RecurringExpense) -> Unit,
    modifier: Modifier = Modifier
) {
    var expenseToDelete by remember { mutableStateOf<RecurringExpense?>(null) }

    // Calculate monthly equivalent recurring cost
    val monthlyBurnRate = recurringExpenses.filter { it.isActive }.sumOf { item ->
        when (RecurringFrequency.fromString(item.frequency)) {
            RecurringFrequency.WEEKLY -> item.amount * 4.33
            RecurringFrequency.MONTHLY -> item.amount
            RecurringFrequency.YEARLY -> item.amount / 12.0
        }
    }

    val activeCount = recurringExpenses.count { it.isActive }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("recurring_expenses_view"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Burn Rate Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recurring_burn_rate_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Subscriptions & Fixed Bills",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Text(
                                text = "$activeCount Active",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = DateTimeUtils.formatCurrency(monthlyBurnRate),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "estimated monthly recurring burn rate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onAddRecurringClick,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_new_subscription_header_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add New Subscription / Bill")
                    }
                }
            }
        }

        if (recurringExpenses.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.EventRepeat,
                    title = "No recurring bills yet",
                    description = "Track recurring subscriptions like Netflix, Spotify, Gym, Rent, or WiFi bills and log them with one tap.",
                    actionLabel = "Add Subscription",
                    onActionClick = onAddRecurringClick
                )
            }
        } else {
            item {
                Text(
                    text = "All Subscriptions & Bills (${recurringExpenses.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            items(recurringExpenses, key = { it.id }) { recurring ->
                RecurringItemCard(
                    recurring = recurring,
                    onClick = { onEditRecurringClick(recurring) },
                    onDeleteClick = { expenseToDelete = recurring },
                    onToggleActiveClick = { onToggleActiveClick(recurring) },
                    onLogAsPaidClick = { onLogAsPaidClick(recurring) }
                )
            }
        }
    }

    expenseToDelete?.let { item ->
        DeleteConfirmDialog(
            title = "Delete Subscription?",
            message = "Are you sure you want to delete ${item.title} (${DateTimeUtils.formatCurrency(item.amount)} / ${item.frequency})?",
            confirmButtonText = "Delete",
            onConfirm = {
                onDeleteRecurringClick(item)
                expenseToDelete = null
            },
            onDismiss = {
                expenseToDelete = null
            }
        )
    }
}

@Composable
fun RecurringItemCard(
    recurring: RecurringExpense,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleActiveClick: () -> Unit,
    onLogAsPaidClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val dueDate = try {
        LocalDate.parse(recurring.nextDueDate)
    } catch (e: Exception) {
        today
    }

    val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate)
    val (statusLabel, statusColor, statusBg) = when {
        !recurring.isActive -> Triple("Paused", MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
        daysUntilDue < 0 -> Triple("Overdue (${-daysUntilDue}d ago)", MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.errorContainer)
        daysUntilDue == 0L -> Triple("Due Today", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
        daysUntilDue <= 3L -> Triple("Due in $daysUntilDue days", MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.tertiaryContainer)
        else -> Triple("Due ${dueDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))}", MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("recurring_card_${recurring.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (recurring.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryIconBadge(
                        categoryName = recurring.category,
                        size = 44
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recurring.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = statusBg
                            ) {
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = recurring.frequency,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = DateTimeUtils.formatCurrency(recurring.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "/ ${recurring.frequency.lowercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (recurring.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = recurring.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Toggle Active button
                    IconButton(
                        onClick = onToggleActiveClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (recurring.isActive) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = if (recurring.isActive) "Pause" else "Activate",
                            tint = if (recurring.isActive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Delete button
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Log as Paid Button
                Button(
                    onClick = onLogAsPaidClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("log_paid_button_${recurring.id}")
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log as Paid", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
