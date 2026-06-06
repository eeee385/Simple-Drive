package com.example.myapplication.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object FileTypeHelper {

    fun getFileIcon(type: String): ImageVector = when (type) {
        "folder" -> Icons.Filled.Folder
        "txt" -> Icons.AutoMirrored.Filled.Article
        "image" -> Icons.Filled.Collections
        "video" -> Icons.Filled.VideoFile
        "audio" -> Icons.Filled.Headphones
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }

    fun getFileColor(type: String): Color = when (type) {
        "folder" -> Color(0xFF0284C7)
        "txt" -> Color(0xFF059669)
        "image" -> Color(0xFFE11D48)
        "video" -> Color(0xFFD97706)
        "audio" -> Color(0xFF7C3AED)
        else -> Color(0xFF64748B)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 0) return ""
        if (bytes == 0L) return "0 B"
        return when {
            bytes < 1024 -> "${bytes} B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toFloat() / (1024 * 1024))} MB"
            else -> "${"%.2f".format(bytes.toFloat() / (1024 * 1024 * 1024))} GB"
        }
    }

    fun getFileTypeFromName(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "txt", "md", "log" -> "txt"
            "mp4", "avi", "mkv", "mov", "wmv", "flv" -> "video"
            "jpg", "jpeg", "png", "gif", "bmp", "webp" -> "image"
            "mp3", "wav", "ogg", "flac", "aac" -> "audio"
            else -> "other"
        }
    }

    fun getFileExtension(name: String): String =
        name.substringAfterLast('.', "")
}
