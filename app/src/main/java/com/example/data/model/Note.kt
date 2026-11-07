package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val createdAt: String = Instant.now().toString(),
    val updatedAt: String = Instant.now().toString()
) {
    val displayTitle: String
        get() {
            if (title.isNotBlank()) return title.trim()
            val firstLine = content.trim().lineSequence().firstOrNull() ?: ""
            return if (firstLine.isNotBlank()) firstLine else "Untitled Note"
        }

    val preview: String
        get() {
            if (title.isNotBlank()) {
                val text = content.trim()
                return if (text.isNotBlank()) text else ""
            }
            val lines = content.trim().lineSequence().drop(1).filter { it.isNotBlank() }.toList()
            return if (lines.isNotEmpty()) lines.joinToString(" ") else ""
        }
}

