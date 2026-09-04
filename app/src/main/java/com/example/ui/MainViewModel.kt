package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategorySpending
import com.example.data.model.DailySpendingTrend
import com.example.data.model.DebtRecord
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.MonthlyBudget
import com.example.data.model.Note
import com.example.data.model.RecurringExpense
import com.example.data.model.RecurringFrequency
import com.example.data.model.TrendTimeRange
import com.example.data.repository.TrackerRepository
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemePalette
import com.example.ui.theme.ThemePreferences
import com.example.ui.util.DateTimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class AppTab {
    NOTES,
    EXPENSES
}

class MainViewModel(
    private val repository: TrackerRepository,
    private val themePreferences: ThemePreferences? = null
) : ViewModel() {

    // Theme Management
    private val _themeMode = MutableStateFlow(themePreferences?.themeMode ?: ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _themePalette = MutableStateFlow(themePreferences?.themePalette ?: ThemePalette.SAGE_FOREST)
    val themePalette: StateFlow<ThemePalette> = _themePalette.asStateFlow()

    private val _dynamicColor = MutableStateFlow(themePreferences?.dynamicColor ?: false)
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _showThemeSelector = MutableStateFlow(false)
    val showThemeSelector: StateFlow<Boolean> = _showThemeSelector.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        themePreferences?.themeMode = mode
    }

    fun setThemePalette(palette: ThemePalette) {
        _themePalette.value = palette
        themePreferences?.themePalette = palette
    }

    fun setDynamicColor(enabled: Boolean) {
        _dynamicColor.value = enabled
        themePreferences?.dynamicColor = enabled
    }

    fun openThemeSelector() {
        _showThemeSelector.value = true
    }

    fun closeThemeSelector() {
        _showThemeSelector.value = false
    }

    // Tab Navigation
    private val _currentTab = MutableStateFlow(AppTab.NOTES)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // Feedback message events (Snackbars)
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // -------------------------------------------------------------
    // NOTES STATE & LOGIC
    // -------------------------------------------------------------
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val notes: StateFlow<List<Note>> = _searchQuery
        .debounce(150)
        .flatMapLatest { query ->
            repository.searchNotes(query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _editingNote = MutableStateFlow<Note?>(null)
    val editingNote: StateFlow<Note?> = _editingNote.asStateFlow()

    private val _noteEditorTitle = MutableStateFlow("")
    val noteEditorTitle: StateFlow<String> = _noteEditorTitle.asStateFlow()

    private val _noteEditorContent = MutableStateFlow("")
    val noteEditorContent: StateFlow<String> = _noteEditorContent.asStateFlow()

    private var autoSaveJob: Job? = null

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun openNewNote() {
        val newNote = Note(
            id = UUID.randomUUID().toString(),
            title = "",
            content = "",
            createdAt = Instant.now().toString(),
            updatedAt = Instant.now().toString()
        )
        _noteEditorTitle.value = ""
        _noteEditorContent.value = ""
        _editingNote.value = newNote
    }

    fun openEditNote(note: Note) {
        _editingNote.value = note
        _noteEditorTitle.value = note.title
        _noteEditorContent.value = note.content
    }

    fun onNoteTitleChange(newTitle: String) {
        _noteEditorTitle.value = newTitle
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(400)
            persistCurrentNote(newTitle, _noteEditorContent.value)
        }
    }

    fun onNoteContentChange(newContent: String) {
        _noteEditorContent.value = newContent
        // Auto-save debounce on every keystroke
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(400)
            persistCurrentNote(_noteEditorTitle.value, newContent)
        }
    }

    private suspend fun persistCurrentNote(title: String, content: String) {
        val current = _editingNote.value ?: return
        if (title.isBlank() && content.isBlank() && current.title.isBlank() && current.content.isBlank()) return

        val updated = current.copy(
            title = title,
            content = content,
            updatedAt = Instant.now().toString()
        )
        _editingNote.value = updated
        repository.insertNote(updated)
    }

    fun closeNoteEditor() {
        autoSaveJob?.cancel()
        val current = _editingNote.value
        val title = _noteEditorTitle.value
        val content = _noteEditorContent.value
        if (current != null) {
            viewModelScope.launch {
                if (title.isNotBlank() || content.isNotBlank()) {
                    val updated = current.copy(
                        title = title,
                        content = content,
                        updatedAt = Instant.now().toString()
                    )
                    repository.insertNote(updated)
                } else if (current.title.isNotBlank() || current.content.isNotBlank()) {
                    repository.deleteNoteById(current.id)
                }
                _editingNote.value = null
                _noteEditorTitle.value = ""
                _noteEditorContent.value = ""
            }
        } else {
            _editingNote.value = null
            _noteEditorTitle.value = ""
            _noteEditorContent.value = ""
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            if (_editingNote.value?.id == note.id) {
                _editingNote.value = null
                _noteEditorTitle.value = ""
                _noteEditorContent.value = ""
            }
            repository.deleteNote(note)
            _userMessage.emit("Note deleted")
        }
    }

    // -------------------------------------------------------------
    // EXPENSES STATE & LOGIC
    // -------------------------------------------------------------
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val dailyExpenses: StateFlow<List<Expense>> = _selectedDate
        .flatMapLatest { date ->
            repository.getExpensesByDate(date.toString())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val dailyTotal: StateFlow<Double> = _selectedDate
        .flatMapLatest { date ->
            repository.getDailyTotal(date.toString())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyTotal: StateFlow<Double> = _selectedDate
        .flatMapLatest { date ->
            val yearMonth = date.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            repository.getMonthlyTotal(yearMonth)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyExpenses: StateFlow<List<Expense>> = _selectedDate
        .flatMapLatest { date ->
            val yearMonth = date.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            repository.getExpensesByMonth(yearMonth)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val monthlyCategorySpending: StateFlow<List<CategorySpending>> = monthlyExpenses
        .combine(monthlyTotal) { expenses, total ->
            if (total <= 0.0 || expenses.isEmpty()) {
                emptyList()
            } else {
                ExpenseCategory.entries.mapNotNull { cat ->
                    val catExpenses = expenses.filter { it.category.equals(cat.displayName, ignoreCase = true) }
                    val catSum = catExpenses.sumOf { it.amount }
                    if (catSum > 0.0) {
                        CategorySpending(
                            category = cat.displayName,
                            amount = catSum,
                            percentage = ((catSum / total) * 100).toFloat(),
                            count = catExpenses.size
                        )
                    } else {
                        null
                    }
                }.sortedByDescending { it.amount }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // -------------------------------------------------------------
    // DAILY SPENDING TRENDS DATA VISUALIZATION (CANVAS / RECHARTS)
    // -------------------------------------------------------------
    val allExpenses: StateFlow<List<Expense>> = repository.getAllExpenses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _trendTimeRange = MutableStateFlow(TrendTimeRange.FULL_MONTH)
    val trendTimeRange: StateFlow<TrendTimeRange> = _trendTimeRange.asStateFlow()

    fun setTrendTimeRange(range: TrendTimeRange) {
        _trendTimeRange.value = range
    }

    val dailySpendingTrends: StateFlow<List<DailySpendingTrend>> = combine(
        _selectedDate,
        _trendTimeRange,
        monthlyExpenses,
        allExpenses
    ) { date, range, mExpenses, allExp ->
        when (range) {
            TrendTimeRange.FULL_MONTH -> {
                val daysInMonth = date.lengthOfMonth()
                val today = LocalDate.now()
                (1..daysInMonth).map { day ->
                    val dayDate = date.withDayOfMonth(day)
                    val dateStr = dayDate.toString()
                    val dayExpenses = mExpenses.filter { it.date == dateStr }
                    val total = dayExpenses.sumOf { it.amount }
                    val label = dayDate.format(DateTimeFormatter.ofPattern("MMM d"))
                    DailySpendingTrend(
                        date = dayDate,
                        dayOfMonth = day,
                        dayLabel = label,
                        totalAmount = total,
                        transactionCount = dayExpenses.size,
                        expenses = dayExpenses,
                        isToday = dayDate == today,
                        isFuture = dayDate.isAfter(today)
                    )
                }
            }
            TrendTimeRange.LAST_7_DAYS, TrendTimeRange.LAST_14_DAYS, TrendTimeRange.LAST_30_DAYS -> {
                val dayCount = when (range) {
                    TrendTimeRange.LAST_7_DAYS -> 7
                    TrendTimeRange.LAST_14_DAYS -> 14
                    TrendTimeRange.LAST_30_DAYS -> 30
                    else -> 7
                }
                val today = LocalDate.now()
                (0 until dayCount).reversed().map { offset ->
                    val dayDate = today.minusDays(offset.toLong())
                    val dateStr = dayDate.toString()
                    val dayExpenses = allExp.filter { it.date == dateStr }
                    val total = dayExpenses.sumOf { it.amount }
                    val label = dayDate.format(DateTimeFormatter.ofPattern("MMM d"))
                    DailySpendingTrend(
                        date = dayDate,
                        dayOfMonth = dayDate.dayOfMonth,
                        dayLabel = label,
                        totalAmount = total,
                        transactionCount = dayExpenses.size,
                        expenses = dayExpenses,
                        isToday = dayDate == today,
                        isFuture = dayDate.isAfter(today)
                    )
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // -------------------------------------------------------------
    // FINANCIAL INTELLIGENCE: BUDGETS
    // -------------------------------------------------------------
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMonthBudget: StateFlow<MonthlyBudget?> = _selectedDate
        .flatMapLatest { date ->
            val yearMonth = date.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            repository.getBudgetForMonth(yearMonth)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun saveMonthlyBudget(
        total: Double,
        food: Double,
        transport: Double,
        shopping: Double,
        bills: Double,
        other: Double
    ) {
        viewModelScope.launch {
            val yearMonth = _selectedDate.value.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val budget = MonthlyBudget(
                monthYear = yearMonth,
                totalBudget = total,
                foodBudget = food,
                transportBudget = transport,
                shoppingBudget = shopping,
                billsBudget = bills,
                otherBudget = other
            )
            repository.saveBudget(budget)
            _userMessage.emit("Budget saved for ${_selectedDate.value.format(DateTimeFormatter.ofPattern("MMMM yyyy"))}")
        }
    }

    fun clearMonthlyBudget() {
        viewModelScope.launch {
            val yearMonth = _selectedDate.value.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            repository.deleteBudget(yearMonth)
            _userMessage.emit("Budget reset")
        }
    }

    // -------------------------------------------------------------
    // FINANCIAL INTELLIGENCE: RECURRING EXPENSES & SUBSCRIPTIONS
    // -------------------------------------------------------------
    val recurringExpenses: StateFlow<List<RecurringExpense>> = repository.getAllRecurringExpenses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveRecurringExpense(
        title: String,
        amount: Double,
        category: String,
        frequency: String,
        nextDueDate: String,
        isActive: Boolean,
        note: String,
        existingId: String? = null
    ) {
        viewModelScope.launch {
            val recurring = RecurringExpense(
                id = existingId ?: UUID.randomUUID().toString(),
                title = title,
                amount = amount,
                category = category,
                frequency = frequency,
                nextDueDate = nextDueDate,
                isActive = isActive,
                note = note,
                createdAt = Instant.now().toString()
            )
            if (existingId != null) {
                repository.updateRecurringExpense(recurring)
                _userMessage.emit("Subscription updated")
            } else {
                repository.insertRecurringExpense(recurring)
                _userMessage.emit("Subscription added")
            }
        }
    }

    fun deleteRecurringExpense(expense: RecurringExpense) {
        viewModelScope.launch {
            repository.deleteRecurringExpense(expense)
            _userMessage.emit("Subscription deleted")
        }
    }

    fun toggleRecurringExpenseActive(expense: RecurringExpense) {
        viewModelScope.launch {
            val updated = expense.copy(isActive = !expense.isActive)
            repository.updateRecurringExpense(updated)
            _userMessage.emit(if (updated.isActive) "Subscription active" else "Subscription paused")
        }
    }

    fun logRecurringExpenseAsPaid(expense: RecurringExpense) {
        viewModelScope.launch {
            // 1. Post expense to today's daily log
            val newExpense = Expense(
                id = UUID.randomUUID().toString(),
                amount = expense.amount,
                category = expense.category,
                note = if (expense.note.isNotBlank()) "${expense.title} (${expense.note})" else "${expense.title} (Recurring)",
                date = LocalDate.now().toString(),
                createdAt = Instant.now().toString()
            )
            repository.insertExpense(newExpense)

            // 2. Advance next due date
            val currentDue = try {
                LocalDate.parse(expense.nextDueDate)
            } catch (e: Exception) {
                LocalDate.now()
            }
            val nextDue = when (RecurringFrequency.fromString(expense.frequency)) {
                RecurringFrequency.WEEKLY -> currentDue.plusWeeks(1)
                RecurringFrequency.MONTHLY -> currentDue.plusMonths(1)
                RecurringFrequency.YEARLY -> currentDue.plusYears(1)
            }
            val updatedRecurring = expense.copy(nextDueDate = nextDue.toString())
            repository.updateRecurringExpense(updatedRecurring)

            _userMessage.emit("Logged ${DateTimeUtils.formatCurrency(expense.amount)} for ${expense.title} to Expenses!")
        }
    }

    // -------------------------------------------------------------
    // FINANCIAL INTELLIGENCE: SPLIT BILL & DEBT TRACKER
    // -------------------------------------------------------------
    val debtRecords: StateFlow<List<DebtRecord>> = repository.getAllDebtRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveDebtRecord(
        personName: String,
        amount: Double,
        description: String,
        isOwedToMe: Boolean,
        date: String,
        existingId: String? = null
    ) {
        viewModelScope.launch {
            val debt = DebtRecord(
                id = existingId ?: UUID.randomUUID().toString(),
                personName = personName,
                amount = amount,
                description = description,
                isOwedToMe = isOwedToMe,
                isSettled = false,
                date = date,
                createdAt = Instant.now().toString()
            )
            if (existingId != null) {
                repository.updateDebtRecord(debt)
                _userMessage.emit("Record updated")
            } else {
                repository.insertDebtRecord(debt)
                _userMessage.emit("Record added")
            }
        }
    }

    fun toggleDebtSettled(debt: DebtRecord) {
        viewModelScope.launch {
            val updated = debt.copy(isSettled = !debt.isSettled)
            repository.updateDebtRecord(updated)
            _userMessage.emit(if (updated.isSettled) "Marked as Settled" else "Marked as Pending")
        }
    }

    fun deleteDebtRecord(debt: DebtRecord) {
        viewModelScope.launch {
            repository.deleteDebtRecord(debt)
            _userMessage.emit("Record deleted")
        }
    }

    // Expense Form Dialog State
    private val _showExpenseDialog = MutableStateFlow(false)
    val showExpenseDialog: StateFlow<Boolean> = _showExpenseDialog.asStateFlow()

    private val _editingExpense = MutableStateFlow<Expense?>(null)
    val editingExpense: StateFlow<Expense?> = _editingExpense.asStateFlow()

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun previousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun nextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }

    fun previousMonth() {
        _selectedDate.value = _selectedDate.value.minusMonths(1)
    }

    fun nextMonth() {
        _selectedDate.value = _selectedDate.value.plusMonths(1)
    }

    fun jumpToToday() {
        _selectedDate.value = LocalDate.now()
    }

    fun openAddExpense() {
        _editingExpense.value = null
        _showExpenseDialog.value = true
    }

    fun openEditExpense(expense: Expense) {
        _editingExpense.value = expense
        _showExpenseDialog.value = true
    }

    fun closeExpenseDialog() {
        _showExpenseDialog.value = false
        _editingExpense.value = null
    }

    fun saveExpense(
        amount: Double,
        category: String,
        note: String,
        date: LocalDate
    ) {
        viewModelScope.launch {
            val existing = _editingExpense.value
            val expense = if (existing != null) {
                existing.copy(
                    amount = amount,
                    category = category,
                    note = note,
                    date = date.toString()
                )
            } else {
                Expense(
                    id = UUID.randomUUID().toString(),
                    amount = amount,
                    category = category,
                    note = note,
                    date = date.toString(),
                    createdAt = Instant.now().toString()
                )
            }
            repository.insertExpense(expense)
            closeExpenseDialog()
            _userMessage.emit(if (existing != null) "Expense updated" else "Expense added")
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            _userMessage.emit("Expense deleted")
        }
    }

    // -------------------------------------------------------------
    // SETTINGS & DATA CONTROL
    // -------------------------------------------------------------
    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    fun openSettings() {
        _showSettings.value = true
    }

    fun closeSettings() {
        _showSettings.value = false
    }

    val notesCount: StateFlow<Int> = repository.getNotesCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val expensesCount: StateFlow<Int> = repository.getExpensesCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allTimeTotal: StateFlow<Double> = repository.getAllTimeExpenseTotal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    suspend fun getExportJson(): String {
        return repository.exportDataAsJson()
    }

    suspend fun getExportExpensesCsv(): String {
        return repository.exportExpensesAsCsv()
    }

    fun importJson(jsonString: String, onResult: (TrackerRepository.ImportResult) -> Unit) {
        viewModelScope.launch {
            val result = repository.importDataFromJson(jsonString, replaceExisting = true)
            onResult(result)
            when (result) {
                is TrackerRepository.ImportResult.Success -> {
                    _userMessage.emit("Restored ${result.notesImported} notes, ${result.expensesImported} expenses, ${result.budgetsImported} budgets, ${result.recurringImported} subscriptions, & ${result.debtsImported} debts")
                }
                is TrackerRepository.ImportResult.Error -> {
                    _userMessage.emit("Import failed: ${result.message}")
                }
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _editingNote.value = null
            _noteEditorTitle.value = ""
            _noteEditorContent.value = ""
            _userMessage.emit("All data has been wiped.")
        }
    }
}

class MainViewModelFactory(
    private val repository: TrackerRepository,
    private val themePreferences: ThemePreferences? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository, themePreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
