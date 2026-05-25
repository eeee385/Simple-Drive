package com.example.myapplication.data.repository

import com.example.myapplication.data.local.db.entity.ShareLinkEntity
import kotlin.random.Random

class ShareRepository(private val fileRepository: FileRepository) {

    private val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    suspend fun generateShareLink(fileId: String): String {
        val shareId = (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        fileRepository.createShareLink(shareId, fileId)
        return "simplepan://share?sid=$shareId"
    }

    suspend fun resolveShareLink(shareId: String): String? {
        return fileRepository.resolveShareLink(shareId)
    }
}
