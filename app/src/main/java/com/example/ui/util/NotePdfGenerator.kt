package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.model.Note
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object NotePdfGenerator {

    // Standard A4 dimensions in PostScript points (72 points/inch)
    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842
    private const val MARGIN_HORIZONTAL = 45f
    private const val MARGIN_TOP = 45f
    private const val MARGIN_BOTTOM = 50f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_HORIZONTAL * 2)

    /**
     * Generates a beautifully formatted PDF for a single note into the given OutputStream.
     */
    fun generateSingleNotePdf(
        context: Context,
        note: Note,
        title: String,
        content: String,
        outputStream: OutputStream
    ) {
        val pdfDocument = PdfDocument()
        val printableTitle = if (title.isNotBlank()) title.trim() else note.displayTitle
        val printableContent = if (content.isNotBlank()) content.trim() else "No content available."

        val words = if (printableContent.isBlank()) 0 else printableContent.split("\\s+".toRegex()).size
        val chars = printableContent.length

        // Paints
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#386A20") // Theme Primary Sage Green
            style = Paint.Style.FILL
        }

        val headerTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            color = android.graphics.Color.parseColor("#5A6B56")
            isFakeBoldText = true
            letterSpacing = 0.08f
        }

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 20f
            color = android.graphics.Color.parseColor("#1C1B1F")
            isFakeBoldText = true
        }

        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9.5f
            color = android.graphics.Color.parseColor("#444746")
        }

        val contentPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            color = android.graphics.Color.parseColor("#1D1B16")
        }

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#E0E3DF")
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8.5f
            color = android.graphics.Color.parseColor("#747775")
        }

        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#F3F5F1")
            style = Paint.Style.FILL
        }

        // Build Title layout
        val titleLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(printableTitle, 0, printableTitle.length, titlePaint, CONTENT_WIDTH.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.15f)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(printableTitle, titlePaint, CONTENT_WIDTH.toInt(), Layout.Alignment.ALIGN_NORMAL, 1.15f, 0f, true)
        }

        // Build Content layout
        val contentLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(printableContent, 0, printableContent.length, contentPaint, CONTENT_WIDTH.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(3f, 1.25f)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(printableContent, contentPaint, CONTENT_WIDTH.toInt(), Layout.Alignment.ALIGN_NORMAL, 1.25f, 3f, true)
        }

        // Calculate pages needed for content
        val pageAvailableContentHeight = PAGE_HEIGHT - MARGIN_TOP - MARGIN_BOTTOM - 20f
        // First page has header, title, and metadata box
        val firstPageHeaderHeight = 140f + titleLayout.height
        val firstPageAvailableContent = (PAGE_HEIGHT - MARGIN_TOP - MARGIN_BOTTOM - firstPageHeaderHeight).coerceAtLeast(100f)

        // Split lines into pages
        var currentLineIndex = 0
        val totalLines = contentLayout.lineCount
        var pageNumber = 1

        while (currentLineIndex < totalLines || pageNumber == 1) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var currentY = MARGIN_TOP

            // Draw Top Decorative Primary Bar
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 6f, brandPaint)

            // Header Banner
            canvas.drawText("NOTES & EXPENSES", MARGIN_HORIZONTAL, currentY + 12f, headerTextPaint)
            val versionTag = "v2.00 • OFFLINE SECURE EXPORT"
            val versionTagWidth = headerTextPaint.measureText(versionTag)
            canvas.drawText(versionTag, PAGE_WIDTH - MARGIN_HORIZONTAL - versionTagWidth, currentY + 12f, headerTextPaint)

            currentY += 24f
            canvas.drawLine(MARGIN_HORIZONTAL, currentY, PAGE_WIDTH - MARGIN_HORIZONTAL, currentY, dividerPaint)
            currentY += 16f

            if (pageNumber == 1) {
                // Draw Note Title
                canvas.save()
                canvas.translate(MARGIN_HORIZONTAL, currentY)
                titleLayout.draw(canvas)
                canvas.restore()
                currentY += titleLayout.height + 14f

                // Draw Metadata Card
                val metaCardRect = RectF(MARGIN_HORIZONTAL, currentY, PAGE_WIDTH - MARGIN_HORIZONTAL, currentY + 44f)
                canvas.drawRoundRect(metaCardRect, 8f, 8f, cardBgPaint)

                val dateFormatted = DateTimeUtils.formatNoteTimestamp(note.updatedAt)
                val metaLeft = "Updated: $dateFormatted"
                val metaRight = "Stats: $words words • $chars characters"

                canvas.drawText(metaLeft, MARGIN_HORIZONTAL + 12f, currentY + 26f, metaPaint)
                val metaRightWidth = metaPaint.measureText(metaRight)
                canvas.drawText(metaRight, PAGE_WIDTH - MARGIN_HORIZONTAL - 12f - metaRightWidth, currentY + 26f, metaPaint)

                currentY += 44f + 16f
                canvas.drawLine(MARGIN_HORIZONTAL, currentY, PAGE_WIDTH - MARGIN_HORIZONTAL, currentY, dividerPaint)
                currentY += 16f
            }

            // Draw content lines for this page
            val availableHeightForThisPage = PAGE_HEIGHT - MARGIN_BOTTOM - currentY - 20f
            val startLineForPage = currentLineIndex
            var endLineForPage = currentLineIndex

            var accumulatedHeight = 0f
            while (endLineForPage < totalLines) {
                val lineHeight = contentLayout.getLineBottom(endLineForPage) - contentLayout.getLineTop(endLineForPage)
                if (accumulatedHeight + lineHeight > availableHeightForThisPage && endLineForPage > startLineForPage) {
                    break
                }
                accumulatedHeight += lineHeight
                endLineForPage++
            }

            // Draw slice of content
            if (startLineForPage < totalLines) {
                val clipTop = contentLayout.getLineTop(startLineForPage)
                val clipBottom = if (endLineForPage < totalLines) contentLayout.getLineBottom(endLineForPage - 1) else contentLayout.height

                canvas.save()
                canvas.clipRect(MARGIN_HORIZONTAL, currentY, PAGE_WIDTH - MARGIN_HORIZONTAL, currentY + (clipBottom - clipTop))
                canvas.translate(MARGIN_HORIZONTAL, currentY - clipTop)
                contentLayout.draw(canvas)
                canvas.restore()
            }

            currentLineIndex = endLineForPage

            // Draw Page Footer
            val footerY = PAGE_HEIGHT - MARGIN_BOTTOM + 20f
            canvas.drawLine(MARGIN_HORIZONTAL, footerY - 10f, PAGE_WIDTH - MARGIN_HORIZONTAL, footerY - 10f, dividerPaint)
            canvas.drawText("Stored 100% locally on device", MARGIN_HORIZONTAL, footerY + 6f, footerPaint)

            val pageLabel = "Page $pageNumber"
            val pageLabelWidth = footerPaint.measureText(pageLabel)
            canvas.drawText(pageLabel, PAGE_WIDTH - MARGIN_HORIZONTAL - pageLabelWidth, footerY + 6f, footerPaint)

            pdfDocument.finishPage(page)
            pageNumber++

            if (currentLineIndex >= totalLines) {
                break
            }
        }

        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }

    /**
     * Generates a batch PDF containing all notes in one document.
     */
    fun generateNotesBatchPdf(
        context: Context,
        notes: List<Note>,
        outputStream: OutputStream
    ) {
        val pdfDocument = PdfDocument()

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#386A20")
            style = Paint.Style.FILL
        }

        val headerTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            color = android.graphics.Color.parseColor("#5A6B56")
            isFakeBoldText = true
        }

        val mainTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 22f
            color = android.graphics.Color.parseColor("#1C1B1F")
            isFakeBoldText = true
        }

        val noteTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 15f
            color = android.graphics.Color.parseColor("#1C1B1F")
            isFakeBoldText = true
        }

        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9.5f
            color = android.graphics.Color.parseColor("#5A6B56")
        }

        val contentPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            color = android.graphics.Color.parseColor("#1D1B16")
        }

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#E0E3DF")
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8.5f
            color = android.graphics.Color.parseColor("#747775")
        }

        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#F3F5F1")
            style = Paint.Style.FILL
        }

        // Render notes across pages
        var pageNumber = 1
        var noteIndex = 0

        while (noteIndex < notes.size) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var currentY = MARGIN_TOP

            // Draw Top Accent
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 6f, brandPaint)

            // Header Banner
            canvas.drawText("NOTES ARCHIVE EXPORT", MARGIN_HORIZONTAL, currentY + 12f, headerTextPaint)
            val exportDate = "Generated on ${LocalDate.now()}"
            val exportDateWidth = headerTextPaint.measureText(exportDate)
            canvas.drawText(exportDate, PAGE_WIDTH - MARGIN_HORIZONTAL - exportDateWidth, currentY + 12f, headerTextPaint)

            currentY += 24f
            canvas.drawLine(MARGIN_HORIZONTAL, currentY, PAGE_WIDTH - MARGIN_HORIZONTAL, currentY, dividerPaint)
            currentY += 16f

            if (pageNumber == 1) {
                // Main Header Banner
                canvas.drawText("All Saved Notes (${notes.size} total)", MARGIN_HORIZONTAL, currentY + 18f, mainTitlePaint)
                currentY += 34f
                canvas.drawLine(MARGIN_HORIZONTAL, currentY, PAGE_WIDTH - MARGIN_HORIZONTAL, currentY, dividerPaint)
                currentY += 16f
            }

            // Draw notes that fit on this page
            while (noteIndex < notes.size) {
                val note = notes[noteIndex]
                val noteTitle = note.displayTitle
                val noteBody = if (note.content.isNotBlank()) note.content.trim() else "(No content)"

                val nTitleLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    StaticLayout.Builder.obtain(noteTitle, 0, noteTitle.length, noteTitlePaint, (CONTENT_WIDTH - 24f).toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    StaticLayout(noteTitle, noteTitlePaint, (CONTENT_WIDTH - 24f).toInt(), Layout.Alignment.ALIGN_NORMAL, 1.1f, 0f, true)
                }

                val nBodyLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    StaticLayout.Builder.obtain(noteBody, 0, noteBody.length, contentPaint, (CONTENT_WIDTH - 24f).toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(2f, 1.2f)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    StaticLayout(noteBody, contentPaint, (CONTENT_WIDTH - 24f).toInt(), Layout.Alignment.ALIGN_NORMAL, 1.2f, 2f, true)
                }

                val entryHeight = nTitleLayout.height + nBodyLayout.height + 48f
                val availableSpace = PAGE_HEIGHT - MARGIN_BOTTOM - currentY - 25f

                if (entryHeight > availableSpace && currentY > MARGIN_TOP + 80f) {
                    // Break to next page
                    break
                }

                // Draw Note Card Frame
                val cardRect = RectF(MARGIN_HORIZONTAL, currentY, PAGE_WIDTH - MARGIN_HORIZONTAL, currentY + entryHeight)
                canvas.drawRoundRect(cardRect, 10f, 10f, cardBgPaint)

                // Title
                canvas.save()
                canvas.translate(MARGIN_HORIZONTAL + 12f, currentY + 12f)
                nTitleLayout.draw(canvas)
                canvas.restore()

                val afterTitleY = currentY + 12f + nTitleLayout.height + 6f
                val noteMeta = "Updated: ${DateTimeUtils.formatNoteTimestamp(note.updatedAt)}"
                canvas.drawText(noteMeta, MARGIN_HORIZONTAL + 12f, afterTitleY + 8f, metaPaint)

                val bodyY = afterTitleY + 18f
                canvas.save()
                canvas.translate(MARGIN_HORIZONTAL + 12f, bodyY)
                nBodyLayout.draw(canvas)
                canvas.restore()

                currentY += entryHeight + 14f
                noteIndex++
            }

            // Footer
            val footerY = PAGE_HEIGHT - MARGIN_BOTTOM + 20f
            canvas.drawLine(MARGIN_HORIZONTAL, footerY - 10f, PAGE_WIDTH - MARGIN_HORIZONTAL, footerY - 10f, dividerPaint)
            canvas.drawText("NoteLedger • Offline Archive", MARGIN_HORIZONTAL, footerY + 6f, footerPaint)

            val pageLabel = "Page $pageNumber"
            val pageLabelWidth = footerPaint.measureText(pageLabel)
            canvas.drawText(pageLabel, PAGE_WIDTH - MARGIN_HORIZONTAL - pageLabelWidth, footerY + 6f, footerPaint)

            pdfDocument.finishPage(page)
            pageNumber++
        }

        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }

    /**
     * Creates a temporary PDF file for previewing, sharing, or opening.
     */
    fun createTempNotePdfFile(
        context: Context,
        note: Note,
        title: String = note.title,
        content: String = note.content
    ): File {
        val sanitizedTitle = note.displayTitle
            .replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            .take(30)
            .ifBlank { "Note" }
        val fileName = "Note_${sanitizedTitle}_${System.currentTimeMillis()}.pdf"
        val cacheDir = File(context.cacheDir, "pdf_exports").apply { mkdirs() }
        val pdfFile = File(cacheDir, fileName)

        FileOutputStream(pdfFile).use { outputStream ->
            generateSingleNotePdf(context, note, title, content, outputStream)
        }
        return pdfFile
    }

    /**
     * Creates a temporary Batch PDF file containing all notes.
     */
    fun createTempBatchPdfFile(context: Context, notes: List<Note>): File {
        val fileName = "All_Notes_Archive_${LocalDate.now()}.pdf"
        val cacheDir = File(context.cacheDir, "pdf_exports").apply { mkdirs() }
        val pdfFile = File(cacheDir, fileName)

        FileOutputStream(pdfFile).use { outputStream ->
            generateNotesBatchPdf(context, notes, outputStream)
        }
        return pdfFile
    }

    /**
     * Launches standard Android Share Sheet for a generated PDF file.
     */
    fun sharePdfFile(context: Context, pdfFile: File, title: String = "Exported Note PDF") {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Note as PDF"))
    }

    /**
     * Launches standard PDF reader or print viewer on device.
     */
    fun openPdfFile(context: Context, pdfFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            // Fallback to sharing if no dedicated viewer is installed
            sharePdfFile(context, pdfFile, pdfFile.name)
        }
    }
}
