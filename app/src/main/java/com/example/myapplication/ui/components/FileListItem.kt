package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.myapplication.data.local.db.entity.FileEntity
import com.example.myapplication.util.FileTypeHelper

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
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(file.fileId) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { showMenu = true }
                    )
                }
        )

        if (showMenu) {
            Popup(
                alignment = androidx.compose.ui.Alignment.TopEnd,
                offset = IntOffset(-32, 64),
                onDismissRequest = { showMenu = false },
                properties = PopupProperties(focusable = true)
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 180.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = MaterialTheme.shapes.medium
                        )
                ) {
                    PopupMenuItem("重命名") { showMenu = false; onRename() }
                    PopupMenuItem("移动") { showMenu = false; onMove() }
                    PopupMenuItem("删除") { showMenu = false; onDelete() }
                    PopupMenuItem("分享") { showMenu = false; onShare() }
                }
            }
        }
    }

    HorizontalDivider()
}

@Composable
private fun PopupMenuItem(label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        modifier = Modifier
            .pointerInput(label) {
                detectTapGestures(onTap = { onClick() })
            }
    )
}
