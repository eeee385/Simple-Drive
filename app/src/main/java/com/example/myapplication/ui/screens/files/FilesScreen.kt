package com.example.myapplication.ui.screens.files

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadFile(context, it) }
    }

    // Selected file for context menu and dialogs
    var selectedFile by remember { mutableStateOf<FileEntity?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Folder name for title
    var currentFolderName by remember { mutableStateOf("文件") }

    // Load folder name
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
    LaunchedEffect(showMoveDialog) {
        if (showMoveDialog && selectedFile != null) {
            moveFolders = withContext(Dispatchers.IO) {
                app.fileRepository.getAllFoldersExcept(selectedFile!!.fileId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentFolderName) },
                navigationIcon = {
                    if (currentParentId != null) {
                        IconButton(onClick = { viewModel.navigateBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { filePickerLauncher.launch("*/*") }) {
                Icon(Icons.Filled.Add, contentDescription = "上传")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (files.isEmpty() && !isLoading) {
                EmptyState(icon = Icons.Filled.Folder, message = "暂无文件")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(files, key = { it.fileId }) { file ->
                        FileListItem(
                            file = file,
                            onClick = { onFileClick(file, viewModel, navController, context) },
                            onLongClick = {
                                selectedFile = file
                                showContextMenu = true
                            }
                        )
                    }
                }
            }

            if (isLoading) LoadingOverlay()

            // Context menu
            if (showContextMenu && selectedFile != null) {
                FileContextMenu(
                    expanded = true,
                    onDismiss = {
                        showContextMenu = false
                        selectedFile = null
                    },
                    onRename = {
                        showContextMenu = false
                        showRenameDialog = true
                    },
                    onMove = {
                        showContextMenu = false
                        showMoveDialog = true
                    },
                    onDelete = {
                        showContextMenu = false
                        showDeleteDialog = true
                    },
                    onShare = {
                        showContextMenu = false
                        val fileId = selectedFile!!.fileId
                        scope.launch {
                            val link = app.shareRepository.generateShareLink(fileId)
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("share_link", link))
                            snackbarHostState.showSnackbar("分享链接已复制到剪贴板")
                        }
                        selectedFile = null
                    }
                )
            }

            // Rename dialog
            if (showRenameDialog && selectedFile != null) {
                RenameDialog(
                    currentName = selectedFile!!.name,
                    onConfirm = { newName ->
                        viewModel.renameFile(selectedFile!!.fileId, newName)
                        showRenameDialog = false
                        selectedFile = null
                    },
                    onDismiss = {
                        showRenameDialog = false
                        selectedFile = null
                    }
                )
            }

            // Move dialog
            if (showMoveDialog && selectedFile != null) {
                MoveFileDialog(
                    folders = moveFolders,
                    onConfirm = { newParentId ->
                        viewModel.moveFile(selectedFile!!.fileId, newParentId)
                        showMoveDialog = false
                        selectedFile = null
                    },
                    onDismiss = {
                        showMoveDialog = false
                        selectedFile = null
                    }
                )
            }

            // Delete dialog
            if (showDeleteDialog && selectedFile != null) {
                DeleteConfirmDialog(
                    fileName = selectedFile!!.name,
                    onConfirm = {
                        viewModel.deleteFile(selectedFile!!.fileId)
                        showDeleteDialog = false
                        selectedFile = null
                    },
                    onDismiss = {
                        showDeleteDialog = false
                        selectedFile = null
                    }
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
        "video" -> {
            viewModel.recordBrowse(file.fileId)
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        android.net.Uri.parse("content://media/external/video/media"),
                        "video/*"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "没有可用的视频播放器", Toast.LENGTH_SHORT).show()
            }
        }
        else -> { /* no action for other types */ }
    }
}
