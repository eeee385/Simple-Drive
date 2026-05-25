package com.example.myapplication.data.local.model

import kotlinx.serialization.Serializable

@Serializable
data class FileDto(
    val fileId: String,
    val name: String,
    val type: String,
    val size: Long = 0,
    val path: String = "",
    val parentId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val children: List<FileDto>? = null
)
