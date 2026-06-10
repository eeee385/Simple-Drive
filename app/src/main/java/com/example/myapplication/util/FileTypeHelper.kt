package com.example.myapplication.util

import com.example.myapplication.R
import com.example.myapplication.domain.model.FileCategory

object FileTypeHelper {
    const val DEEP_LINK_PREFIX = "simplepan://share?sid="

    fun getFileIconRes(type: String): Int = when (type) {
        FileCategory.FOLDER -> R.drawable.ic_folder
        FileCategory.TXT -> R.drawable.ic_txt
        FileCategory.IMAGE -> R.drawable.ic_image
        FileCategory.VIDEO -> R.drawable.ic_video
        FileCategory.AUDIO -> R.drawable.ic_music
        FileCategory.PDF -> R.drawable.ic_pdf
        FileCategory.PPT -> R.drawable.ic_ppt
        FileCategory.APK -> R.drawable.ic_apk
        FileCategory.CODE -> R.drawable.ic_code
        FileCategory.EXE -> R.drawable.ic_exe
        FileCategory.RAR -> R.drawable.ic_rar
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
            "txt", "md", "log" -> FileCategory.TXT
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg" -> FileCategory.IMAGE
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "3gp" -> FileCategory.VIDEO
            "mp3", "wav", "ogg", "flac", "aac", "wma" -> FileCategory.AUDIO
            "pdf" -> FileCategory.PDF
            "ppt", "pptx" -> FileCategory.PPT
            "apk" -> FileCategory.APK
            "kt", "java", "py", "js", "ts", "html", "css", "xml", "json", "c", "cpp", "h", "go", "rs", "swift" -> FileCategory.CODE
            "exe", "msi" -> FileCategory.EXE
            "rar", "zip", "7z", "tar", "gz" -> FileCategory.RAR
            else -> FileCategory.OTHER
        }
    }

    fun getFileExtension(name: String): String =
        name.substringAfterLast('.', "")
}
