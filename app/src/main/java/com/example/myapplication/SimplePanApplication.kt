package com.example.myapplication

import android.app.Application
import com.example.myapplication.data.local.db.AppDatabase
import com.example.myapplication.data.repository.FileRepository
import com.example.myapplication.data.repository.ShareRepository
import com.example.myapplication.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SimplePanApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val fileRepository: FileRepository by lazy { FileRepository(this) }
    val userRepository: UserRepository by lazy { UserRepository(this) }
    val shareRepository: ShareRepository by lazy { ShareRepository(fileRepository) }

    // DeepLink bridge: Activity sets shareId, Compose observes and consumes
    private val _pendingShareId = MutableStateFlow<String?>(null)
    val pendingShareId = _pendingShareId.asStateFlow()

    fun submitDeepLinkShareId(shareId: String?) {
        _pendingShareId.value = shareId
    }

    fun consumeDeepLinkShareId() {
        _pendingShareId.value = null
    }
}
