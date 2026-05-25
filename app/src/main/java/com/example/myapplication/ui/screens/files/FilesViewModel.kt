package com.example.myapplication.ui.screens.files

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.db.entity.FileEntity
import com.example.myapplication.data.repository.FileRepository
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.util.FileTypeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FilesViewModel(
    private val fileRepository: FileRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _currentParentId = MutableStateFlow<String?>(null)
    val currentParentId: StateFlow<String?> = _currentParentId.asStateFlow()

    private val navStack = ArrayDeque<String?>()

    val files: StateFlow<List<FileEntity>> = _currentParentId
        .flatMapLatest { parentId -> fileRepository.getFilesByParentId(parentId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLoading = MutableStateFlow(false)

    fun navigateToFolder(parentId: String) {
        navStack.addLast(_currentParentId.value)
        _currentParentId.value = parentId
    }

    fun navigateBack(): Boolean {
        if (navStack.isEmpty()) return false
        _currentParentId.value = navStack.removeLast()
        return true
    }

    fun deleteFile(fileId: String) {
        viewModelScope.launch {
            fileRepository.deleteFileRecursively(fileId)
            updateUsedSpace()
        }
    }

    fun renameFile(fileId: String, newName: String) {
        viewModelScope.launch {
            fileRepository.renameFile(fileId, newName)
        }
    }

    fun moveFile(fileId: String, newParentId: String?) {
        viewModelScope.launch {
            fileRepository.moveFile(fileId, newParentId)
        }
    }

    fun uploadFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            isLoading.value = true
            withContext(Dispatchers.IO) {
                try {
                    val resolver = context.contentResolver
                    var name = "unknown_file"
                    var size = 0L

                    resolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (nameIdx >= 0) name = cursor.getString(nameIdx)
                            if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
                        }
                    }

                    // Copy to internal storage
                    val internalPath = "uploads/${UUID.randomUUID()}_$name"
                    resolver.openInputStream(uri)?.use { input ->
                        context.openFileOutput(internalPath, Context.MODE_PRIVATE).use { output ->
                            input.copyTo(output, bufferSize = 8192)
                        }
                    }

                    val type = FileTypeHelper.getFileTypeFromName(name)
                    val file = FileEntity(
                        fileId = UUID.randomUUID().toString(),
                        name = name,
                        size = size,
                        path = internalPath,
                        type = type,
                        parentId = _currentParentId.value,
                        timestamp = System.currentTimeMillis(),
                        contentUri = internalPath
                    )
                    fileRepository.insertFile(file)
                    fileRepository.recordTransfer(file.fileId)
                    updateUsedSpace()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            isLoading.value = false
        }
    }

    fun recordBrowse(fileId: String) {
        viewModelScope.launch { fileRepository.recordBrowse(fileId) }
    }

    private suspend fun updateUsedSpace() {
        val used = fileRepository.getTotalUsedSpace()
        userRepository.updateUsedSpace(used)
    }

    class Factory(
        private val fileRepository: FileRepository,
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FilesViewModel(fileRepository, userRepository) as T
        }
    }
}
