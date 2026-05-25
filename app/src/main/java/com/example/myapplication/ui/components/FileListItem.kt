package com.example.myapplication.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.example.myapplication.data.local.db.entity.FileEntity
import com.example.myapplication.util.FileTypeHelper

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    file: FileEntity,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = {
                Text(
                    text = file.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = if (file.type != "folder") {
                { Text(FileTypeHelper.formatFileSize(file.size)) }
            } else null,
            leadingContent = {
                Icon(
                    imageVector = FileTypeHelper.getFileIcon(file.type),
                    contentDescription = file.type
                )
            },
            modifier = Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("重命名") },
                onClick = { showMenu = false; onRename() }
            )
            DropdownMenuItem(
                text = { Text("移动") },
                onClick = { showMenu = false; onMove() }
            )
            DropdownMenuItem(
                text = { Text("删除") },
                onClick = { showMenu = false; onDelete() }
            )
            DropdownMenuItem(
                text = { Text("分享") },
                onClick = { showMenu = false; onShare() }
            )
        }
    }

    HorizontalDivider()
}
