package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.ui.expenses.*
import com.example.ui.notes.*
import com.example.ui.settings.SettingsScreen
import com.example.ui.components.ThemeSelectorSheet
import com.example.ui.theme.NoteLedgerTheme
import com.example.ui.theme.ThemePreferences
import com.example.ui.SplashScreen

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

        // Initialize AdMob
        com.example.ads.AdManager.initialize(this)



        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val themePalette by viewModel.themePalette.collectAsStateWithLifecycle()
            val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
            
            var showSplash by remember { mutableStateOf(true) }

            NoteLedgerTheme(
                themeMode = themeMode,
                themePalette = themePalette,
                dynamicColor = dynamicColor
            ) {
                val emphasizedEasing = remember { CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f) }
                AnimatedContent(
                    targetState = showSplash,
                    transitionSpec = {
                        (fadeIn(tween(520, easing = emphasizedEasing)) +
                            scaleIn(initialScale = 0.96f, animationSpec = tween(560, easing = emphasizedEasing)))
                            .togetherWith(
                                fadeOut(tween(420, easing = FastOutSlowInEasing)) +
                                    scaleOut(targetScale = 1.06f, animationSpec = tween(460, easing = FastOutSlowInEasing))
                            )
                    },
                    label = "splash_transition"
                ) { isSplashing ->
                    if (isSplashing) {
                        SplashScreen(onAnimationFinished = { showSplash = false })
                    } else {
                        MainApp(
                            viewModel = viewModel
                        )
                    }
                }
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

    LaunchedEffect(Unit) {
        viewModel.adTrigger.collect {
            val activity = (context as? Activity) ?: (context as? android.content.ContextWrapper)?.baseContext as? Activity
            activity?.let {
                com.example.ads.AdManager.showInterstitial(it)
            }
        }
    }

    // Handle System Back Button
    BackHandler(enabled = editingNote != null || showSettings) {
        if (editingNote != null) {
            viewModel.closeNoteEditor()
        } else if (showSettings) {
            viewModel.closeSettings()
        }
    }

    val activeScreenKey = when {
        editingNote != null -> "editor"
        showSettings -> "settings"
        else -> "home"
    }
    val screenEasing = remember { CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f) }

    AnimatedContent(
        targetState = activeScreenKey,
        transitionSpec = {
            val isEnteringDetail = targetState != "home"
            val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(durationMillis = 360, easing = screenEasing)
            val fadeInSpec = tween<Float>(durationMillis = 280, easing = screenEasing)
            val fadeOutSpec = tween<Float>(durationMillis = 220, easing = FastOutSlowInEasing)

            if (isEnteringDetail) {
                (slideInHorizontally(animationSpec = slideSpec) { width -> (width * 0.14f).toInt() } +
                    scaleIn(initialScale = 0.97f, animationSpec = tween(360, easing = screenEasing)) +
                    fadeIn(fadeInSpec))
                    .togetherWith(
                        slideOutHorizontally(animationSpec = slideSpec) { width -> -(width * 0.06f).toInt() } +
                            scaleOut(targetScale = 0.97f, animationSpec = tween(320, easing = screenEasing)) +
                            fadeOut(fadeOutSpec)
                    )
            } else {
                (slideInHorizontally(animationSpec = slideSpec) { width -> -(width * 0.08f).toInt() } +
                    scaleIn(initialScale = 0.97f, animationSpec = tween(340, easing = screenEasing)) +
                    fadeIn(fadeInSpec))
                    .togetherWith(
                        slideOutHorizontally(animationSpec = slideSpec) { width -> (width * 0.14f).toInt() } +
                            scaleOut(targetScale = 0.96f, animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                            fadeOut(fadeOutSpec)
                    )
            }
        },
        modifier = modifier.fillMaxSize(),
        label = "screen_transition"
    ) { screenKey ->
        when (screenKey) {
            "editor" -> {
                editingNote?.let { activeNote ->
                    NoteEditorScreen(
                        note = activeNote,
                        title = noteEditorTitle,
                        content = noteEditorContent,
                        onTitleChange = viewModel::onNoteTitleChange,
                        onContentChange = viewModel::onNoteContentChange,
                        onSaveNote = viewModel::saveCurrentNote,
                        onBackClick = viewModel::closeNoteEditor,
                        onDeleteNote = viewModel::deleteNote,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            "settings" -> {
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
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                // Standard Phone / Tablet Interface
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (currentTab) {
                                    AppTab.NOTES -> "Notes"
                                    AppTab.EXPENSES -> "Expenses & Finance"
                                },
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
                        val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(durationMillis = 320, easing = screenEasing)
                        val scaleSpec = tween<Float>(durationMillis = 320, easing = screenEasing)
                        val fadeInSpec = tween<Float>(durationMillis = 260, easing = screenEasing)
                        val fadeOutSpec = tween<Float>(durationMillis = 190, easing = FastOutSlowInEasing)

                        if (isMovingRight) {
                            (slideInHorizontally(animationSpec = slideSpec) { width -> (width * 0.14f).toInt() } +
                                scaleIn(initialScale = 0.98f, animationSpec = scaleSpec) +
                                fadeIn(fadeInSpec))
                                .togetherWith(
                                    slideOutHorizontally(animationSpec = slideSpec) { width -> -(width * 0.10f).toInt() } +
                                        scaleOut(targetScale = 0.98f, animationSpec = scaleSpec) +
                                        fadeOut(fadeOutSpec)
                                )
                        } else {
                            (slideInHorizontally(animationSpec = slideSpec) { width -> -(width * 0.14f).toInt() } +
                                scaleIn(initialScale = 0.98f, animationSpec = scaleSpec) +
                                fadeIn(fadeInSpec))
                                .togetherWith(
                                    slideOutHorizontally(animationSpec = slideSpec) { width -> (width * 0.10f).toInt() } +
                                        scaleOut(targetScale = 0.98f, animationSpec = scaleSpec) +
                                        fadeOut(fadeOutSpec)
                                )
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
                                onDeleteNote = viewModel::deleteNote
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
                                onDeleteExpense = viewModel::deleteExpense,
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
    }
    }

        // Add / Edit Daily Expense Dialog
        if (showExpenseDialog) {
            ExpenseDialog(
                expenseToEdit = editingExpense,
                initialDate = selectedDate,
                onSave = viewModel::saveExpense,
                onDismiss = viewModel::closeExpenseDialog
            )
        }

        // Monthly Budget Planner Dialog
        if (showBudgetPlanner) {
            BudgetPlannerDialog(
                currentBudget = currentBudget,
                selectedDate = selectedDate,
                onSave = viewModel::saveMonthlyBudget,
                onClear = viewModel::clearMonthlyBudget,
                onDismiss = { showBudgetPlanner = false }
            )
        }

        // Recurring Expense / Subscription Dialog
        if (showRecurringDialog) {
            RecurringExpenseDialog(
                expenseToEdit = editingRecurring,
                onSave = viewModel::saveRecurringExpense,
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
                onSave = viewModel::saveDebtRecord,
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