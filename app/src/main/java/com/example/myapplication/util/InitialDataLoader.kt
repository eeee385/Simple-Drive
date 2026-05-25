package com.example.myapplication.util

import android.content.Context
import com.example.myapplication.data.repository.FileRepository
import com.example.myapplication.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object InitialDataLoader {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_DATA_INITIALIZED = "data_initialized"

    fun isInitialized(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DATA_INITIALIZED, false)
    }

    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        val app = context.applicationContext as com.example.myapplication.SimplePanApplication
        val fileRepo = app.fileRepository
        val userRepo = app.userRepository

        if (!isInitialized(context)) {
            // sync mock network data
            if (fileRepo.getFileCount() == 0) {
                fileRepo.syncFromMockData(context)
            }
            userRepo.refreshUserInfo(context)
            // update used space from actual file data
            val usedSpace = fileRepo.getTotalUsedSpace()
            userRepo.updateUsedSpace(usedSpace)

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_DATA_INITIALIZED, true)
                .apply()
        }
    }
}
