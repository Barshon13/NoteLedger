package com.example.ui.settings

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VerifiedUser
import android.os.Build
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemePalette
import com.example.ui.components.PaletteCardItem
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.core.content.FileProvider
import com.example.data.model.Note
import com.example.ui.components.BatchNotesPdfExportDialog
import java.io.File
import java.io.FileOutputStream
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.TrackerRepository
import com.example.ui.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    notesCount: Int,
    expensesCount: Int,
    allTimeTotal: Double,
    onBackClick: () -> Unit,
    onExportJson: suspend () -> String,
    onExportCsv: suspend () -> String,
    onImportJson: (String, (TrackerRepository.ImportResult) -> Unit) -> Unit,
    onClearAllData: () -> Unit,
    notes: List<Note> = emptyList(),
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themePalette: ThemePalette = ThemePalette.SAGE_FOREST,
    dynamicColor: Boolean = false,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onThemePaletteChange: (ThemePalette) -> Unit = {},
    onDynamicColorChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showFirstClearDialog by remember { mutableStateOf(false) }
    var showSecondClearDialog by remember { mutableStateOf(false) }
    var showPasteImportDialog by remember { mutableStateOf(false) }
    var showExportPreviewDialog by remember { mutableStateOf<String?>(null) }
    var showCsvPreviewDialog by remember { mutableStateOf<String?>(null) }
    var showBatchPdfDialog by remember { mutableStateOf(false) }
    var showDrmDetailsDialog by remember { mutableStateOf(false) }
    var showActivateKeyDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showCopyrightDialog by remember { mutableStateOf(false) }
    var inputLicenseKey by remember { mutableStateOf("") }
    var pasteJsonText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val drmLicense by com.example.drm.DrmLicenseManager.licenseState.collectAsStateWithLifecycle()

    // File Picker for Export (Save JSON to file)
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = onExportJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(json.toByteArray())
                        }
                    }
                    statusMessage = "Backup successfully exported to file!"
                } catch (e: Exception) {
                    statusMessage = "Export failed: ${e.localizedMessage}"
                }
            }
        }
    }

    // File Picker for Export Expenses CSV (Save CSV to file)
    val exportCsvFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val csv = onExportCsv()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(csv.toByteArray())
                        }
                    }
                    statusMessage = "Expenses successfully exported to CSV file!"
                } catch (e: Exception) {
                    statusMessage = "CSV export failed: ${e.localizedMessage}"
                }
            }
        }
    }

    // File Picker for Import (Pick JSON file)
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            BufferedReader(InputStreamReader(inputStream)).readText()
                        } ?: ""
                    }
                    if (json.isNotBlank()) {
                        onImportJson(json) { result ->
                            statusMessage = when (result) {
                                is TrackerRepository.ImportResult.Success ->
                                    "Successfully restored ${result.notesImported} notes and ${result.expensesImported} expenses."
                                is TrackerRepository.ImportResult.Error ->
                                    "Import error: ${result.message}"
                            }
                        }
                    }
                } catch (e: Exception) {
                    statusMessage = "Failed to read backup file: ${e.localizedMessage}"
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Data Control",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Privacy & Offline Promise Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("privacy_banner_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "100% Offline & Private",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "All notes and expenses are stored strictly on your device in a local SQLite database. No accounts, no clouds, and zero tracking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Stats Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Local Storage Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(
                            title = "Notes",
                            value = notesCount.toString(),
                            icon = Icons.Default.Storage
                        )
                        StatItem(
                            title = "Expenses",
                            value = expensesCount.toString(),
                            icon = Icons.Default.ReceiptLong
                        )
                        StatItem(
                            title = "Total Spent",
                            value = DateTimeUtils.formatCurrency(allTimeTotal),
                            icon = Icons.Default.VerifiedUser
                        )
                    }
                }
            }

            // Appearance & Theme Section
            Text(
                text = "APPEARANCE & THEME",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("theme_settings_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Theme Mode",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Choose between light, dark, or system matching theme",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Theme Mode Selector Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionPill(
                            label = "System",
                            icon = Icons.Default.BrightnessAuto,
                            isSelected = themeMode == ThemeMode.SYSTEM,
                            onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_pill_system")
                        )
                        ThemeOptionPill(
                            label = "Light",
                            icon = Icons.Default.LightMode,
                            isSelected = themeMode == ThemeMode.LIGHT,
                            onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_pill_light")
                        )
                        ThemeOptionPill(
                            label = "Dark",
                            icon = Icons.Default.DarkMode,
                            isSelected = themeMode == ThemeMode.DARK,
                            onClick = { onThemeModeChange(ThemeMode.DARK) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_pill_dark")
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Color Palette",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select a handcrafted Material 3 color harmony",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        items(ThemePalette.values()) { palette ->
                            val isSelected = themePalette == palette && !dynamicColor

                            PaletteCardItem(
                                palette = palette,
                                isSelected = isSelected,
                                onClick = {
                                    if (dynamicColor) {
                                        onDynamicColorChange(false)
                                    }
                                    onThemePaletteChange(palette)
                                }
                            )
                        }
                    }

                    // Dynamic Wallpaper Colors (Android 12+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dynamic Accent Colors",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Sample colors dynamically from your device wallpaper (Material You)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = dynamicColor,
                                onCheckedChange = onDynamicColorChange,
                                modifier = Modifier.testTag("dynamic_color_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Data Backup & Restore Section
            Text(
                text = "DATA MANAGEMENT",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    SettingsActionItem(
                        icon = Icons.Default.PictureAsPdf,
                        title = "Export All Notes (PDF)",
                        subtitle = "Compile and download all saved notes into a formatted A4 PDF document",
                        onClick = {
                            showBatchPdfDialog = true
                        },
                        tag = "export_notes_pdf_button"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    SettingsActionItem(
                        icon = Icons.Default.TableChart,
                        title = "Export Expenses (CSV)",
                        subtitle = "Export current expense data to CSV spreadsheet for Excel or Google Sheets",
                        onClick = {
                            scope.launch {
                                val csv = onExportCsv()
                                showCsvPreviewDialog = csv
                            }
                        },
                        tag = "export_csv_button"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    SettingsActionItem(
                        icon = Icons.Default.FileDownload,
                        title = "Export Data Backup (JSON)",
                        subtitle = "Save all notes and expenses as a backup JSON file or copy to clipboard",
                        onClick = {
                            scope.launch {
                                val json = onExportJson()
                                showExportPreviewDialog = json
                            }
                        },
                        tag = "export_json_button"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    SettingsActionItem(
                        icon = Icons.Default.FileUpload,
                        title = "Import Data Backup (JSON)",
                        subtitle = "Restore notes and expenses from a JSON file or clipboard",
                        onClick = {
                            importFileLauncher.launch("application/json")
                        },
                        tag = "import_json_file_button"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    SettingsActionItem(
                        icon = Icons.Default.ContentPaste,
                        title = "Paste JSON from Clipboard",
                        subtitle = "Manually paste JSON backup content to restore",
                        onClick = {
                            showPasteImportDialog = true
                        },
                        tag = "paste_json_button"
                    )
                }
            }

            // Danger Zone Section
            Text(
                text = "DANGER ZONE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                SettingsActionItem(
                    icon = Icons.Default.DeleteForever,
                    iconTint = MaterialTheme.colorScheme.error,
                    title = "Clear All Data",
                    subtitle = "Permanently erase all notes and expenses from this device",
                    onClick = { showFirstClearDialog = true },
                    tag = "clear_all_data_button"
                )
            }

            // DRM & App License Section
            Text(
                text = "DRM & APP LICENSING",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drm_license_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    SettingsActionItem(
                        icon = Icons.Default.Security,
                        title = "DRM License Status",
                        subtitle = drmLicense?.status?.displayName ?: "Verified & Active",
                        onClick = { showDrmDetailsDialog = true },
                        tag = "drm_status_item"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    SettingsActionItem(
                        icon = Icons.Default.VerifiedUser,
                        title = "Verify License & Certificate",
                        subtitle = "Inspect hardware-bound DRM certificate, SHA-256 seal & signature",
                        onClick = { showDrmDetailsDialog = true },
                        tag = "drm_verify_cert_item"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    SettingsActionItem(
                        icon = Icons.Default.Lock,
                        title = "Activate Custom License Key",
                        subtitle = "Enter an offline or enterprise DRM activation code",
                        onClick = { showActivateKeyDialog = true },
                        tag = "drm_activate_key_item"
                    )
                }
            }

            // Legal, Privacy & Copyright Section
            Text(
                text = "LEGAL & PRIVACY",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("privacy_copyright_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    SettingsActionItem(
                        icon = Icons.Default.Policy,
                        title = "Privacy Policy",
                        subtitle = "100% Offline-First • Zero PII tracking • 100% Ad-Free",
                        onClick = { showPrivacyPolicyDialog = true },
                        tag = "privacy_policy_item"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    SettingsActionItem(
                        icon = Icons.Default.Info,
                        title = "Copyright & License Notice",
                        subtitle = "© 2026 Mohammad Borshon Hossain. All rights reserved.",
                        onClick = { showCopyrightDialog = true },
                        tag = "copyright_notice_item"
                    )
                }
            }

            // Amazon Fire TV & Big Screen Mode Section
            Text(
                text = "BIG SCREEN & FIRE TV",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fire_tv_settings_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Amazon Fire TV & Android TV",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enabled • D-Pad Remote Navigation",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Native support for Amazon Fire OS (Fire TV Stick, Fire TV Cube, Omni TV) and Android TV with 16:9 Navigation Rail layout and D-Pad focus ring navigation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // About Section
            Text(
                text = "ABOUT",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "NoteLedger",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version 2.00 • Purely Client-Side Offline Architecture",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Designed for high privacy with zero telemetry, zero analytics, and local persistence.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "© 2026 Mohammad Borshon Hossain. All rights reserved.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Export Options / Preview Dialog (JSON)
    showExportPreviewDialog?.let { json ->
        AlertDialog(
            onDismissRequest = { showExportPreviewDialog = null },
            modifier = Modifier.testTag("export_preview_dialog"),
            icon = {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text("Export JSON Backup")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Your backup is ready (${json.length} characters). You can save it to a file, share it, or copy it directly to your clipboard.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = json,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fileName = "offline_backup_${LocalDate.now()}.json"
                        exportFileLauncher.launch(fileName)
                        showExportPreviewDialog = null
                    }
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to File")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("NoteLedger Backup", json)
                            clipboard.setPrimaryClip(clip)
                            statusMessage = "JSON copied to clipboard!"
                            showExportPreviewDialog = null
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }

                    TextButton(onClick = { showExportPreviewDialog = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    // Export Expenses CSV Dialog
    showCsvPreviewDialog?.let { csv ->
        AlertDialog(
            onDismissRequest = { showCsvPreviewDialog = null },
            modifier = Modifier.testTag("export_csv_preview_dialog"),
            icon = {
                Icon(
                    imageVector = Icons.Default.TableChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text("Export Expenses (CSV)")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Your expense spreadsheet data is ready ($expensesCount records). Save it as a .csv file or copy it to import into Google Sheets, Excel, or Calc.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = csv,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fileName = "expenses_${LocalDate.now()}.csv"
                        exportCsvFileLauncher.launch(fileName)
                        showCsvPreviewDialog = null
                    },
                    modifier = Modifier.testTag("save_csv_file_button")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to File (.csv)")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Expenses CSV Data", csv)
                            clipboard.setPrimaryClip(clip)
                            statusMessage = "CSV data copied to clipboard!"
                            showCsvPreviewDialog = null
                        },
                        modifier = Modifier.testTag("copy_csv_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }

                    TextButton(onClick = { showCsvPreviewDialog = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    // Paste JSON Import Dialog
    if (showPasteImportDialog) {
        AlertDialog(
            onDismissRequest = { showPasteImportDialog = false },
            modifier = Modifier.testTag("paste_import_dialog"),
            title = { Text("Import from JSON") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Paste your exported JSON backup text below. This will replace the local data with the imported records.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = pasteJsonText,
                        onValueChange = { pasteJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("paste_json_textfield"),
                        placeholder = { Text("{\n  \"version\": 1,\n  \"notes\": [...],\n  \"expenses\": [...]\n}") },
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pasteJsonText.isNotBlank()) {
                            onImportJson(pasteJsonText) { result ->
                                statusMessage = when (result) {
                                    is TrackerRepository.ImportResult.Success ->
                                        "Imported ${result.notesImported} notes & ${result.expensesImported} expenses!"
                                    is TrackerRepository.ImportResult.Error ->
                                        "Import error: ${result.message}"
                                }
                            }
                            showPasteImportDialog = false
                            pasteJsonText = ""
                        }
                    },
                    modifier = Modifier.testTag("confirm_paste_import_button")
                ) {
                    Text("Import & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear All Data Confirmation 1
    if (showFirstClearDialog) {
        AlertDialog(
            onDismissRequest = { showFirstClearDialog = false },
            icon = {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("Clear All Data?") },
            text = {
                Text("Are you sure you want to delete all stored notes and expenses? This will wipe your local database.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFirstClearDialog = false
                        showSecondClearDialog = true
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFirstClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear All Data Double Confirmation 2
    if (showSecondClearDialog) {
        AlertDialog(
            onDismissRequest = { showSecondClearDialog = false },
            icon = {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("Final Confirmation") },
            text = {
                Text("This action is completely IRREVERSIBLE. All $notesCount notes and $expensesCount expenses will be permanently destroyed. Are you absolutely certain?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSecondClearDialog = false
                        onClearAllData()
                        statusMessage = "All data cleared."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_clear_all_button")
                ) {
                    Text("Yes, Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSecondClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Batch Notes PDF Export Dialog
    if (showBatchPdfDialog) {
        BatchNotesPdfExportDialog(
            notes = notes,
            onDismiss = { showBatchPdfDialog = false }
        )
    }

    // DRM License Details Dialog
    if (showDrmDetailsDialog) {
        val info = drmLicense
        AlertDialog(
            onDismissRequest = { showDrmDetailsDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "DRM & App License",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (info != null) {
                        DrmInfoRow(label = "Status", value = info.status.displayName)
                        DrmInfoRow(label = "License Token", value = info.licenseId)
                        DrmInfoRow(label = "DRM Device ID", value = info.deviceDrmId)
                        DrmInfoRow(label = "Protection Level", value = info.protectionLevel)
                        DrmInfoRow(label = "Installer Source", value = info.installerSource)
                        DrmInfoRow(label = "Validity", value = info.expiryDate)
                        DrmInfoRow(label = "SHA-256 Seal", value = info.signatureHash)
                    } else {
                        Text("License verification is initializing...")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        com.example.drm.DrmLicenseManager.refreshLicense(context)
                        statusMessage = "DRM License and Device Hardware Token successfully refreshed and verified."
                        showDrmDetailsDialog = false
                    }
                ) {
                    Text("Re-verify")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDrmDetailsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Activate Custom DRM Key Dialog
    if (showActivateKeyDialog) {
        AlertDialog(
            onDismissRequest = { showActivateKeyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text("Activate DRM License Key")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter your enterprise or offline DRM product license key (e.g. DRM-XXXX-XXXX-XXXX):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = inputLicenseKey,
                        onValueChange = { inputLicenseKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("DRM-XXXX-XXXX-XXXX") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputLicenseKey.isNotBlank()) {
                            val success = com.example.drm.DrmLicenseManager.activateCustomKey(context, inputLicenseKey)
                            if (success) {
                                statusMessage = "DRM License Key successfully activated and bound to this hardware device!"
                                showActivateKeyDialog = false
                                inputLicenseKey = ""
                            } else {
                                statusMessage = "Invalid DRM License Key format. Please check the code and try again."
                            }
                        }
                    }
                ) {
                    Text("Activate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showActivateKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyPolicyDialog) {
        val privacyPolicyFullText = """
            Privacy Policy for Notes & Expense Tracker
            Last Updated: August 2026

            1. Offline-First Privacy Architecture
            Notes & Expense Tracker operates purely client-side on your Android device. All notes, expense transactions, category budgets, and financial records are stored exclusively in your local on-device SQLite/Room database.

            2. Zero Personal Data Collection
            We do not collect, harvest, transmit, or share any Personally Identifiable Information (PII), such as your name, contacts, photos, or GPS location.

            3. Local Encrypted & Sandboxed Storage
            Your data is protected by the Android application sandbox. We do not maintain any cloud servers or external databases for your note or expense data.

            4. 100% Ad-Free & Offline Experience
            This application contains zero advertisements and zero ad-tracking SDKs. No analytics or identifiers are collected or transmitted.

            5. Developer & Contact
            Developer: Mohammad Borshon Hossain
            Email: mohammadborshonhossain6@gmail.com
            Copyright © 2026 Mohammad Borshon Hossain. All rights reserved.
        """.trimIndent()

        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Policy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = privacyPolicyFullText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Privacy Policy", privacyPolicyFullText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Privacy Policy copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Policy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacyPolicyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Copyright Notice Dialog
    if (showCopyrightDialog) {
        AlertDialog(
            onDismissRequest = { showCopyrightDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Copyright & IP Notice",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Notes & Expense Tracker",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Copyright © 2026 Mohammad Borshon Hossain.\nAll Rights Reserved.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Text(
                        text = "All design layouts, visual components, DRM systems, database schemas, code architecture, and intellectual property in this application are protected under international copyright and intellectual property laws.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Unauthorized copying, decompilation, reproduction, or redistribution of this software or any portion thereof without explicit permission is strictly prohibited.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showCopyrightDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }

    // Status Message Dialog / Toast
    statusMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { statusMessage = null },
            title = { Text("Notice") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { statusMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun StatItem(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: androidx.compose.ui.graphics.Color? = null,
    tag: String = ""
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint ?: MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun DrmInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    }
}

@Composable
fun ThemeOptionPill(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}


