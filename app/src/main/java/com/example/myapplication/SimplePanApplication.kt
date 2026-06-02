package com.example.myapplication

import android.app.Application
import com.example.myapplication.data.local.db.AppDatabase
import com.example.myapplication.data.repository.FileRepository
import com.example.myapplication.data.repository.ShareRepository
import com.example.myapplication.data.repository.UserRepository

class SimplePanApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val fileRepository: FileRepository by lazy { FileRepository(this) }
    val userRepository: UserRepository by lazy { UserRepository(this) }
    val shareRepository: ShareRepository by lazy { ShareRepository(fileRepository) }

    // DeepLink bridge: callback set by Compose, invoked by Activity
    var onDeepLinkShareId: ((String) -> Unit)? = null
}
