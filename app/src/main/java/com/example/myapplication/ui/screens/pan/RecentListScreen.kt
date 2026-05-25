package com.example.myapplication.ui.screens.pan

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.SimplePanApplication
import com.example.myapplication.data.local.db.entity.FileEntity
import com.example.myapplication.ui.components.EmptyState
import com.example.myapplication.ui.navigation.Screen
import com.example.myapplication.util.FileOpener
import com.example.myapplication.util.FileTypeHelper
import com.example.myapplication.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentListScreen(
    listType: String,
    navController: NavHostController
) {
    val context = LocalContext.current
    val app = context.applicationContext as SimplePanApplication
    val viewModel: PanViewModel = viewModel(
        factory = PanViewModel.Factory(app.fileRepository, app.userRepository)
    )

    val recentBrowses by viewModel.recentBrowses.collectAsState()
    val recentTransfers by viewModel.recentTransfers.collectAsState()

    data class RecentItem(val id: String, val name: String, val type: String, val size: Long, val time: Long, val file: FileEntity)

    val (title, items) = when (listType) {
        "browse" -> "最近浏览" to recentBrowses.map { ft ->
            RecentItem(ft.file.fileId, ft.file.name, ft.file.type, ft.file.size, ft.browseTime, ft.file)
        }
        else -> "最近转存" to recentTransfers.map { ft ->
            RecentItem(ft.file.fileId, ft.file.name, ft.file.type, ft.file.size, ft.transferTime, ft.file)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (items.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.History,
                message = "暂无记录",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                items(items, key = { it.id + listType }) { item ->
                    ListItem(
                        headlineContent = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(TimeUtils.formatRelativeTime(item.time)) },
                        leadingContent = { Icon(FileTypeHelper.getFileIcon(item.type), contentDescription = item.type) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(item.name) {
                                detectTapGestures(onTap = {
                                    viewModel.recordBrowse(item.file.fileId)
                                    when (item.file.type) {
                                        "folder" -> navController.navigate(Screen.FileList.createRoute(item.file.fileId))
                                        "txt" -> navController.navigate(Screen.Reader.createRoute(item.file.fileId))
                                        else -> FileOpener.openFile(context, item.file)
                                    }
                                })
                            }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
