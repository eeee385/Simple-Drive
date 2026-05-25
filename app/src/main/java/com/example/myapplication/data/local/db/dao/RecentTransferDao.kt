package com.example.myapplication.data.local.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.db.entity.FileEntity
import com.example.myapplication.data.local.db.entity.RecentTransferEntity
import kotlinx.coroutines.flow.Flow

data class FileWithTransferTime(
    @Embedded val file: FileEntity,
    val transferTime: Long
)

@Dao
interface RecentTransferDao {

    @Query("""
        SELECT f.*, rt.transferTime FROM files f
        INNER JOIN recent_transfer rt ON f.fileId = rt.fileId
        ORDER BY rt.transferTime DESC LIMIT :limit
    """)
    fun getRecentTransfers(limit: Int = 20): Flow<List<FileWithTransferTime>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: RecentTransferEntity)
}
