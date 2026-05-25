package com.example.myapplication.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "share_links")
data class ShareLinkEntity(
    @PrimaryKey val shareId: String,
    val fileId: String,
    val createdAt: Long = System.currentTimeMillis()
)
