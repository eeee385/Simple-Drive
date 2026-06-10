package com.example.myapplication.data.local.db.entity

import androidx.room.Entity

@Entity(tableName = "share_links", primaryKeys = ["shareId", "fileId"])
data class ShareLinkEntity(
    val shareId: String,
    val fileId: String,
    val createdAt: Long = System.currentTimeMillis()
)
