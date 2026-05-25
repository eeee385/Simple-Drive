package com.example.myapplication.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recent_browse",
    indices = [Index(value = ["fileId"], unique = true)]
)
data class RecentBrowseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileId: String,
    val browseTime: Long
)
