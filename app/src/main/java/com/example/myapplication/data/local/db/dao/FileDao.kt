package com.example.myapplication.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.db.entity.FileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {

    @Query("SELECT * FROM files WHERE parentId IS :parentId OR (parentId IS NULL AND :parentId IS NULL)")
    fun getFilesByParentId(parentId: String?): Flow<List<FileEntity>>

    @Query("SELECT * FROM files")
    fun getAllFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE fileId = :fileId")
    suspend fun getFileById(fileId: String): FileEntity?

    @Query("SELECT * FROM files WHERE type = 'folder' AND fileId != :excludeId")
    suspend fun getAllFoldersExcept(excludeId: String): List<FileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<FileEntity>)

    @Query("UPDATE files SET name = :newName WHERE fileId = :fileId")
    suspend fun renameFile(fileId: String, newName: String)

    @Query("UPDATE files SET parentId = :newParentId WHERE fileId = :fileId")
    suspend fun moveFile(fileId: String, newParentId: String?)

    @Query("DELETE FROM files WHERE fileId = :fileId")
    suspend fun deleteFile(fileId: String)

    @Query("DELETE FROM files WHERE parentId = :parentId")
    suspend fun deleteChildrenOfFolder(parentId: String)

    @Query("SELECT COALESCE(SUM(size), 0) FROM files WHERE type != 'folder'")
    suspend fun getTotalUsedSpace(): Long

    @Query("SELECT COUNT(*) FROM files")
    suspend fun getFileCount(): Int
}
