package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.ThemePalette
import com.example.ui.theme.ThemePreferences
import com.example.ui.SplashScreen
import com.example.ui.coinstore.*
import android.widget.Toast
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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

    private val billingManager by lazy { com.example.billing.BillingManager(applicationContext, lifecycleScope) }
    private val userBalanceDao by lazy { AppDatabase.getDatabase(applicationContext).userBalanceDao() }
    private val purchaseDao by lazy { AppDatabase.getDatabase(applicationContext).purchaseDao() }
    
    private val coinStoreViewModel by viewModels<com.example.ui.coinstore.CoinStoreViewModel> {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return com.example.ui.coinstore.CoinStoreViewModel(userBalanceDao, purchaseDao, billingManager) as T
            }
        }
    }

    private val purchaseHistoryViewModel by viewModels<com.example.ui.coinstore.PurchaseHistoryViewModel> {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return com.example.ui.coinstore.PurchaseHistoryViewModel(purchaseDao) as T
            }
        }
    }
    private val driveBackupManager by lazy { com.example.backup.DriveBackupManager(applicationContext) }

    private val googleSignInClient by lazy {
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_APPDATA))
            .build()
        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso)
    }

    private var pendingBackupAction: ((com.google.android.gms.auth.api.signin.GoogleSignInAccount) -> Unit)? = null

    private val signInLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                if (account != null) {
                    pendingBackupAction?.invoke(account)
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Sign in failed", e)
                Toast.makeText(this, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        pendingBackupAction = null
    }

    private fun performDriveAction(action: (com.google.android.gms.auth.api.signin.GoogleSignInAccount) -> Unit) {
        val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(this)
        if (account != null && account.grantedScopes.contains(com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_APPDATA))) {
            action(account)
        } else {
            pendingBackupAction = action
            signInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun backupToDrive() {
        performDriveAction { account ->
            lifecycleScope.launch {
                Toast.makeText(this@MainActivity, "Backing up to Google Drive...", Toast.LENGTH_SHORT).show()
                val result = driveBackupManager.backupData(account)
                if (result.isSuccess) {
                    Toast.makeText(this@MainActivity, "Backup Successful!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "Backup Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun restoreFromDrive() {
        performDriveAction { account ->
            lifecycleScope.launch {
                Toast.makeText(this@MainActivity, "Restoring from Google Drive...", Toast.LENGTH_SHORT).show()
                val result = driveBackupManager.restoreData(account)
                if (result.isSuccess) {
                    Toast.makeText(this@MainActivity, "Restore Successful! Data synced.", Toast.LENGTH_LONG).show()
                    // Re-launch or refresh UI might be needed depending on implementation
                } else {
                    Toast.makeText(this@MainActivity, "Restore Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private val noteDetailViewModel by viewModels<com.example.ui.notes.NoteDetailViewModel> {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return com.example.ui.notes.NoteDetailViewModel(userBalanceDao) as T
            }
        }
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
                androidx.compose.animation.Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(800),
                    label = "splash_transition"
                ) { isSplashing ->
                    if (isSplashing) {
                        SplashScreen(onAnimationFinished = { showSplash = false })
                    } else {
                        MainApp(
                            viewModel = viewModel,
                            coinStoreViewModel = coinStoreViewModel,
                            noteDetailViewModel = noteDetailViewModel,
                            purchaseHistoryViewModel = purchaseHistoryViewModel,
                            onBackupClick = ::backupToDrive,
                            onRestoreClick = ::restoreFromDrive
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
    coinStoreViewModel: CoinStoreViewModel,
    noteDetailViewModel: NoteDetailViewModel,
    purchaseHistoryViewModel: com.example.ui.coinstore.PurchaseHistoryViewModel,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
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
    val sheetState = rememberModalBottomSheetState()
    var showQuotaSheet by remember { mutableStateOf(false) }

    LaunchedEffect(noteDetailViewModel.saveResult) {
        noteDetailViewModel.saveResult.collect { result ->
            if (result is com.example.ui.notes.NoteSaveResult.OutOfQuota) {
                showQuotaSheet = true
            }
        }
    }

    if (showQuotaSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuotaSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF0F241D),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CurrencyExchange,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Out of Note Coins!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "You've used your 10 free notes. Top up coins in the Coin Store to keep writing seamlessly.",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        showQuotaSheet = false
                        viewModel.selectTab(com.example.ui.AppTab.COIN_STORE)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                ) {
                    Text("Go to Coin Store", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var showPurchaseHistory by remember { mutableStateOf(false) }

    // Handle System Back Button
    BackHandler(enabled = editingNote != null || showSettings || showPurchaseHistory) {
        if (editingNote != null) {
            viewModel.closeNoteEditor()
        } else if (showSettings) {
            viewModel.closeSettings()
        } else if (showPurchaseHistory) {
            showPurchaseHistory = false
        }
    }

    if (showPurchaseHistory) {
        com.example.ui.coinstore.PurchaseHistoryScreen(
            viewModel = purchaseHistoryViewModel,
            onBack = { showPurchaseHistory = false }
        )
    } else if (editingNote != null) {
        val scope = rememberCoroutineScope()
        NoteEditorScreen(
            note = editingNote!!,
            title = noteEditorTitle,
            content = noteEditorContent,
            onTitleChange = viewModel::onNoteTitleChange,
            onContentChange = viewModel::onNoteContentChange,
            onSaveNote = {
                noteDetailViewModel.attemptSaveNote {
                    viewModel.saveCurrentNote()
                    val balance = noteDetailViewModel.userBalance.value
                    if (balance != null && balance.freeNotesRemaining == 0) {
                        Toast.makeText(context, "+1 Note Saved (-1 Coin)", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onBackClick = viewModel::closeNoteEditor,
            onDeleteNote = viewModel::deleteNote,
            modifier = modifier
        )
    }
 else if (showSettings) {
        val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
        val themePalette by viewModel.themePalette.collectAsStateWithLifecycle()
        val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()

        SettingsScreen(
            notesCount = notesCount,
            expensesCount = expensesCount,
            allTimeTotal = allTimeTotal,
            onBackClick = viewModel::closeSettings,
            onClearAllData = viewModel::clearAllData,
            onBackupClick = onBackupClick,
            onRestoreClick = onRestoreClick,
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
                                text = when (currentTab) {
                                    AppTab.NOTES -> "Notes"
                                    AppTab.EXPENSES -> "Expenses & Finance"
                                    AppTab.COIN_STORE -> "Coin Store"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        actions = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                val storeUiState by coinStoreViewModel.uiState.collectAsStateWithLifecycle()
                                Text(
                                    text = String.format("%.2f", storeUiState.userBalance.coinBalance),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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

                            NavigationBarItem(
                                selected = currentTab == AppTab.COIN_STORE,
                                onClick = {
                                    if (currentTab != AppTab.COIN_STORE) {
                                        viewModel.selectTab(AppTab.COIN_STORE)
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = "Coin Store"
                                    )
                                },
                                label = { Text("Store") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("coin_store_tab_item")
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
                        AppTab.COIN_STORE -> {
                            CoinStoreScreen(
                                viewModel = coinStoreViewModel,
                                onHistoryClick = { showPurchaseHistory = true }
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