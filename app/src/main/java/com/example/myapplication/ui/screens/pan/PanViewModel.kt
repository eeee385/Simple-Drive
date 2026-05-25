package com.example.myapplication.ui.screens.pan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.db.dao.FileWithBrowseTime
import com.example.myapplication.data.local.db.dao.FileWithTransferTime
import com.example.myapplication.data.repository.FileRepository
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.domain.model.UserInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PanViewModel(
    private val fileRepository: FileRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val userInfo: StateFlow<UserInfo> = userRepository.userInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserInfo("", "", 0, 0))

    val recentBrowses: StateFlow<List<FileWithBrowseTime>> = fileRepository.getRecentBrowses(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransfers: StateFlow<List<FileWithTransferTime>> = fileRepository.getRecentTransfers(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun recordBrowse(fileId: String) {
        viewModelScope.launch { fileRepository.recordBrowse(fileId) }
    }

    class Factory(
        private val fileRepository: FileRepository,
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PanViewModel(fileRepository, userRepository) as T
        }
    }
}
