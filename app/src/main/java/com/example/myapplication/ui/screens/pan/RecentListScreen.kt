package com.example.myapplication.ui.screens.pan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    val (title, items) = remember(listType, recentBrowses, recentTransfers) {
        when (listType) {
            "browse" -> "最近浏览" to recentBrowses.map { ft ->
                RecentItem(ft.file.fileId, ft.file.name, ft.file.type, ft.file.size, ft.browseTime, ft.file)
            }
            else -> "最近转存" to recentTransfers.map { ft ->
                RecentItem(ft.file.fileId, ft.file.name, ft.file.type, ft.file.size, ft.transferTime, ft.file)
            }
        }
    }

    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(items) {
        if (items.isNotEmpty()) ready = true
    }

    if (!ready) {
        com.example.myapplication.ui.components.LoadingOverlay()
    } else if (items.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.History,
            message = "暂无记录"
        )
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items, key = { it.id + listType }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = {
                                viewModel.recordBrowse(item.file.fileId)
                                when (item.file.type) {
                                    "folder" -> navController.navigate(Screen.FileList.createRoute(item.file.fileId))
                                    "txt" -> navController.navigate(Screen.Reader.createRoute(item.file.fileId))
                                    else -> FileOpener.openFile(context, item.file)
                                }
                            })
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(FileTypeHelper.getFileIconRes(item.type)),
                            contentDescription = item.type,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                TimeUtils.formatRelativeTime(item.time),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
}

