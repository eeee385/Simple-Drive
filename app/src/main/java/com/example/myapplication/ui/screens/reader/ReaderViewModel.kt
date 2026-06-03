package com.example.myapplication.ui.screens.reader

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ReaderViewModel(
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _fileName = MutableStateFlow("")
    val fileName: StateFlow<String> = _fileName.asStateFlow()

    private val _fullText = MutableStateFlow("")
    val fullText: StateFlow<String> = _fullText.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun initialize(context: Context, fileId: String) {
        if (_fullText.value.isNotEmpty()) return  // Already loaded
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val file = fileRepository.getFileById(fileId)
                _fileName.value = file?.name ?: "未知文件"

                if (file?.contentUri != null) {
                    // Real uploaded file
                    val f = File(context.filesDir, file.contentUri)
                    _fullText.value = if (f.exists()) f.readText() else "文件不存在"
                } else {
                    // Mock file: load sample text from assets
                    try {
                        _fullText.value = context.assets.open("sample.txt").bufferedReader().use { it.readText() }
                    } catch (e: Exception) {
                        _fullText.value = "无法加载文本"
                    }
                }
                _isLoading.value = false
            }
        }
    }

    class Factory(
        private val fileRepository: FileRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReaderViewModel(fileRepository) as T
        }
    }
}
