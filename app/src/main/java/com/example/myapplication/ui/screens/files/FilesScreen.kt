package com.example.myapplication.ui.screens.files

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.SimplePanApplication
import com.example.myapplication.data.local.db.entity.FileEntity
import com.example.myapplication.ui.components.EmptyState
import com.example.myapplication.ui.components.FileListItem
import com.example.myapplication.ui.components.LoadingOverlay
import com.example.myapplication.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(navController: NavHostController) {
    val app = LocalContext.current.applicationContext as SimplePanApplication
    val viewModel: FilesViewModel = viewModel(
        factory = FilesViewModel.Factory(app.fileRepository, app.userRepository)
    )

    val files by viewModel.files.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentParentId by viewModel.currentParentId.collectAsState()
    val selectedIds by viewModel.selectedFileIds.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadFile(context, it) }
    }

    // Dialog states for single-file actions from bottom bar
    var renameTarget by remember { mutableStateOf<FileEntity?>(null) }
    var moveTargets by remember { mutableStateOf<List<String>?>(null) }
    var deleteTargets by remember { mutableStateOf<List<String>?>(null) }
    var showCreateFolder by remember { mutableStateOf(false) }

    var currentFolderName by remember { mutableStateOf("文件") }

    LaunchedEffect(currentParentId) {
        val parentId = currentParentId
        currentFolderName = if (parentId == null) "文件" else {
            withContext(Dispatchers.IO) {
                app.fileRepository.getFileById(parentId)?.name ?: "文件"
            }
        }
    }

    // Preload folders for move dialog
    var moveFolders by remember { mutableStateOf<List<FileEntity>>(emptyList()) }
    LaunchedEffect(moveTargets) {
        if (moveTargets != null) {
            moveFolders = withContext(Dispatchers.IO) {
                // Exclude all selected items from available folders
                val excludeId = if (selectedIds.size == 1) selectedIds.first() else ""
                app.fileRepository.getAllFoldersExcept(excludeId)
            }
        }
    }

    // Snackbar observer
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    BackHandler(enabled = currentParentId != null) {
        viewModel.navigateBack()
    }

    // Also handle back in selection mode
    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isSelectionMode) "已选 ${selectedIds.size} 项"
                        else currentFolderName
                    )
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Filled.Close, contentDescription = "取消选择")
                        }
                    } else if (currentParentId != null) {
                        IconButton(onClick = { viewModel.navigateBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Filled.SelectAll, contentDescription = "全选")
                        }
                    } else {
                        IconButton(onClick = { showCreateFolder = true }) {
                            Icon(Icons.Filled.CreateNewFolder, contentDescription = "新建文件夹")
                        }
                        IconButton(onClick = {
                            scope.launch { app.fileRepository.syncFromMockData(context) }
                        }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isSelectionMode) {
                BottomAppBar(
                    actions = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 重命名 - only when single file selected
                            if (selectedIds.size == 1) {
                                TextButton(onClick = {
                                    val id = selectedIds.first()
                                    renameTarget = files.find { it.fileId == id }
                                }) {
                                    Icon(Icons.Filled.CreateNewFolder, contentDescription = null)
                                    Text("重命名", modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                            // 移动
                            TextButton(onClick = {
                                moveTargets = selectedIds.toList()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null)
                                Text("移动", modifier = Modifier.padding(start = 4.dp))
                            }
                            // 删除
                            TextButton(onClick = {
                                deleteTargets = selectedIds.toList()
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = null)
                                Text("删除", modifier = Modifier.padding(start = 4.dp))
                            }
                            // 分享 - only when single file selected
                            if (selectedIds.size == 1) {
                                TextButton(onClick = {
                                    val id = selectedIds.first()
                                    scope.launch {
                                        val link = app.shareRepository.generateShareLink(id)
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("share_link", link))
                                        snackbarHostState.showSnackbar("分享链接已复制到剪贴板")
                                    }
                                }) {
                                    Icon(Icons.Filled.Share, contentDescription = null)
                                    Text("分享", modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = { filePickerLauncher.launch("*/*") }) {
                    Icon(Icons.Filled.Add, contentDescription = "上传")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        var isRefreshing by remember { mutableStateOf(false) }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    app.fileRepository.syncFromMockData(context)
                    isRefreshing = false
                }
            },
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            if (files.isEmpty() && !isLoading) {
                EmptyState(icon = Icons.Filled.Folder, message = "暂无文件")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(files, key = { it.fileId }) { file ->
                        FileListItem(
                            file = file,
                            isSelected = file.fileId in selectedIds,
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleSelection(file.fileId)
                                } else {
                                    onFileClick(file, viewModel, navController, context)
                                }
                            },
                            onLongPress = {
                                if (!isSelectionMode) {
                                    viewModel.toggleSelection(file.fileId)
                                }
                            }
                        )
                    }
                }
            }

            if (isLoading) LoadingOverlay()

            // Rename dialog
            renameTarget?.let { file ->
                RenameDialog(
                    currentName = file.name,
                    onConfirm = { newName ->
                        viewModel.renameFile(file.fileId, newName)
                        renameTarget = null
                    },
                    onDismiss = { renameTarget = null }
                )
            }

            // Move dialog
            moveTargets?.let { ids ->
                MoveFileDialog(
                    folders = moveFolders,
                    onConfirm = { newParentId ->
                        viewModel.moveSelectedFiles(newParentId)
                        moveTargets = null
                    },
                    onDismiss = { moveTargets = null }
                )
            }

            // Delete dialog
            deleteTargets?.let { ids ->
                val count = ids.size
                DeleteConfirmDialog(
                    fileName = if (count == 1) files.find { it.fileId == ids.first() }?.name ?: "文件" else "${count}个项目",
                    onConfirm = {
                        viewModel.deleteSelectedFiles()
                        deleteTargets = null
                    },
                    onDismiss = { deleteTargets = null }
                )
            }

            // Create folder dialog
            if (showCreateFolder) {
                CreateFolderDialog(
                    onConfirm = { folderName ->
                        viewModel.createFolder(folderName)
                        showCreateFolder = false
                    },
                    onDismiss = { showCreateFolder = false }
                )
            }
        }
    }
}

private fun onFileClick(
    file: FileEntity,
    viewModel: FilesViewModel,
    navController: NavHostController,
    context: android.content.Context
) {
    when (file.type) {
        "folder" -> viewModel.navigateToFolder(file.fileId)
        "txt" -> {
            viewModel.recordBrowse(file.fileId)
            navController.navigate(Screen.Reader.createRoute(file.fileId))
        }
        "video", "audio" -> {
            viewModel.recordBrowse(file.fileId)
            com.example.myapplication.util.FileOpener.openFile(context, file)
        }
        else -> {
            Toast.makeText(context, "暂不支持预览此文件类型", Toast.LENGTH_SHORT).show()
        }
    }
}
