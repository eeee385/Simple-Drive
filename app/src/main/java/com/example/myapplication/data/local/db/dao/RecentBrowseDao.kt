package com.example.myapplication.data.local.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.db.entity.FileEntity
import com.example.myapplication.data.local.db.entity.RecentBrowseEntity
import kotlinx.coroutines.flow.Flow

data class FileWithBrowseTime(
    @Embedded val file: FileEntity,
    val browseTime: Long
)

@Dao
interface RecentBrowseDao {

    @Query("""
        SELECT f.*, rb.browseTime FROM files f
        INNER JOIN recent_browse rb ON f.fileId = rb.fileId
        ORDER BY rb.browseTime DESC LIMIT :limit
    """)
    fun getRecentBrowses(limit: Int = 20): Flow<List<FileWithBrowseTime>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: RecentBrowseEntity)
}
