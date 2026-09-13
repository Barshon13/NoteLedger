package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Note
import com.example.ui.util.DateTimeUtils
import com.example.ui.util.NotePdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.io.File

@Composable
fun NotePdfExportDialog(
    note: Note,
    currentTitle: String = note.title,
    currentContent: String = note.content,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val displayTitle = if (currentTitle.isNotBlank()) currentTitle.trim() else note.displayTitle
    val totalText = "$currentTitle $currentContent".trim()
    val words = if (totalText.isBlank()) 0 else totalText.split("\\s+".toRegex()).size
    val chars = totalText.length

    val sanitizedName = displayTitle
        .replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        .take(25)
        .ifBlank { "Note" }
    val defaultPdfFileName = "${sanitizedName}_${LocalDate.now()}.pdf"

    // Storage Access Framework Picker to Save PDF to Device
    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            NotePdfGenerator.generateSingleNotePdf(
                                context = context,
                                note = note,
                                title = currentTitle,
                                content = currentContent,
                                outputStream = outputStream
                            )
                        }
                    }
                    Toast.makeText(context, "PDF saved successfully to device!", Toast.LENGTH_LONG).show()
                    onDismiss()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to save PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("note_pdf_export_dialog"),
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF Export",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "Download Note as PDF",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Generate a formatted PDF document with timestamps, metadata, and typography.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Note Info Summary Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Last updated: " + DateTimeUtils.formatNoteTimestamp(note.updatedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$words words • $chars characters",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Action Buttons inside Dialog
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Save to Device Storage (Primary)
                    Button(
                        onClick = {
                            savePdfLauncher.launch(defaultPdfFileName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_pdf_storage_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save PDF to Device")
                    }

                    // 2. Open / Print Preview
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val tempFile = withContext(Dispatchers.IO) {
                                        NotePdfGenerator.createTempNotePdfFile(
                                            context = context,
                                            note = note,
                                            title = currentTitle,
                                            content = currentContent
                                        )
                                    }
                                    NotePdfGenerator.openPdfFile(context, tempFile)
                                    onDismiss()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error opening PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_pdf_preview_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open / Print PDF")
                    }

                    // 3. Share via Apps
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val tempFile = withContext(Dispatchers.IO) {
                                        NotePdfGenerator.createTempNotePdfFile(
                                            context = context,
                                            note = note,
                                            title = currentTitle,
                                            content = currentContent
                                        )
                                    }
                                    NotePdfGenerator.sharePdfFile(context, tempFile, "$displayTitle (PDF)")
                                    onDismiss()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error sharing PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_pdf_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share PDF to Apps")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_pdf_dialog_button")
            ) {
                Text("Close")
            }
        }
    )
}
