package com.example.myapplication.domain.model

data class UserInfo(
    val userName: String,
    val avatarUrl: String,
    val totalSpace: Long,
    val usedSpace: Long
)
