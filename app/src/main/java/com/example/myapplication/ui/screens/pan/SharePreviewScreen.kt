package com.example.myapplication.ui.screens.pan

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePreviewScreen(
    shareId: String,
    onDismiss: () -> Unit,
    onTransfer: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SimplePanApplication
    var isLoading by remember { mutableStateOf(true) }
    var rootItem by remember { mutableStateOf<ShareTreeItem?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val expandedIds = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(shareId) {
        withContext(Dispatchers.IO) {
            val fileId = app.shareRepository.resolveShareLink(shareId)
            if (fileId == null) { errorMsg = "分享链接无效或已过期"; isLoading = false; return@withContext }
            val file = app.fileRepository.getFileById(fileId)
            if (file == null) { errorMsg = "文件不存在"; isLoading = false; return@withContext }
            rootItem = buildTree(app, file, 0)
            // Expand root folder by default
            if (file.type == "folder") expandedIds[file.fileId] = true
            isLoading = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("分享文件") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("取消") }
                    Button(
                        onClick = onTransfer,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = WarmAmber,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        )
                    ) { Text("转存") }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when {
                isLoading -> LoadingOverlay()
                errorMsg != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) { Text(errorMsg!!, style = MaterialTheme.typography.titleMedium) }
                }
                rootItem != null -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) { Text("分享者: 演示用户", style = MaterialTheme.typography.bodyMedium) }
                            HorizontalDivider()
                        }
                        item { ShareTreeView(rootItem!!, expandedIds) }
                    }
                }
            }
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
                .clickable {
                    if (isFolder) {
                        expandedIds[item.file.fileId] = !isExpanded
                    } else {
                        Toast.makeText(context, "请转存后查看", Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width((item.depth * 24).dp))

            if (isFolder && hasChildren) {
                Icon(
                    if (isExpanded) Icons.Filled.ExpandMore else Icons.Filled.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.width(24.dp)
                )
            } else if (isFolder && !hasChildren) {
                Spacer(Modifier.width(24.dp))
            } else {
                Spacer(Modifier.width(24.dp))
            }

            Spacer(Modifier.width(8.dp))
            Icon(FileTypeHelper.getFileIcon(item.file.type), contentDescription = null)
            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.file.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isFolder) FontWeight.Medium else FontWeight.Normal
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
        HorizontalDivider()

        // Render children if expanded
        if (isExpanded) {
            item.children.forEach { child -> ShareTreeView(child, expandedIds) }
        }
    }
}

private suspend fun buildTree(app: SimplePanApplication, file: FileEntity, depth: Int): ShareTreeItem {
    val children = if (file.type == "folder") {
        app.fileRepository.getFilesByParentId(file.fileId)
            .let { flow -> flow.first() }
            .map { child -> buildTree(app, child, depth + 1) }
    } else emptyList()
    return ShareTreeItem(file = file, depth = depth, children = children)
}
