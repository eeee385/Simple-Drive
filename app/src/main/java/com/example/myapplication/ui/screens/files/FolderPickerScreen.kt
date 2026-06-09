package com.example.myapplication.ui.screens.files

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.SimplePanApplication
import com.example.myapplication.data.local.db.entity.FileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickerScreen(
    initialParentId: String,
    excludedFolderIds: Set<String> = emptySet(),
    onFolderSelected: (String?) -> Unit,
    onCancel: () -> Unit
) {
    val app = LocalContext.current.applicationContext as SimplePanApplication
    var currentParentId by remember { mutableStateOf(if (initialParentId == "root") null else initialParentId) }
    var folders by remember { mutableStateOf<List<FileEntity>>(emptyList()) }
    var currentFolderName by remember { mutableStateOf("我的网盘") }
    val navStack = remember { mutableListOf<String?>() }
    val nameStack = remember { mutableStateListOf("我的网盘") }

    val pathString = nameStack.joinToString(" > ")

    LaunchedEffect(currentParentId) {
        withContext(Dispatchers.IO) {
            val all = app.fileRepository.getFilesByParentId(currentParentId).first()
            folders = all.filter { it.type == "folder" && it.fileId !in excludedFolderIds }
            currentFolderName = if (currentParentId != null) {
                app.fileRepository.getFileById(currentParentId!!)?.name ?: "我的网盘"
            } else "我的网盘"
        }
    }

    BackHandler {
        if (navStack.isNotEmpty()) {
            nameStack.removeLastOrNull()
            currentParentId = navStack.removeLastOrNull()
        } else {
            onCancel()
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.66f)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.width(48.dp)) {
                    IconButton(onClick = {
                        if (navStack.isNotEmpty()) {
                            nameStack.removeLastOrNull()
                            currentParentId = navStack.removeLastOrNull()
                        } else {
                            onCancel()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
                Text(
                    "选择文件夹",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(48.dp))
            }

            // Breadcrumb
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    pathString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Folder list
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(folders, key = { it.fileId }) { folder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navStack.add(currentParentId)
                                nameStack.add(folder.name)
                                currentParentId = folder.fileId
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(com.example.myapplication.R.drawable.ic_folder),
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            folder.name,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(0.5.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
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
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = { onFolderSelected(currentParentId ?: "root") },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("保存到此处")
                }
            }
        }
    }
}
