package com.example.myapplication.util

import com.example.myapplication.R

object FileTypeHelper {
    const val DEEP_LINK_PREFIX = "simplepan://share?sid="

    fun getFileIconRes(type: String): Int = when (type) {
        "folder" -> R.drawable.ic_folder
        "txt" -> R.drawable.ic_txt
        "image" -> R.drawable.ic_image
        "video" -> R.drawable.ic_video
        "audio" -> R.drawable.ic_music
        "pdf" -> R.drawable.ic_pdf
        "ppt" -> R.drawable.ic_ppt
        "apk" -> R.drawable.ic_apk
        "code" -> R.drawable.ic_code
        "exe" -> R.drawable.ic_exe
        "rar" -> R.drawable.ic_rar
        else -> R.drawable.ic_file
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
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg" -> "image"
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "3gp" -> "video"
            "mp3", "wav", "ogg", "flac", "aac", "wma" -> "audio"
            "pdf" -> "pdf"
            "ppt", "pptx" -> "ppt"
            "apk" -> "apk"
            "kt", "java", "py", "js", "ts", "html", "css", "xml", "json", "c", "cpp", "h", "go", "rs", "swift" -> "code"
            "exe", "msi" -> "exe"
            "rar", "zip", "7z", "tar", "gz" -> "rar"
            else -> "other"
        }
    }

    fun getFileExtension(name: String): String =
        name.substringAfterLast('.', "")
}
