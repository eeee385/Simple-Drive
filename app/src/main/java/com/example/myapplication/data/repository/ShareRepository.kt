package com.example.myapplication.data.repository

import com.example.myapplication.data.local.db.entity.ShareLinkEntity
import com.example.myapplication.util.FileTypeHelper
import kotlin.random.Random

class ShareRepository(private val fileRepository: FileRepository) {

    private val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    suspend fun generateShareLink(fileIds: List<String>): String {
        val shareId = (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        for (fileId in fileIds) {
            fileRepository.createShareLink(shareId, fileId)
        }
        return "${FileTypeHelper.DEEP_LINK_PREFIX}$shareId"
    }

    suspend fun resolveShareLinks(shareId: String): List<String> {
        return fileRepository.resolveShareLinks(shareId)
    }
}
