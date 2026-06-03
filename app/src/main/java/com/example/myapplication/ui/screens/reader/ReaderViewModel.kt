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
import java.nio.charset.Charset

private fun readWithDetectedEncoding(file: File): String {
    val bytes = file.readBytes()
    return decodeWithDetection(bytes)
}

private fun decodeWithDetection(bytes: ByteArray): String {
    val charset = detectCharset(bytes)
    return String(bytes, charset)
}

private fun detectCharset(bytes: ByteArray): Charset {
    // UTF-8 BOM: EF BB BF
    if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
        return Charsets.UTF_8
    }
    // UTF-16 LE BOM: FF FE
    if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
        return Charsets.UTF_16LE
    }
    // UTF-16 BE BOM: FE FF
    if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
        return Charsets.UTF_16BE
    }
    // No BOM: try UTF-8, if replacement chars appear, fall back to GBK (Chinese ANSI)
    val utf8 = String(bytes, Charsets.UTF_8)
    if (utf8.indexOf('�') >= 0) {
        return try { Charset.forName("GBK") } catch (_: Exception) { Charsets.UTF_8 }
    }
    return Charsets.UTF_8
}

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
                    val f = File(context.filesDir, file.contentUri)
                    _fullText.value = if (f.exists()) readWithDetectedEncoding(f) else "文件不存在"
                } else {
                    try {
                        val bytes = context.assets.open("sample.txt").use { it.readBytes() }
                        _fullText.value = decodeWithDetection(bytes)
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
