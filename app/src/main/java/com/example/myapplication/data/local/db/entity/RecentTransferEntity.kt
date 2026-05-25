package com.example.myapplication.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recent_transfer",
    indices = [Index(value = ["fileId"], unique = true)]
)
data class RecentTransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileId: String,
    val transferTime: Long
)
