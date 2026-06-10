package com.example.myapplication.ui.screens.pan

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.myapplication.domain.model.UserInfo
import com.example.myapplication.ui.components.EmptyState
import com.example.myapplication.ui.navigation.Screen
import com.example.myapplication.ui.theme.GradientEnd
import com.example.myapplication.ui.theme.GradientStart
import com.example.myapplication.ui.theme.WarmAmber
import com.example.myapplication.util.FileTypeHelper
import com.example.myapplication.util.TimeUtils

@Composable
fun PanScreen(navController: NavHostController) {
    val context = LocalContext.current
    val app = context.applicationContext as SimplePanApplication
    val viewModel: PanViewModel = viewModel(
        factory = PanViewModel.Factory(app.fileRepository, app.userRepository)
    )

    val userInfo by viewModel.userInfo.collectAsState()
    val recentBrowses by viewModel.recentBrowses.collectAsState()
    val recentTransfers by viewModel.recentTransfers.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item { Spacer(Modifier.height(16.dp)) }

        // User info card with gradient
        item { UserInfoCard(userInfo) }
        item { Spacer(Modifier.height(20.dp)) }

        // Recent transfers
        item {
            SectionHeader(
                title = "最近转存",
                onViewAll = if (recentTransfers.size > 4) {
                    { navController.navigate(Screen.RecentList.createRoute("transfer")) }
                } else null
            )
        }
        if (recentTransfers.isEmpty()) {
            item { EmptyState(Icons.Filled.Bookmark, "暂无转存记录", Modifier.height(120.dp)) }
        } else {
            items(recentTransfers.take(4), key = { it.file.fileId + "t" }) { ft ->
                RecentFileItem(
                    name = ft.file.name,
                    type = ft.file.type,
                    size = ft.file.size,
                    time = ft.transferTime,
                    onClick = { navigateToFile(ft.file, navController, context, viewModel) }
                )
            }
        }

        item { Spacer(Modifier.height(20.dp)) }

        // Recent browses
        item {
            SectionHeader(
                title = "最近浏览",
                onViewAll = if (recentBrowses.size > 4) {
                    { navController.navigate(Screen.RecentList.createRoute("browse")) }
                } else null
            )
        }
        if (recentBrowses.isEmpty()) {
            item { EmptyState(Icons.Filled.History, "暂无浏览记录", Modifier.height(120.dp)) }
        } else {
            items(recentBrowses.take(4), key = { it.file.fileId + "b" }) { fb ->
                RecentFileItem(
                    name = fb.file.name,
                    type = fb.file.type,
                    size = fb.file.size,
                    time = fb.browseTime,
                    onClick = { navigateToFile(fb.file, navController, context, viewModel) }
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun UserInfoCard(userInfo: UserInfo) {
    val used = userInfo.usedSpace
    val total = userInfo.totalSpace
    val percent = if (total > 0) used.toFloat() / total else 0f

    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(GradientStart, GradientEnd)
                    )
                )
                .padding(20.dp)
        ) {
            // Row 1: avatar + name | remaining space
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        userInfo.userName.ifEmpty { "演示用户" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "个人网盘",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "剩余",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        FileTypeHelper.formatFileSize(total - used),
                        style = MaterialTheme.typography.titleMedium,
                        color = WarmAmber,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { percent.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = WarmAmber,
                trackColor = Color.White.copy(alpha = 0.2f),
            )

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "已用 ${FileTypeHelper.formatFileSize(used)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    "共 ${FileTypeHelper.formatFileSize(total)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Three entry points
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EntryChip(Icons.Filled.Subscriptions, "我的订阅") { /* TODO */ }
                EntryChip(Icons.Filled.Share, "我的分享") { /* TODO */ }
                EntryChip(Icons.Filled.Star, "云收藏文件") { /* TODO */ }
            }
        }
    }
}

@Composable
private fun EntryChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onViewAll: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(WarmAmber)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (onViewAll != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onViewAll)
                    .padding(start = 8.dp, top = 4.dp, bottom = 4.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "全部",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "查看全部",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun RecentFileItem(
    name: String,
    type: String,
    size: Long,
    time: Long,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(FileTypeHelper.getFileIconRes(type)),
            contentDescription = type,
            tint = Color.Unspecified,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                TimeUtils.formatRelativeTime(time),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun navigateToFile(file: FileEntity, navController: NavHostController, context: android.content.Context, viewModel: PanViewModel) {
    viewModel.recordBrowse(file.fileId)
    when (file.type) {
        "folder" -> navController.navigate(Screen.FileList.createRoute(file.fileId))
        "txt" -> navController.navigate(Screen.Reader.createRoute(file.fileId))
        else -> com.example.myapplication.util.FileOpener.openFile(context, file)
    }
}
