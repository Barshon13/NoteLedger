package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppDatabase
import com.example.data.model.DebtRecord
import com.example.data.model.RecurringExpense
import com.example.data.repository.TrackerRepository
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.expenses.BudgetPlannerDialog
import com.example.ui.expenses.DebtRecordDialog
import com.example.ui.expenses.ExpenseDialog
import com.example.ui.expenses.ExpensesScreen
import com.example.ui.expenses.RecurringExpenseDialog
import com.example.ui.notes.NoteEditorScreen
import com.example.ui.notes.NotesScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.components.ThemeSelectorSheet
import com.example.ads.AdManager
import com.example.ads.BannerAdView
import com.example.ui.theme.NoteLedgerTheme
import com.example.ui.theme.ThemePalette
import com.example.ui.theme.ThemePreferences

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TrackerRepository(
            noteDao = database.noteDao(),
            expenseDao = database.expenseDao(),
            budgetDao = database.budgetDao(),
            recurringExpenseDao = database.recurringExpenseDao(),
            debtRecordDao = database.debtRecordDao()
        )
        val themePreferences = ThemePreferences(applicationContext)
        MainViewModelFactory(repository, themePreferences)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase App Check first (Play Integrity / reCAPTCHA) to protect backend and AdMob
        com.example.security.AppCheckManager.initialize(this)

        // Initialize Firebase Crashlytics with OPPO App Market environment detection
        com.example.crashlytics.CrashlyticsManager.initialize(this)

        // Initialize DRM & App License Verification
        com.example.drm.DrmLicenseManager.initialize(this)

        // Initialize Google Mobile Ads (AdMob) & Remote Configuration
        AdManager.initialize(this)
        com.example.ads.RemoteAdsConfigService.initialize(this)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val themePalette by viewModel.themePalette.collectAsStateWithLifecycle()
            val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()

            NoteLedgerTheme(
                themeMode = themeMode,
                themePalette = themePalette,
                dynamicColor = dynamicColor
            ) {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val editingNote by viewModel.editingNote.collectAsStateWithLifecycle()
    val noteEditorTitle by viewModel.noteEditorTitle.collectAsStateWithLifecycle()
    val noteEditorContent by viewModel.noteEditorContent.collectAsStateWithLifecycle()

    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val dailyExpenses by viewModel.dailyExpenses.collectAsStateWithLifecycle()
    val dailyTotal by viewModel.dailyTotal.collectAsStateWithLifecycle()
    val monthlyTotal by viewModel.monthlyTotal.collectAsStateWithLifecycle()
    val monthlyExpenses by viewModel.monthlyExpenses.collectAsStateWithLifecycle()
    val monthlyCategorySpending by viewModel.monthlyCategorySpending.collectAsStateWithLifecycle()
    val currentBudget by viewModel.currentMonthBudget.collectAsStateWithLifecycle()
    val recurringExpenses by viewModel.recurringExpenses.collectAsStateWithLifecycle()
    val debtRecords by viewModel.debtRecords.collectAsStateWithLifecycle()
    val dailySpendingTrends by viewModel.dailySpendingTrends.collectAsStateWithLifecycle()
    val trendTimeRange by viewModel.trendTimeRange.collectAsStateWithLifecycle()

    val showExpenseDialog by viewModel.showExpenseDialog.collectAsStateWithLifecycle()
    val editingExpense by viewModel.editingExpense.collectAsStateWithLifecycle()
    val showSettings by viewModel.showSettings.collectAsStateWithLifecycle()
    val showThemeSelector by viewModel.showThemeSelector.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val themePalette by viewModel.themePalette.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()

    val notesCount by viewModel.notesCount.collectAsStateWithLifecycle()
    val expensesCount by viewModel.expensesCount.collectAsStateWithLifecycle()
    val allTimeTotal by viewModel.allTimeTotal.collectAsStateWithLifecycle()

    // Local Dialog States
    var showBudgetPlanner by remember { mutableStateOf(false) }

    var showRecurringDialog by remember { mutableStateOf(false) }
    var editingRecurring by remember { mutableStateOf<RecurringExpense?>(null) }

    var showDebtDialog by remember { mutableStateOf(false) }
    var editingDebt by remember { mutableStateOf<DebtRecord?>(null) }
    var initialDebtAmount by remember { mutableDoubleStateOf(0.0) }
    var initialDebtDesc by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Handle System Back Button
    BackHandler(enabled = editingNote != null || showSettings) {
        if (editingNote != null) {
            viewModel.closeNoteEditor()
            AdManager.tryShowInterstitial(context, force = false)
        } else if (showSettings) {
            viewModel.closeSettings()
        }
    }

    // When Note Editor is open
    if (editingNote != null) {
        NoteEditorScreen(
            note = editingNote!!,
            title = noteEditorTitle,
            content = noteEditorContent,
            onTitleChange = viewModel::onNoteTitleChange,
            onContentChange = viewModel::onNoteContentChange,
            onSaveNote = {
                val act = AdManager.findActivity(context)
                if (act != null) {
                    AdManager.showInterstitialAd(act) {
                        viewModel.saveCurrentNote()
                    }
                } else {
                    viewModel.saveCurrentNote()
                }
            },
            onBackClick = {
                viewModel.closeNoteEditor()
            },
            onDeleteNote = { note ->
                val act = AdManager.findActivity(context)
                if (act != null) {
                    AdManager.showInterstitialAd(act) {
                        viewModel.deleteNote(note)
                    }
                } else {
                    viewModel.deleteNote(note)
                }
            },
            modifier = modifier
        )
    } else if (showSettings) {
        val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
        val themePalette by viewModel.themePalette.collectAsStateWithLifecycle()
        val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()

        SettingsScreen(
            notesCount = notesCount,
            expensesCount = expensesCount,
            allTimeTotal = allTimeTotal,
            onBackClick = viewModel::closeSettings,
            onClearAllData = viewModel::clearAllData,
            themeMode = themeMode,
            themePalette = themePalette,
            dynamicColor = dynamicColor,
            onThemeModeChange = viewModel::setThemeMode,
            onThemePaletteChange = viewModel::setThemePalette,
            onDynamicColorChange = viewModel::setDynamicColor,
            modifier = modifier
        )
    } else {
        // Standard Phone / Tablet Interface
        Scaffold(
                modifier = modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (currentTab == AppTab.NOTES) "Notes" else "Expenses & Finance",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = viewModel::openThemeSelector,
                                modifier = Modifier.testTag("theme_palette_top_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Adjust Theme Colors",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = viewModel::openSettings,
                                modifier = Modifier.testTag("settings_top_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                bottomBar = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        BannerAdView(modifier = Modifier.fillMaxWidth())
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 0.dp,
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = currentTab == AppTab.NOTES,
                                onClick = {
                                    if (currentTab != AppTab.NOTES) {
                                        viewModel.selectTab(AppTab.NOTES)
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == AppTab.NOTES) Icons.Default.EditNote else Icons.Default.Description,
                                        contentDescription = "Notes"
                                    )
                                },
                                label = { Text("Notes") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("notes_tab_item")
                            )

                            NavigationBarItem(
                                selected = currentTab == AppTab.EXPENSES,
                                onClick = {
                                    if (currentTab != AppTab.EXPENSES) {
                                        viewModel.selectTab(AppTab.EXPENSES)
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == AppTab.EXPENSES) Icons.Default.AccountBalanceWallet else Icons.Default.ReceiptLong,
                                        contentDescription = "Expenses"
                                    )
                                },
                                label = { Text("Expenses") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("expenses_tab_item")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        val isMovingRight = targetState.ordinal > initialState.ordinal
                        val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(durationMillis = 280, easing = FastOutSlowInEasing)
                        val fadeSpec = tween<Float>(durationMillis = 220)

                        if (isMovingRight) {
                            (slideInHorizontally(animationSpec = slideSpec) { width -> (width * 0.18f).toInt() } + fadeIn(fadeSpec))
                                .togetherWith(slideOutHorizontally(animationSpec = slideSpec) { width -> -(width * 0.18f).toInt() } + fadeOut(fadeSpec))
                        } else {
                            (slideInHorizontally(animationSpec = slideSpec) { width -> -(width * 0.18f).toInt() } + fadeIn(fadeSpec))
                                .togetherWith(slideOutHorizontally(animationSpec = slideSpec) { width -> (width * 0.18f).toInt() } + fadeOut(fadeSpec))
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    label = "tab_transition"
                ) { tab ->
                    when (tab) {
                        AppTab.NOTES -> {
                            NotesScreen(
                                notes = notes,
                                searchQuery = searchQuery,
                                onSearchQueryChange = viewModel::onSearchQueryChange,
                                onNoteClick = viewModel::openEditNote,
                                onAddNoteClick = viewModel::openNewNote,
                                onDeleteNote = { note ->
                                    val act = AdManager.findActivity(context)
                                    if (act != null) {
                                        AdManager.showInterstitialAd(act) {
                                            viewModel.deleteNote(note)
                                        }
                                    } else {
                                        viewModel.deleteNote(note)
                                    }
                                }
                            )
                        }
                        AppTab.EXPENSES -> {
                            ExpensesScreen(
                                selectedDate = selectedDate,
                                dailyExpenses = dailyExpenses,
                                dailyTotal = dailyTotal,
                                monthlyTotal = monthlyTotal,
                                monthlyExpenses = monthlyExpenses,
                                categorySpending = monthlyCategorySpending,
                                currentBudget = currentBudget,
                                recurringExpenses = recurringExpenses,
                                debtRecords = debtRecords,
                                dailySpendingTrends = dailySpendingTrends,
                                trendTimeRange = trendTimeRange,
                                onTimeRangeChange = viewModel::setTrendTimeRange,
                                onSelectDate = viewModel::selectDate,
                                onPreviousDay = viewModel::previousDay,
                                onNextDay = viewModel::nextDay,
                                onPreviousMonth = viewModel::previousMonth,
                                onNextMonth = viewModel::nextMonth,
                                onJumpToToday = viewModel::jumpToToday,
                                onOpenBudgetPlanner = { showBudgetPlanner = true },
                                onAddExpenseClick = viewModel::openAddExpense,
                                onEditExpenseClick = viewModel::openEditExpense,
                                onDeleteExpense = { expense ->
                                    val act = AdManager.findActivity(context)
                                    if (act != null) {
                                        AdManager.showInterstitialAd(act) {
                                            viewModel.deleteExpense(expense)
                                        }
                                    } else {
                                        viewModel.deleteExpense(expense)
                                    }
                                },
                                onAddRecurringClick = {
                                    editingRecurring = null
                                    showRecurringDialog = true
                                },
                                onEditRecurringClick = { recurring ->
                                    editingRecurring = recurring
                                    showRecurringDialog = true
                                },
                                onDeleteRecurringClick = viewModel::deleteRecurringExpense,
                                onToggleRecurringActive = viewModel::toggleRecurringExpenseActive,
                                onLogRecurringAsPaid = viewModel::logRecurringExpenseAsPaid,
                                onAddDebtClick = { amount, desc ->
                                    editingDebt = null
                                    initialDebtAmount = amount
                                    initialDebtDesc = desc
                                    showDebtDialog = true
                                },
                                onEditDebtClick = { debt ->
                                    editingDebt = debt
                                    initialDebtAmount = 0.0
                                    initialDebtDesc = ""
                                    showDebtDialog = true
                                },
                                onToggleDebtSettled = viewModel::toggleDebtSettled,
                                onDeleteDebtClick = viewModel::deleteDebtRecord
                            )
                        }
                    }
                }
            }
        }

        // Add / Edit Daily Expense Dialog
        if (showExpenseDialog) {
            ExpenseDialog(
                expenseToEdit = editingExpense,
                initialDate = selectedDate,
                onSave = { amount, category, note, date ->
                    val act = AdManager.findActivity(context)
                    if (act != null) {
                        AdManager.showInterstitialAd(act) {
                            viewModel.saveExpense(amount, category, note, date)
                        }
                    } else {
                        viewModel.saveExpense(amount, category, note, date)
                    }
                },
                onDismiss = viewModel::closeExpenseDialog
            )
        }

        // Monthly Budget Planner Dialog
        if (showBudgetPlanner) {
            BudgetPlannerDialog(
                currentBudget = currentBudget,
                selectedDate = selectedDate,
                onSave = { total, food, transport, shopping, bills, other ->
                    viewModel.saveMonthlyBudget(total, food, transport, shopping, bills, other)
                    AdManager.tryShowInterstitial(context, force = false)
                },
                onClear = viewModel::clearMonthlyBudget,
                onDismiss = { showBudgetPlanner = false }
            )
        }

        // Recurring Expense / Subscription Dialog
        if (showRecurringDialog) {
            RecurringExpenseDialog(
                expenseToEdit = editingRecurring,
                onSave = { title, amount, category, frequency, nextDueDate, isActive, note, existingId ->
                    viewModel.saveRecurringExpense(title, amount, category, frequency, nextDueDate, isActive, note, existingId)
                    AdManager.tryShowInterstitial(context, force = false)
                },
                onDismiss = {
                    showRecurringDialog = false
                    editingRecurring = null
                }
            )
        }

        // Debt / Split Bill Record Dialog
        if (showDebtDialog) {
            DebtRecordDialog(
                debtToEdit = editingDebt,
                initialAmount = initialDebtAmount,
                initialDescription = initialDebtDesc,
                onSave = { personName, amount, description, isOwedToMe, date, existingId ->
                    viewModel.saveDebtRecord(personName, amount, description, isOwedToMe, date, existingId)
                    AdManager.tryShowInterstitial(context, force = false)
                },
                onDismiss = {
                    showDebtDialog = false
                    editingDebt = null
                    initialDebtAmount = 0.0
                    initialDebtDesc = ""
                }
            )
        }

        // Theme & Color Adjustment Bottom Sheet
        if (showThemeSelector) {
            ThemeSelectorSheet(
                themeMode = themeMode,
                currentPalette = themePalette,
                dynamicColor = dynamicColor,
                onThemeModeChange = viewModel::setThemeMode,
                onPaletteChange = viewModel::setThemePalette,
                onDynamicColorChange = viewModel::setDynamicColor,
                onDismiss = viewModel::closeThemeSelector
            )
        }
    }