package com.example.myapplication.domain.model

enum class FilterType { ALL, IMAGE, VIDEO, DOC }

object FileCategory {
    const val FOLDER = "folder"
    const val TXT = "txt"
    const val IMAGE = "image"
    const val VIDEO = "video"
    const val AUDIO = "audio"
    const val PDF = "pdf"
    const val PPT = "ppt"
    const val APK = "apk"
    const val CODE = "code"
    const val EXE = "exe"
    const val RAR = "rar"
    const val OTHER = "other"

    val MEDIA_TYPES = setOf(IMAGE, VIDEO, AUDIO)
    val BINARY_TYPES = setOf(APK, EXE, RAR)
    val DOC_TYPES = setOf(TXT, PDF, PPT, CODE)

    fun isDocument(type: String) = type !in setOf(FOLDER) + MEDIA_TYPES + BINARY_TYPES

    fun isFolder(type: String) = type == FOLDER
}
