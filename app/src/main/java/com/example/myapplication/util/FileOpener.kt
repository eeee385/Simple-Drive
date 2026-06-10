package com.example.myapplication.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.myapplication.data.local.db.entity.FileEntity
import com.example.myapplication.domain.model.FileCategory
import java.io.File

object FileOpener {

    fun openFile(context: Context, file: FileEntity) {
        when (file.type) {
            FileCategory.VIDEO -> openWithSystemPlayer(context, file, "video/*")
            FileCategory.AUDIO -> openWithSystemPlayer(context, file, "audio/*")
            else -> Toast.makeText(context, "暂不支持预览此文件类型", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWithSystemPlayer(context: Context, file: FileEntity, mimeType: String) {
        val contentUri = file.contentUri
        if (contentUri.isNullOrBlank()) {
            Toast.makeText(context, "模拟数据无法播放，请上传真实文件", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val realFile = File(context.filesDir, contentUri)
            if (!realFile.exists()) {
                Toast.makeText(context, "文件不存在", Toast.LENGTH_SHORT).show()
                return
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                realFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "没有可用的播放器", Toast.LENGTH_SHORT).show()
        }
    }
}
