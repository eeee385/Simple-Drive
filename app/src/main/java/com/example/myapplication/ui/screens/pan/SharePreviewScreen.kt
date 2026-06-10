package com.example.myapplication.ui.screens.pan

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.SimplePanApplication
import com.example.myapplication.data.local.db.entity.FileEntity
import com.example.myapplication.ui.components.LoadingOverlay
import com.example.myapplication.ui.theme.WarmAmber
import com.example.myapplication.util.FileTypeHelper
import com.example.myapplication.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class ShareTreeItem(
    val file: FileEntity,
    val depth: Int,
    val children: List<ShareTreeItem> = emptyList()
)

@Composable
fun SharePreviewScreen(
    shareId: String,
    onDismiss: () -> Unit,
    onTransfer: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SimplePanApplication
    var isLoading by remember { mutableStateOf(true) }
    var rootItems by remember { mutableStateOf<List<ShareTreeItem>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val expandedIds = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(shareId) {
        withContext(Dispatchers.IO) {
            val fileIds = app.shareRepository.resolveShareLinks(shareId)
            if (fileIds.isEmpty()) { errorMsg = "分享链接无效或已过期"; isLoading = false; return@withContext }
            val items = fileIds.mapNotNull { id -> app.fileRepository.getFileById(id) }
            if (items.isEmpty()) { errorMsg = "文件不存在"; isLoading = false; return@withContext }
            rootItems = items.map { file -> buildTree(app, file, 0) }.sortedByDescending { it.file.type == "folder" }
            items.filter { it.type == "folder" }.forEach { expandedIds[it.fileId] = true }
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sharer info
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "分享者: 演示用户",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider()

        // Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                isLoading -> LoadingOverlay()
                errorMsg != null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(errorMsg!!, style = MaterialTheme.typography.titleMedium)
                }
                rootItems.isNotEmpty() -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(rootItems) { item ->
                            ShareTreeView(item, expandedIds)
                        }
                    }
                }
            }
        }

        // Bottom buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(24.dp)
            ) { Text("取消") }
            Button(
                onClick = onTransfer,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(24.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = WarmAmber)
            ) { Text("转存") }
        }
    }
}

@Composable
private fun ShareTreeView(item: ShareTreeItem, expandedIds: MutableMap<String, Boolean>) {
    val context = LocalContext.current
    val isFolder = item.file.type == "folder"
    val hasChildren = item.children.isNotEmpty()
    val isExpanded = expandedIds[item.file.fileId] == true

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    if (isFolder) {
                        expandedIds[item.file.fileId] = !isExpanded
                    } else {
                        Toast.makeText(context, "请转存后查看", Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width((item.depth * 24).dp))

            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                if (isFolder && hasChildren) {
                    Icon(
                        if (isExpanded) Icons.Filled.ExpandMore else Icons.Filled.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(FileTypeHelper.getFileIconRes(item.file.type)),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.file.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isFolder) FontWeight.Medium else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    buildString {
                        if (item.file.type != "folder") {
                            append(FileTypeHelper.formatFileSize(item.file.size))
                            append(" · ")
                        }
                        append(TimeUtils.formatRelativeTime(item.file.timestamp))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isExpanded && hasChildren) {
            item.children.forEach { child -> ShareTreeView(child, expandedIds) }
        }
    }
}

private suspend fun buildTree(app: SimplePanApplication, file: FileEntity, depth: Int): ShareTreeItem {
    val children = if (file.type == "folder") {
        app.fileRepository.getFilesByParentId(file.fileId)
            .let { flow -> flow.first() }
            .map { child -> buildTree(app, child, depth + 1) }
            .sortedByDescending { it.file.type == "folder" }
    } else emptyList()
    return ShareTreeItem(file = file, depth = depth, children = children)
}
