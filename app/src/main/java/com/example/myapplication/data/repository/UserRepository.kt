package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.domain.model.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class UserRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _userInfo = MutableStateFlow(loadDefaultUserInfo())

    val userInfo: Flow<UserInfo> = _userInfo.asStateFlow()

    private fun loadDefaultUserInfo(): UserInfo {
        val savedName = prefs.getString("user_name", "演示用户") ?: "演示用户"
        return UserInfo(
            userName = savedName,
            avatarUrl = "",
            totalSpace = 10L * 1024 * 1024 * 1024, // 10 GB
            usedSpace = 0
        )
    }

    suspend fun refreshUserInfo(context: Context) = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("user_info.json").bufferedReader().use { it.readText() }
            val json = Json.parseToJsonElement(jsonString).jsonObject
            val name = json["userName"]?.jsonPrimitive?.content ?: "演示用户"
            val total = json["totalSpace"]?.jsonPrimitive?.content?.toLongOrNull() ?: (10L * 1024 * 1024 * 1024)
            val used = json["usedSpace"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
            _userInfo.value = UserInfo(userName = name, avatarUrl = "", totalSpace = total, usedSpace = used)
        } catch (e: Exception) {
            // use defaults
        }
    }

    fun updateUsedSpace(usedSpace: Long) {
        _userInfo.value = _userInfo.value.copy(usedSpace = usedSpace)
    }
}
