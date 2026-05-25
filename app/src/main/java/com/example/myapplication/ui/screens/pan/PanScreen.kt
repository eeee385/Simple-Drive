package com.example.myapplication.ui.screens.pan

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.SimplePanApplication
import com.example.myapplication.domain.model.UserInfo
import com.example.myapplication.ui.components.EmptyState
import com.example.myapplication.ui.navigation.Screen
import com.example.myapplication.util.FileTypeHelper
import com.example.myapplication.util.TimeUtils

@Composable
fun PanScreen(navController: NavHostController) {
    val app = LocalContext.current.applicationContext as SimplePanApplication
    val viewModel: PanViewModel = viewModel(
        factory = PanViewModel.Factory(app.fileRepository, app.userRepository)
    )

    val userInfo by viewModel.userInfo.collectAsState()
    val recentBrowses by viewModel.recentBrowses.collectAsState()
    val recentTransfers by viewModel.recentTransfers.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // User info card
        item { UserInfoCard(userInfo) }
        item { Spacer(Modifier.height(16.dp)) }

        // Recent transfers
        item { SectionHeader("最近转存", Icons.Filled.Bookmark) }
        if (recentTransfers.isEmpty()) {
            item { EmptyState(Icons.Filled.Bookmark, "暂无转存记录", Modifier.height(120.dp)) }
        } else {
            items(recentTransfers, key = { it.file.fileId + "t" }) { ft ->
                RecentFileItem(
                    name = ft.file.name,
                    type = ft.file.type,
                    size = ft.file.size,
                    time = ft.transferTime,
                    onClick = { navigateToFile(ft.file.fileId, ft.file.type, navController) }
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        // Recent browses
        item { SectionHeader("最近浏览", Icons.Filled.History) }
        if (recentBrowses.isEmpty()) {
            item { EmptyState(Icons.Filled.History, "暂无浏览记录", Modifier.height(120.dp)) }
        } else {
            items(recentBrowses, key = { it.file.fileId + "b" }) { fb ->
                RecentFileItem(
                    name = fb.file.name,
                    type = fb.file.type,
                    size = fb.file.size,
                    time = fb.browseTime,
                    onClick = { navigateToFile(fb.file.fileId, fb.file.type, navController) }
                )
            }
        }
    }
}

@Composable
private fun UserInfoCard(userInfo: UserInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Avatar and name
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                userInfo.userName.ifEmpty { "演示用户" },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Space usage
            val used = userInfo.usedSpace
            val total = userInfo.totalSpace
            val percent = if (total > 0) used.toFloat() / total else 0f

            Text(
                "已用 ${FileTypeHelper.formatFileSize(used)} / 共 ${FileTypeHelper.formatFileSize(total)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { percent.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "剩余 ${FileTypeHelper.formatFileSize(total - used)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun RecentFileItem(
    name: String,
    type: String,
    size: Long,
    time: Long,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(TimeUtils.formatRelativeTime(time))
        },
        leadingContent = {
            Icon(FileTypeHelper.getFileIcon(type), contentDescription = type)
        },
        modifier = Modifier
            .fillMaxWidth()
            .then(
                Modifier.pointerInput(name) {
                    detectTapGestures(onTap = { onClick() })
                }
            )
    )
    HorizontalDivider()
}

private fun navigateToFile(fileId: String, type: String, navController: NavHostController) {
    when (type) {
        "folder" -> navController.navigate(Screen.FileList.createRoute(fileId))
        "txt" -> navController.navigate(Screen.Reader.createRoute(fileId))
        else -> { /* just show the item, no navigation for now */ }
    }
}
