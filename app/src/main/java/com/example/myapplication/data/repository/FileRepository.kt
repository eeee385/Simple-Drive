package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.local.db.AppDatabase
import com.example.myapplication.data.local.db.dao.FileDao
import com.example.myapplication.data.local.db.dao.FileWithBrowseTime
import com.example.myapplication.data.local.db.dao.FileWithTransferTime
import com.example.myapplication.data.local.db.dao.RecentBrowseDao
import com.example.myapplication.data.local.db.dao.RecentTransferDao
import com.example.myapplication.data.local.db.dao.ShareLinkDao
import com.example.myapplication.data.local.db.entity.FileEntity
import com.example.myapplication.data.local.db.entity.RecentBrowseEntity
import com.example.myapplication.data.local.db.entity.RecentTransferEntity
import com.example.myapplication.data.local.model.FileDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class FileRepository(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val db = AppDatabase.getInstance(context)
    private val fileDao: FileDao = db.fileDao()
    private val browseDao: RecentBrowseDao = db.recentBrowseDao()
    private val transferDao: RecentTransferDao = db.recentTransferDao()
    private val shareLinkDao: ShareLinkDao = db.shareLinkDao()

    fun getFilesByParentId(parentId: String?): Flow<List<FileEntity>> =
        fileDao.getFilesByParentId(parentId)

    fun getAllFiles(): Flow<List<FileEntity>> =
        fileDao.getAllFiles()

    suspend fun getFileById(fileId: String): FileEntity? =
        fileDao.getFileById(fileId)

    suspend fun getAllFoldersExcept(excludeId: String): List<FileEntity> =
        fileDao.getAllFoldersExcept(excludeId)

    suspend fun insertFile(file: FileEntity) = fileDao.insertFile(file)

    suspend fun renameFile(fileId: String, newName: String) = fileDao.renameFile(fileId, newName)

    suspend fun moveFile(fileId: String, newParentId: String?) = fileDao.moveFile(fileId, newParentId)

    suspend fun deleteFileRecursively(fileId: String) {
        fileDao.deleteChildrenOfFolder(fileId)
        fileDao.deleteFile(fileId)
    }

    suspend fun getTotalUsedSpace(): Long = fileDao.getTotalUsedSpace()

    suspend fun getFileCount(): Int = fileDao.getFileCount()

    fun getRecentBrowses(limit: Int = 20): Flow<List<FileWithBrowseTime>> =
        browseDao.getRecentBrowses(limit)

    fun getRecentTransfers(limit: Int = 20): Flow<List<FileWithTransferTime>> =
        transferDao.getRecentTransfers(limit)

    suspend fun recordBrowse(fileId: String) {
        browseDao.upsert(
            RecentBrowseEntity(fileId = fileId, browseTime = System.currentTimeMillis())
        )
    }

    suspend fun recordTransfer(fileId: String) {
        transferDao.upsert(
            RecentTransferEntity(fileId = fileId, transferTime = System.currentTimeMillis())
        )
    }

    // Share link
    suspend fun createShareLink(shareId: String, fileId: String) {
        shareLinkDao.insert(
            com.example.myapplication.data.local.db.entity.ShareLinkEntity(
                shareId = shareId, fileId = fileId
            )
        )
    }

    suspend fun resolveShareLink(shareId: String): String? =
        shareLinkDao.getShareLink(shareId)?.fileId

    suspend fun resolveShareLinks(shareId: String): List<String> =
        shareLinkDao.getShareLinks(shareId).map { it.fileId }

    // Mock network sync: load JSON from assets, parse, insert into Room
    suspend fun syncFromMockData(context: Context) = withContext(Dispatchers.IO) {
        delay(500) // simulate network latency
        val jsonString = context.assets.open("files.json").bufferedReader().use { it.readText() }
        val dtos = json.decodeFromString<List<FileDto>>(jsonString)
        val entities = mutableListOf<FileEntity>()
        flattenDtos(dtos, null, entities)
        fileDao.insertFiles(entities)
    }

    private fun flattenDtos(
        dtos: List<FileDto>,
        parentId: String?,
        target: MutableList<FileEntity>
    ) {
        for (dto in dtos) {
            target.add(
                FileEntity(
                    fileId = dto.fileId,
                    name = dto.name,
                    size = dto.size,
                    path = dto.path,
                    type = dto.type,
                    parentId = parentId ?: dto.parentId,
                    timestamp = dto.timestamp
                )
            )
            if (!dto.children.isNullOrEmpty()) {
                flattenDtos(dto.children, dto.fileId, target)
            }
        }
    }
}
