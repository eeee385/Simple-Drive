package com.example.myapplication.ui.screens.files

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.R
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
fun FilesScreen(
    navController: NavHostController,
    onSelectionChanged: ((Boolean, Int, Boolean, () -> Unit, () -> Unit) -> Unit)? = null,
    onFolderChanged: ((isInSubFolder: Boolean, folderName: String, onBack: () -> Unit) -> Unit)? = null
) {
    val app = LocalContext.current.applicationContext as SimplePanApplication
    val viewModel: FilesViewModel = viewModel(
        factory = FilesViewModel.Factory(app.fileRepository, app.userRepository)
    )

    // Read parentId from navigation argument (used by DeepLink)
    val navParentId = navController.currentBackStackEntry?.arguments?.getString("parentId")?.takeIf { it != "root" }
    LaunchedEffect(navParentId) {
        if (navParentId != null) {
            val name = withContext(Dispatchers.IO) {
                app.fileRepository.getFileById(navParentId)?.name ?: ""
            }
            viewModel.navigateToFolder(navParentId, name)
        }
    }

    val files by viewModel.files.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentParentId by viewModel.currentParentId.collectAsState()
    val selectedIds by viewModel.selectedFileIds.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val currentFilter by viewModel.filterType.collectAsState()

    LaunchedEffect(isSelectionMode, selectedIds.size, files.size) {
        val allSelected = files.isNotEmpty() && selectedIds.size == files.size
        onSelectionChanged?.invoke(
            isSelectionMode, selectedIds.size, allSelected,
            { viewModel.clearSelection() },
            { viewModel.selectAll() }
        )
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadFile(context, it) }
    }

    // Dialog states
    var renameTarget by remember { mutableStateOf<FileEntity?>(null) }
    var deleteTargets by remember { mutableStateOf<List<String>?>(null) }
    var showCreateFolder by remember { mutableStateOf(false) }
    var showActionSheet by remember { mutableStateOf(false) }

    // Pending move state (must survive navigation)
    var pendingMoveIds by rememberSaveable { mutableStateOf<List<String>?>(null) }

    var currentFolderName by remember { mutableStateOf("") }
    var folderPath by remember { mutableStateOf<List<String>>(emptyList()) }

    // Listen for folder picker result from savedStateHandle
    val backStackEntryId = navController.currentBackStackEntry?.id
    LaunchedEffect(backStackEntryId) {
        // When returning from FolderPicker, check for result
        val handle = navController.currentBackStackEntry?.savedStateHandle
        val result = handle?.get<String>("picker_result")
        if (result != null) {
            val ids = pendingMoveIds
            if (ids != null) {
                viewModel.moveSelectedFiles(if (result == "root") null else result)
                pendingMoveIds = null
            }
            handle.remove<String>("picker_result")
        }
    }

    // Snackbar observer
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Indefinite) }
            kotlinx.coroutines.delay(2000)
            snackbarHostState.currentSnackbarData?.dismiss()
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

    // Track folder path for breadcrumb — using SideEffect for same-frame update
    val vmFolderName by viewModel.currentFolderName.collectAsState()
    currentFolderName = if (currentParentId == null) "" else vmFolderName
    folderPath = if (currentParentId == null) emptyList() else listOf("我的网盘") + viewModel.getFolderPath()
    SideEffect {
        onFolderChanged?.invoke(currentParentId != null, currentFolderName) { viewModel.navigateBack() }
    }

    Scaffold(
        topBar = { },
        bottomBar = {
            if (isSelectionMode) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.background,
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
                                    Icon(painterResource(R.drawable.ic_rename), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
                                    Text("重命名", modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                            // 移动
                            TextButton(onClick = {
                                pendingMoveIds = selectedIds.toList()
                                navController.currentBackStackEntry?.savedStateHandle?.set(
                                    "move_ids",
                                    selectedIds.toList()
                                )
                                navController.navigate(com.example.myapplication.ui.navigation.Screen.FolderPicker.createRoute())
                            }) {
                                Icon(painterResource(R.drawable.ic_move), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
                                Text("移动", modifier = Modifier.padding(start = 4.dp))
                            }
                            // 删除
                            TextButton(onClick = {
                                deleteTargets = selectedIds.toList()
                            }) {
                                Icon(painterResource(R.drawable.ic_delete), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
                                Text("删除", modifier = Modifier.padding(start = 4.dp))
                            }
                            // 分享 - only when single file selected
                            if (selectedIds.size == 1) {
                                TextButton(onClick = {
                                    val id = selectedIds.first()
                                    scope.launch {
                                        viewModel.clearSelection()
                                        val link = app.shareRepository.generateShareLink(id)
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("share_link", link))
                                        launch { snackbarHostState.showSnackbar("分享链接已复制到剪贴板", duration = SnackbarDuration.Indefinite) }
                                        kotlinx.coroutines.delay(2000)
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                    }
                                }) {
                                    Icon(painterResource(R.drawable.ic_share), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
                                    Text("分享", modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = { },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
            if (currentParentId == null) {
                Box(modifier = Modifier.alpha(if (isSelectionMode) 0.4f else 1f)) {
                    FilterChipRow(
                        currentFilter = currentFilter,
                        onFilterSelected = { if (!isSelectionMode) viewModel.setFilter(it) }
                    )
                }
            } else if (folderPath.isNotEmpty()) {
                BreadcrumbRow(folderPath)
            }

            var isRefreshing by remember { mutableStateOf(false) }
            @Composable
            fun FileListInner() {
                if (files.isEmpty() && !isLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        EmptyState(icon = Icons.Filled.Folder, message = "暂无文件", modifier = Modifier.height(160.dp))
                        TextButton(onClick = { filePickerLauncher.launch("*/*") }) {
                            Text("上传第一个文件")
                        }
                    }
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
            }

            @Composable
            fun FileListContent() {
                if (!isSelectionMode) {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            scope.launch {
                                isRefreshing = true
                                app.fileRepository.syncFromMockData(context)
                                isRefreshing = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { FileListInner() }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) { FileListInner() }
                }
            }

            FileListContent()

            // Dialogs
            renameTarget?.let { file ->
                RenameDialog(
                    currentName = file.name,
                    onConfirm = { newName ->
                        viewModel.renameFile(file.fileId, newName)
                        viewModel.clearSelection()
                        renameTarget = null
                    },
                    onDismiss = { renameTarget = null }
                )
            }

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

            // FAB — right side, 1/5 from bottom
            if (!isSelectionMode) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(Modifier.weight(8f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FloatingActionButton(
                            onClick = { showActionSheet = true },
                            containerColor = com.example.myapplication.ui.theme.WarmAmber,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "添加")
                        }
                    }
                    Spacer(Modifier.weight(1f))
                }
            }

    // Action bottom sheet
    if (showActionSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showActionSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 36.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showActionSheet = false
                            showCreateFolder = true
                        }
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_folder),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "新建文件夹",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showActionSheet = false
                            filePickerLauncher.launch("*/*")
                        }
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_upload),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "上传文件",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
    } // Box
}

@Composable
private fun BreadcrumbRow(path: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        path.forEachIndexed { index, name ->
            if (index > 0) {
                Text(
                    " > ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (index == path.lastIndex) FontWeight.Bold else FontWeight.Normal,
                color = if (index == path.lastIndex) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FilterChipRow(
    currentFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit
) {
    val items = listOf(FilterType.ALL, FilterType.IMAGE, FilterType.VIDEO, FilterType.DOC)
    val labels = listOf("全部", "图片", "视频", "文档")
    val selectedIndex = items.indexOf(currentFilter)

    val indicatorOffset by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = androidx.compose.animation.core.tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "filterIndicator"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val segmentWidth = maxWidth / 4
            val indicatorWidth = segmentWidth - 8.dp

            // Sliding indicator
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = segmentWidth * indicatorOffset + 4.dp)
                    .width(indicatorWidth)
                    .height(36.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )

            // Labels
            Row(modifier = Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, filter ->
                    val isSelected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) { onFilterSelected(filter) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            labels[index],
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
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
        "folder" -> viewModel.navigateToFolder(file.fileId, file.name)
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
