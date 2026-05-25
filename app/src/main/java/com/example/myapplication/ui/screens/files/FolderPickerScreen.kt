package com.example.myapplication.ui.screens.files

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var selectedFolderName by remember { mutableStateOf<String?>(null) }
    var folders by remember { mutableStateOf<List<FileEntity>>(emptyList()) }
    var currentFolderName by remember { mutableStateOf("根目录") }
    val navStack = remember { mutableListOf<String?>() }
    val nameStack = remember { mutableStateListOf("根目录") }

    // Build path string: e.g. "根目录/文档/子文件夹"
    val pathString = nameStack.joinToString("/")

    LaunchedEffect(currentParentId) {
        withContext(Dispatchers.IO) {
            val all = app.fileRepository.getFilesByParentId(currentParentId).first()
            folders = all.filter { it.type == "folder" && it.fileId !in excludedFolderIds }
            currentFolderName = if (currentParentId != null) {
                val file = app.fileRepository.getFileById(currentParentId!!)
                file?.name ?: "根目录"
            } else "根目录"
        }
    }

    // System back button
    BackHandler(enabled = currentParentId != null) {
        navStack.removeLastOrNull()
        nameStack.removeLastOrNull()
        currentParentId = navStack.lastOrNull()
        selectedFolderId = null
        selectedFolderName = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentFolderName) },
                navigationIcon = {
                    if (currentParentId != null) {
                        IconButton(onClick = {
                            navStack.removeLastOrNull()
                            nameStack.removeLastOrNull()
                            currentParentId = navStack.lastOrNull()
                            selectedFolderId = null
                            selectedFolderName = null
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { onFolderSelected(selectedFolderId) }) {
                        Text("确定")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("目标: ")
                Text(
                    pathString,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider()

            // Root option (only at top level)
            if (currentParentId == null) {
                ListItem(
                    headlineContent = { Text("根目录") },
                    leadingContent = {
                        Checkbox(
                            checked = selectedFolderId == null,
                            onCheckedChange = { if (it) { selectedFolderId = null; selectedFolderName = "根目录" } }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider()
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(folders, key = { it.fileId }) { folder ->
                    ListItem(
                        headlineContent = { Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingContent = {
                            Checkbox(
                                checked = selectedFolderId == folder.fileId,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        selectedFolderId = folder.fileId
                                        selectedFolderName = folder.name
                                    }
                                }
                            )
                        },
                        trailingContent = { Icon(Icons.Filled.Folder, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navStack.add(currentParentId)
                                nameStack.add(folder.name)
                                currentParentId = folder.fileId
                            }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
