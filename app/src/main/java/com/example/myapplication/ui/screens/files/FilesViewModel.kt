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
import kotlinx.coroutines.flow.map
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
        .map { list -> list.sortedWith(fileSortComparator) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        private val typeOrder = mapOf(
            "folder" to 0,
            "txt" to 1,
            "image" to 2,
            "video" to 3,
            "audio" to 4
        )

        val fileSortComparator = Comparator<FileEntity> { a, b ->
            val typeA = typeOrder[a.type] ?: 99
            val typeB = typeOrder[b.type] ?: 99
            if (typeA != typeB) typeA - typeB
            else a.name.lowercase().compareTo(b.name.lowercase())
        }
    }

    val isLoading = MutableStateFlow(false)

    // Multi-select state
    private val _selectedFileIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedFileIds: StateFlow<Set<String>> = _selectedFileIds.asStateFlow()

    val isSelectionMode: StateFlow<Boolean> = _selectedFileIds
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleSelection(fileId: String) {
        _selectedFileIds.value = _selectedFileIds.value.let { ids ->
            if (fileId in ids) ids - fileId else ids + fileId
        }
    }

    fun selectAll() {
        viewModelScope.launch {
            _selectedFileIds.value = files.value.map { it.fileId }.toSet()
        }
    }

    fun clearSelection() {
        _selectedFileIds.value = emptySet()
    }

    fun navigateToFolder(parentId: String) {
        clearSelection()
        navStack.addLast(_currentParentId.value)
        _currentParentId.value = parentId
    }

    fun navigateBack(): Boolean {
        if (navStack.isEmpty()) return false
        clearSelection()
        _currentParentId.value = navStack.removeLast()
        return true
    }

    fun deleteSelectedFiles() {
        viewModelScope.launch {
            val ids = _selectedFileIds.value.toList()
            for (id in ids) {
                fileRepository.deleteFileRecursively(id)
            }
            clearSelection()
            updateUsedSpace()
        }
    }

    fun renameFile(fileId: String, newName: String) {
        viewModelScope.launch {
            fileRepository.renameFile(fileId, newName)
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val folder = FileEntity(
                fileId = UUID.randomUUID().toString(),
                name = name,
                size = 0,
                path = name,
                type = "folder",
                parentId = _currentParentId.value,
                timestamp = System.currentTimeMillis()
            )
            fileRepository.insertFile(folder)
        }
    }

    fun moveSelectedFiles(newParentId: String?) {
        viewModelScope.launch {
            val ids = _selectedFileIds.value.toList()
            for (id in ids) {
                fileRepository.moveFile(id, newParentId)
            }
            clearSelection()
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

                    val fileName = "${UUID.randomUUID()}_$name"
                    val uploadDir = java.io.File(context.filesDir, "uploads")
                    if (!uploadDir.exists()) uploadDir.mkdirs()
                    val destFile = java.io.File(uploadDir, fileName)
                    resolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output, bufferSize = 8192)
                        }
                    }
                    val internalPath = "uploads/$fileName"

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
