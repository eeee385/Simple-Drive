package com.example.myapplication.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector

object FileTypeHelper {

    fun getFileIcon(type: String): ImageVector = when (type) {
        "folder" -> Icons.Filled.Folder
        "video" -> Icons.Filled.PlayArrow
        "txt" -> Icons.Filled.Description
        "image" -> Icons.Filled.Image
        "audio" -> Icons.Filled.MusicNote
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
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
