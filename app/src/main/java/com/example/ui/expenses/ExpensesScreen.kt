package com.example.ui.expenses

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CategorySpending
import com.example.data.model.DailySpendingTrend
import com.example.data.model.DebtRecord
import com.example.data.model.Expense
import com.example.data.model.MonthlyBudget
import com.example.data.model.RecurringExpense
import com.example.data.model.TrendTimeRange
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.util.DateTimeUtils
import com.example.ui.util.tvFocusHighlight
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class ExpensesViewMode(val title: String, val icon: ImageVector) {
    DAILY("Daily", Icons.Default.ReceiptLong),
    DASHBOARD("Analytics", Icons.Default.BarChart),
    RECURRING("Subscriptions", Icons.Default.Repeat),
    DEBTS("Split & IOU", Icons.Default.Handshake)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    selectedDate: LocalDate,
    dailyExpenses: List<Expense>,
    dailyTotal: Double,
    monthlyTotal: Double,
    monthlyExpenses: List<Expense>,
    categorySpending: List<CategorySpending>,
    currentBudget: MonthlyBudget?,
    recurringExpenses: List<RecurringExpense>,
    debtRecords: List<DebtRecord>,
    dailySpendingTrends: List<DailySpendingTrend> = emptyList(),
    trendTimeRange: TrendTimeRange = TrendTimeRange.FULL_MONTH,
    onTimeRangeChange: (TrendTimeRange) -> Unit = {},
    onSelectDate: (LocalDate) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToToday: () -> Unit,
    onOpenBudgetPlanner: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onEditExpenseClick: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onAddRecurringClick: () -> Unit,
    onEditRecurringClick: (RecurringExpense) -> Unit,
    onDeleteRecurringClick: (RecurringExpense) -> Unit,
    onToggleRecurringActive: (RecurringExpense) -> Unit,
    onLogRecurringAsPaid: (RecurringExpense) -> Unit,
    onAddDebtClick: (initialAmount: Double, initialDesc: String) -> Unit,
    onEditDebtClick: (DebtRecord) -> Unit,
    onToggleDebtSettled: (DebtRecord) -> Unit,
    onDeleteDebtClick: (DebtRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(ExpensesViewMode.DAILY) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var showDatePickerModal by remember { mutableStateOf(false) }

    val isToday = selectedDate == LocalDate.now()
    val currentMonthName = selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    val budgetTotal = currentBudget?.totalBudget ?: 0.0
    val hasBudget = budgetTotal > 0.0
    val budgetPercentage = if (hasBudget) (monthlyTotal / budgetTotal).toFloat() else 0f
    val remainingBudget = budgetTotal - monthlyTotal

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Enhanced 4-Segment Modern Tabs for Finance Management
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("expenses_view_mode_toggle_bar"),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExpensesViewMode.entries.forEach { mode ->
                        val isSelected = viewMode == mode
                        Surface(
                            modifier = Modifier
                                .tvFocusHighlight(shape = RoundedCornerShape(10.dp), onClick = { viewMode = mode })
                                .clickable { viewMode = mode }
                                .testTag("expenses_tab_${mode.name.lowercase()}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shadowElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = viewMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "expenses_view_transition"
            ) { currentMode ->
                when (currentMode) {
                    ExpensesViewMode.DASHBOARD -> {
                        ExpensesDashboardView(
                            selectedDate = selectedDate,
                            monthlyTotal = monthlyTotal,
                            monthlyExpenses = monthlyExpenses,
                            categorySpending = categorySpending,
                            currentBudget = currentBudget,
                            dailySpendingTrends = dailySpendingTrends,
                            trendTimeRange = trendTimeRange,
                            onTimeRangeChange = onTimeRangeChange,
                            onSelectDate = onSelectDate,
                            onNavigateToDailyLog = { targetDate ->
                                onSelectDate(targetDate)
                                viewMode = ExpensesViewMode.DAILY
                            },
                            onOpenBudgetPlanner = onOpenBudgetPlanner,
                            onPreviousMonth = onPreviousMonth,
                            onNextMonth = onNextMonth,
                            onJumpToToday = onJumpToToday,
                            onAddExpenseClick = onAddExpenseClick
                        )
                    }
                    ExpensesViewMode.RECURRING -> {
                        RecurringExpensesView(
                            recurringExpenses = recurringExpenses,
                            onAddRecurringClick = onAddRecurringClick,
                            onEditRecurringClick = onEditRecurringClick,
                            onDeleteRecurringClick = onDeleteRecurringClick,
                            onToggleActiveClick = onToggleRecurringActive,
                            onLogAsPaidClick = onLogRecurringAsPaid
                        )
                    }
                    ExpensesViewMode.DEBTS -> {
                        SplitAndDebtsView(
                            debtRecords = debtRecords,
                            onAddDebtClick = { onAddDebtClick(0.0, "") },
                            onEditDebtClick = onEditDebtClick,
                            onToggleSettledClick = onToggleDebtSettled,
                            onDeleteDebtClick = onDeleteDebtClick,
                            onQuickSplitSave = { amount, note -> onAddDebtClick(amount, note) }
                        )
                    }
                    ExpensesViewMode.DAILY -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Monthly Summary Overview Card with Budget Tracker
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .testTag("monthly_overview_card"),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$currentMonthName Spending",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                onClick = { viewMode = ExpensesViewMode.DASHBOARD },
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                                modifier = Modifier.testTag("trends_shortcut_button")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.BarChart,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Trends",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }

                                            Surface(
                                                onClick = onOpenBudgetPlanner,
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                                modifier = Modifier.testTag("budget_shortcut_button")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.AccountBalanceWallet,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (hasBudget) "Budget" else "+ Budget",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }

                                            if (!isToday) {
                                                Surface(
                                                    onClick = onJumpToToday,
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.surface,
                                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Today,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "Today",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = DateTimeUtils.formatCurrency(monthlyTotal),
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "total",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(bottom = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = "${monthlyExpenses.size} items",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Budget bar in daily view overview if budget is configured
                                    if (hasBudget) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        val progressColor = when {
                                            budgetPercentage >= 1.0f -> MaterialTheme.colorScheme.error
                                            budgetPercentage >= 0.8f -> Color(0xFFD97706)
                                            else -> MaterialTheme.colorScheme.primary
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (remainingBudget >= 0) "Left: ${DateTimeUtils.formatCurrency(remainingBudget)}" else "Over: ${DateTimeUtils.formatCurrency(-remainingBudget)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = progressColor
                                            )
                                            Text(
                                                text = "${(budgetPercentage * 100).toInt()}% of ${DateTimeUtils.formatCurrency(budgetTotal)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(budgetPercentage.coerceIn(0.02f, 1f))
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(progressColor)
                                            )
                                        }
                                    }
                                }
                            }

                            // Date Navigation Header
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(
                                        onClick = onPreviousDay,
                                        modifier = Modifier.testTag("prev_day_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = "Previous day",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .clickable { showDatePickerModal = true }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                            .testTag("date_picker_header_trigger"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = DateTimeUtils.formatExpenseDateHeader(selectedDate.toString()),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    IconButton(
                                        onClick = onNextDay,
                                        modifier = Modifier.testTag("next_day_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Next day",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Daily Total Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${dailyExpenses.size} ${if (dailyExpenses.size == 1) "transaction" else "transactions"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Daily Total: " + DateTimeUtils.formatCurrency(dailyTotal),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Expenses List or Empty State
                            if (dailyExpenses.isEmpty()) {
                                EmptyStateView(
                                    icon = Icons.Default.Receipt,
                                    title = "No expenses on this day",
                                    description = "Keep track of food, transport, bills, and shopping by logging your expenses.",
                                    actionLabel = "Add Expense for this Day",
                                    onActionClick = onAddExpenseClick,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("expenses_list"),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(
                                        items = dailyExpenses,
                                        key = { it.id }
                                    ) { expense ->
                                        ExpenseItemCard(
                                            expense = expense,
                                            onClick = { onEditExpenseClick(expense) },
                                            onDeleteClick = { expenseToDelete = expense }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button for Adding (shown in Daily and Analytics views)
        if (viewMode == ExpensesViewMode.DAILY || viewMode == ExpensesViewMode.DASHBOARD) {
            FloatingActionButton(
                onClick = onAddExpenseClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .tvFocusHighlight(shape = RoundedCornerShape(18.dp), onClick = onAddExpenseClick)
                    .testTag("add_expense_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
            }
        }

        // Delete Confirmation Dialog
        expenseToDelete?.let { expense ->
            DeleteConfirmDialog(
                title = "Delete Expense?",
                message = "Are you sure you want to delete this expense (${DateTimeUtils.formatCurrency(expense.amount)} for ${expense.category})?",
                confirmButtonText = "Delete",
                onConfirm = {
                    onDeleteExpense(expense)
                    expenseToDelete = null
                },
                onDismiss = {
                    expenseToDelete = null
                }
            )
        }

        // Modal Date Picker
        if (showDatePickerModal) {
            val initialMillis = remember(selectedDate) {
                selectedDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            }
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

            DatePickerDialog(
                onDismissRequest = { showDatePickerModal = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val date = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.of("UTC"))
                                    .toLocalDate()
                                onSelectDate(date)
                            }
                            showDatePickerModal = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerModal = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@Composable
fun ExpenseItemCard(
    expense: Expense,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusHighlight(shape = RoundedCornerShape(16.dp), onClick = onClick)
            .testTag("expense_item_${expense.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIconBadge(categoryName = expense.category, size = 44)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.category,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (expense.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = expense.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = DateTimeUtils.formatCurrency(expense.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("delete_expense_button_${expense.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete expense",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
