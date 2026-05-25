package com.example.myapplication.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey val fileId: String,
    val name: String,
    val size: Long,
    val path: String,
    val type: String,
    val parentId: String?,
    val timestamp: Long,
    val contentUri: String? = null
)
